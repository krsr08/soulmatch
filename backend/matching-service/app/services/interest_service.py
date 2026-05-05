import uuid
import json
import os
from typing import Optional

import httpx

from app.config.database import get_db_pool


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
        interest = await conn.fetchrow(
            'SELECT sender_id, receiver_id FROM interests WHERE interest_id=$1 AND receiver_id=$2',
            interest_id,
            current_profile_id
        )
        if not interest:
            raise ValueError("Interest not found for this profile")
        await conn.execute('UPDATE interests SET status=$1,responded_at=NOW() WHERE interest_id=$2 AND receiver_id=$3', status, interest_id, current_profile_id)
        sender_profile = await _get_profile_summary(conn, interest['sender_id'])
        receiver_profile = await _get_profile_summary(conn, interest['receiver_id'])
        if status == 'accepted' and interest:
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
            if sender_profile.get('user_id'):
                await _send_template_notification(
                    sender_profile['user_id'],
                    'match_made',
                    {'name': f"{receiver_profile.get('first_name', 'A member')} {receiver_profile.get('last_name', '')}".strip()},
                    {
                        'type': 'interest_accepted',
                        'interestId': interest_id,
                        'profileId': str(receiver_profile.get('profile_id', '')),
                        'receiverUserId': str(receiver_profile.get('user_id', ''))
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
        return {"status": status, "interestId": interest_id}


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
        await conn.execute('INSERT INTO shortlists (shortlist_id,added_by,profile_id) VALUES ($1,$2,$3)', str(uuid.uuid4()), user_id, resolved_profile_id)
        return {"action": "added"}


async def get_shortlist(user_id: str) -> list:
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        rows = await conn.fetch('SELECT p.profile_id,p.user_id,p.first_name,p.last_name,p.primary_photo_url,s.added_at FROM shortlists s JOIN profiles p ON p.profile_id=s.profile_id WHERE s.added_by=$1 ORDER BY s.added_at DESC', user_id)
        return [dict(r) for r in rows]
