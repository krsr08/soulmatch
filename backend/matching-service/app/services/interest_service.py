import uuid
import json
import os
from typing import Optional

import httpx

from app.config.database import get_db_pool

PLAN_ENTITLEMENTS = {
    "free": {"interests": 5, "shortlist": 5},
    "bronze": {"interests": 5, "shortlist": 5},
    "silver": {"interests": 20, "shortlist": 20},
    "gold": {"interests": 40, "shortlist": 40},
    "platinum": {"interests": 80, "shortlist": 80},
}


def _config_dict(value):
    if isinstance(value, dict):
        return value
    if isinstance(value, str):
        try:
            return json.loads(value)
        except Exception:
            return {}
    return {}


def _normalize_plan_id(plan_id: Optional[str]) -> str:
    value = str(plan_id or "free").lower()
    return "bronze" if value == "free" else value if value in PLAN_ENTITLEMENTS else "bronze"


async def _member_entitlements(conn, plan_id: str) -> dict:
    normalized = _normalize_plan_id(plan_id)
    monetization = _config_dict(
        await conn.fetchval("SELECT config_value FROM app_config WHERE config_key='monetization' LIMIT 1")
    )
    configured = _config_dict(monetization.get("memberPlanEntitlements"))
    raw = _config_dict(configured.get(normalized))
    base = PLAN_ENTITLEMENTS.get(normalized, PLAN_ENTITLEMENTS["bronze"])
    return {
        "interests": int(raw.get("interests") or base["interests"]),
        "shortlist": int(raw.get("shortlist") or base["shortlist"]),
    }


async def _active_plan_id(conn, user_id: str) -> str:
    row = await conn.fetchval(
        """
        SELECT plan_id
        FROM subscriptions
        WHERE user_id=$1
          AND is_active=true
          AND (end_date IS NULL OR end_date>NOW())
        ORDER BY created_at DESC
        LIMIT 1
        """,
        user_id,
    )
    return _normalize_plan_id(row)


async def _ensure_usage_record(conn, user_id: str, plan_id: str) -> dict:
    row = await conn.fetchrow("SELECT * FROM member_subscription_usage WHERE user_id=$1 LIMIT 1", user_id)
    normalized = _normalize_plan_id(plan_id)
    if not row:
        row = await conn.fetchrow(
            """
            INSERT INTO member_subscription_usage (user_id, plan_id, period_started_at, period_ends_at)
            VALUES ($1,$2,NOW(),NOW() + INTERVAL '30 days')
            RETURNING *
            """,
            user_id,
            normalized,
        )
        return dict(row)
    expired = row["period_ends_at"] and row["period_ends_at"].timestamp() <= __import__("time").time()
    plan_changed = _normalize_plan_id(row["plan_id"]) != normalized
    if expired or plan_changed:
        row = await conn.fetchrow(
            """
            UPDATE member_subscription_usage
            SET plan_id=$2,
                period_started_at=NOW(),
                period_ends_at=NOW() + INTERVAL '30 days',
                profile_views_used=0,
                contact_unlocks_used=0,
                shortlists_used=0,
                interests_used=0,
                spotlight_boosts_used=0,
                updated_at=NOW()
            WHERE user_id=$1
            RETURNING *
            """,
            user_id,
            normalized,
        )
    return dict(row)


async def _consume_limit(conn, user_id: str, target_profile_id: str, event_type: str, column: str, limit: int) -> None:
    plan_id = await _active_plan_id(conn, user_id)
    usage = await _ensure_usage_record(conn, user_id, plan_id)
    already = await conn.fetchval(
        """
        SELECT true
        FROM member_meter_events
        WHERE user_id=$1
          AND target_profile_id=$2
          AND event_type=$3
          AND period_key=DATE_TRUNC('month', NOW())::date
        LIMIT 1
        """,
        user_id,
        target_profile_id,
        event_type,
    )
    if already:
        return
    used = int(usage.get(column) or 0)
    if used >= int(limit):
        raise ValueError("Limit reached. Extend your subscription to continue.")
    await conn.execute(
        """
        INSERT INTO member_meter_events (event_id,user_id,target_profile_id,event_type,period_key,metadata,created_at)
        VALUES ($1,$2,$3,$4,DATE_TRUNC('month', NOW())::date,'{}'::jsonb,NOW())
        ON CONFLICT (user_id,target_profile_id,event_type,period_key) DO NOTHING
        """,
        str(uuid.uuid4()),
        user_id,
        target_profile_id,
        event_type,
    )
    await conn.execute(
        f"UPDATE member_subscription_usage SET {column}={column}+1, updated_at=NOW() WHERE user_id=$1",
        user_id,
    )


async def _get_profile_id_for_user(conn, user_id: str) -> Optional[str]:
    return await conn.fetchval('SELECT profile_id FROM profiles WHERE user_id=$1 LIMIT 1', user_id)


async def _resolve_profile_id(conn, identifier: str) -> Optional[str]:
    return await conn.fetchval('SELECT profile_id FROM profiles WHERE profile_id=$1 OR user_id=$1 LIMIT 1', identifier)


async def _get_profile_summary(conn, profile_id: str) -> dict:
    row = await conn.fetchrow(
        'SELECT profile_id, user_id, first_name, last_name FROM profiles WHERE profile_id=$1 LIMIT 1',
        profile_id
    )
    return dict(row) if row else {}


async def _record_analytics(conn, event_type: str, user_id: Optional[str], payload: dict) -> None:
    await conn.execute(
        'INSERT INTO analytics_events (event_type, service_name, user_id, payload) VALUES ($1,$2,$3,$4::jsonb)',
        event_type,
        'matching-service',
        user_id,
        json.dumps(payload or {})
    )


async def _ensure_matching_outbox(conn) -> None:
    await conn.execute(
        """
        CREATE TABLE IF NOT EXISTS matching_outbox (
            outbox_id UUID PRIMARY KEY,
            event_type VARCHAR(80) NOT NULL,
            aggregate_id VARCHAR(120),
            payload JSONB NOT NULL DEFAULT '{}'::jsonb,
            status VARCHAR(24) NOT NULL DEFAULT 'pending',
            created_at TIMESTAMP DEFAULT NOW(),
            processed_at TIMESTAMP
        )
        """
    )


async def _queue_outbox(conn, event_type: str, aggregate_id: str, payload: dict) -> None:
    await _ensure_matching_outbox(conn)
    await conn.execute(
        """
        INSERT INTO matching_outbox (outbox_id,event_type,aggregate_id,payload,status,created_at)
        VALUES ($1,$2,$3,$4::jsonb,'pending',NOW())
        """,
        str(uuid.uuid4()),
        event_type,
        aggregate_id,
        json.dumps(payload or {}),
    )


async def _create_chat_conversation(sender_user_id: str, receiver_user_id: str, interest_id: str) -> Optional[str]:
    base_url = os.getenv('CHAT_API_URL', 'http://localhost:3004/api/v1/chat')
    service_secret = os.getenv('INTERNAL_SERVICE_SECRET', '')
    if not base_url or not service_secret:
        return None
    try:
        async with httpx.AsyncClient(timeout=3.0) as client:
            response = await client.post(
                f'{base_url.rstrip("/")}/internal/conversations',
                headers={'x-internal-service-secret': service_secret},
                json={
                    'participants': [str(sender_user_id), str(receiver_user_id)],
                    'interestId': str(interest_id),
                    'source': 'matching-service'
                }
            )
            response.raise_for_status()
            payload = response.json()
            return payload.get('data', {}).get('chatId')
    except Exception as exc:
        print(f'Chat conversation creation failed for interest={interest_id}: {exc}', flush=True)
        return None


async def _send_template_notification(user_id: str, template_key: str, variables: dict, data: Optional[dict] = None) -> None:
    base_url = os.getenv('NOTIFICATION_API_URL', 'http://localhost:3006/api/v1/notifications')
    service_secret = os.getenv('INTERNAL_SERVICE_SECRET', '')
    url = f'{base_url.rstrip("/")}/template'
    normalized_data = {str(key): '' if value is None else str(value) for key, value in (data or {}).items()}
    normalized_variables = {str(key): '' if value is None else str(value) for key, value in (variables or {}).items()}
    try:
        async with httpx.AsyncClient(timeout=3.0) as client:
            response = await client.post(
                url,
                headers={'x-internal-service-secret': service_secret} if service_secret else {},
                json={
                    'userId': str(user_id),
                    'templateKey': template_key,
                    'variables': normalized_variables,
                    'data': normalized_data
                }
            )
            response.raise_for_status()
    except Exception as exc:
        # Notification dispatch is best-effort so matching flow stays available.
        print(f'Notification dispatch failed for template={template_key} user={user_id}: {exc}', flush=True)
        return


async def send_interest(sender_user_id: str, receiver_id: str) -> dict:
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        sender_profile_id = await _get_profile_id_for_user(conn, sender_user_id)
        receiver_profile_id = await _resolve_profile_id(conn, receiver_id)
        if not sender_profile_id or not receiver_profile_id:
            raise ValueError("Both members need a valid profile before an interest can be sent")
        if sender_profile_id == receiver_profile_id:
            raise ValueError("You cannot send an interest to your own profile")
        existing = await conn.fetchrow('SELECT * FROM interests WHERE sender_id=$1 AND receiver_id=$2', sender_profile_id, receiver_profile_id)
        resent = False
        if existing:
            existing_status = str(existing["status"] or "").lower()
            if existing_status == "declined":
                interest_id = str(existing["interest_id"])
                resent = True
                await conn.execute(
                    """
                    UPDATE interests
                    SET status='pending', sent_at=NOW(), responded_at=NULL
                    WHERE interest_id=$1
                    """,
                    interest_id
                )
            else:
                return {
                    "status": "already_sent",
                    "interestId": str(existing["interest_id"]),
                    "existingStatus": existing_status or "pending"
                }
        else:
            plan_id = await _active_plan_id(conn, sender_user_id)
            entitlements = await _member_entitlements(conn, plan_id)
            await _consume_limit(
                conn,
                sender_user_id,
                receiver_profile_id,
                "interest",
                "interests_used",
                entitlements["interests"],
            )
            interest_id = str(uuid.uuid4())
            await conn.execute('INSERT INTO interests (interest_id,sender_id,receiver_id) VALUES ($1,$2,$3)', interest_id, sender_profile_id, receiver_profile_id)
        mutual = await conn.fetchrow("SELECT * FROM interests WHERE sender_id=$1 AND receiver_id=$2 AND status='accepted'", receiver_profile_id, sender_profile_id)
        sender_profile = await _get_profile_summary(conn, sender_profile_id)
        receiver_profile = await _get_profile_summary(conn, receiver_profile_id)
        if receiver_profile.get('user_id'):
            await _send_template_notification(
                receiver_profile['user_id'],
                'someone_liked_you',
                {'name': f"{sender_profile.get('first_name', 'Someone')} {sender_profile.get('last_name', '')}".strip()},
                {
                    'type': 'interest_received',
                    'interestId': interest_id,
                    'profileId': str(sender_profile.get('profile_id', '')),
                    'senderUserId': str(sender_profile.get('user_id', ''))
                }
            )
        return {"status": "interest_resent" if resent else "interest_sent", "interestId": interest_id, "isMutual": mutual is not None}


async def get_received_interests(user_id: str) -> list:
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        current_profile_id = await _get_profile_id_for_user(conn, user_id)
        if not current_profile_id:
            return []
        rows = await conn.fetch(
            '''
            SELECT
                i.interest_id,
                i.status,
                i.sent_at,
                p.profile_id,
                p.user_id,
                p.first_name,
                p.last_name,
                p.primary_photo_url,
                COALESCE(ec.occupation, '') AS occupation,
                COALESCE(ec.working_city, '') AS working_city,
                COALESCE(fd.family_city, '') AS family_city
            FROM interests i
            JOIN profiles p ON p.profile_id=i.sender_id
            LEFT JOIN education_career ec ON ec.profile_id=p.profile_id
            LEFT JOIN family_details fd ON fd.profile_id=p.profile_id
            WHERE i.receiver_id=$1
            ORDER BY i.sent_at DESC
            ''',
            current_profile_id
        )
        return [dict(r) for r in rows]


async def get_sent_interests(user_id: str) -> list:
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        current_profile_id = await _get_profile_id_for_user(conn, user_id)
        if not current_profile_id:
            return []
        rows = await conn.fetch(
            '''
            SELECT
                i.interest_id,
                i.status,
                i.sent_at,
                p.profile_id,
                p.user_id,
                p.first_name,
                p.last_name,
                p.primary_photo_url,
                COALESCE(ec.occupation, '') AS occupation,
                COALESCE(ec.working_city, '') AS working_city,
                COALESCE(fd.family_city, '') AS family_city
            FROM interests i
            JOIN profiles p ON p.profile_id=i.receiver_id
            LEFT JOIN education_career ec ON ec.profile_id=p.profile_id
            LEFT JOIN family_details fd ON fd.profile_id=p.profile_id
            WHERE i.sender_id=$1
            ORDER BY i.sent_at DESC
            ''',
            current_profile_id
        )
        return [dict(r) for r in rows]


async def respond_to_interest(interest_id: str, status: str, user_id: str) -> dict:
    if status not in ("accepted", "declined"):
        raise ValueError("Status must be accepted or declined")
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        current_profile_id = await _get_profile_id_for_user(conn, user_id)
        if not current_profile_id:
            raise ValueError("Profile not found")
        sender_profile = {}
        receiver_profile = {}
        async with conn.transaction():
            interest = await conn.fetchrow(
                'SELECT sender_id, receiver_id FROM interests WHERE interest_id=$1 AND receiver_id=$2 FOR UPDATE',
                interest_id,
                current_profile_id
            )
            if not interest:
                raise ValueError("Interest not found for this profile")
            await conn.execute('UPDATE interests SET status=$1,responded_at=NOW() WHERE interest_id=$2 AND receiver_id=$3', status, interest_id, current_profile_id)
            sender_profile = await _get_profile_summary(conn, interest['sender_id'])
            receiver_profile = await _get_profile_summary(conn, interest['receiver_id'])
            if status == 'accepted':
                await _record_analytics(
                    conn,
                    'match_made',
                    receiver_profile.get('user_id'),
                    {
                        'interestId': interest_id,
                        'senderUserId': sender_profile.get('user_id'),
                        'receiverUserId': receiver_profile.get('user_id')
                    }
                )
                await _queue_outbox(
                    conn,
                    'chat_conversation_create',
                    interest_id,
                    {
                        'interestId': interest_id,
                        'senderUserId': str(sender_profile.get('user_id', '')),
                        'receiverUserId': str(receiver_profile.get('user_id', ''))
                    }
                )
                await _queue_outbox(
                    conn,
                    'notification_template',
                    interest_id,
                    {
                        'userId': str(sender_profile.get('user_id', '')),
                        'templateKey': 'match_made',
                        'profileId': str(receiver_profile.get('profile_id', ''))
                    }
                )
            if status == 'declined':
                await _queue_outbox(
                    conn,
                    'notification_template',
                    interest_id,
                    {
                        'userId': str(sender_profile.get('user_id', '')),
                        'templateKey': 'interest_declined',
                        'profileId': str(receiver_profile.get('profile_id', ''))
                    }
                )
        chat_id = None
        if status == 'accepted' and sender_profile.get('user_id') and receiver_profile.get('user_id'):
            chat_id = await _create_chat_conversation(sender_profile['user_id'], receiver_profile['user_id'], interest_id)
            if sender_profile.get('user_id'):
                await _send_template_notification(
                    sender_profile['user_id'],
                    'match_made',
                    {'name': f"{receiver_profile.get('first_name', 'A member')} {receiver_profile.get('last_name', '')}".strip()},
                    {
                        'type': 'interest_accepted',
                        'interestId': interest_id,
                        'profileId': str(receiver_profile.get('profile_id', '')),
                        'receiverUserId': str(receiver_profile.get('user_id', '')),
                        'chatId': str(chat_id or '')
                    }
                )
        if status == 'declined' and sender_profile.get('user_id'):
            await _send_template_notification(
                sender_profile['user_id'],
                'interest_declined',
                {'name': f"{receiver_profile.get('first_name', 'A member')} {receiver_profile.get('last_name', '')}".strip()},
                {
                    'type': 'interest_declined',
                    'interestId': interest_id,
                    'profileId': str(receiver_profile.get('profile_id', '')),
                    'receiverUserId': str(receiver_profile.get('user_id', ''))
                }
            )
        return {"status": status, "interestId": interest_id, "chatId": chat_id}


async def toggle_shortlist(user_id: str, profile_id: str) -> dict:
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        resolved_profile_id = await _resolve_profile_id(conn, profile_id)
        if not resolved_profile_id:
            raise ValueError("Target profile not found")
        existing = await conn.fetchrow('SELECT * FROM shortlists WHERE added_by=$1 AND profile_id=$2', user_id, resolved_profile_id)
        if existing:
            await conn.execute('DELETE FROM shortlists WHERE added_by=$1 AND profile_id=$2', user_id, resolved_profile_id)
            return {"action": "removed"}
        plan_id = await _active_plan_id(conn, user_id)
        entitlements = await _member_entitlements(conn, plan_id)
        await _consume_limit(
            conn,
            user_id,
            resolved_profile_id,
            "shortlist",
            "shortlists_used",
            entitlements["shortlist"],
        )
        await conn.execute('INSERT INTO shortlists (shortlist_id,added_by,profile_id) VALUES ($1,$2,$3)', str(uuid.uuid4()), user_id, resolved_profile_id)
        return {"action": "added"}


async def get_shortlist(user_id: str) -> list:
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch('SELECT p.profile_id,p.user_id,p.first_name,p.last_name,p.primary_photo_url,s.added_at FROM shortlists s JOIN profiles p ON p.profile_id=s.profile_id WHERE s.added_by=$1 ORDER BY s.added_at DESC', user_id)
        return [dict(r) for r in rows]
