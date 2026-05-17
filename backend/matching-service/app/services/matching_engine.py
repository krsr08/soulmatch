import json
from datetime import datetime, timezone

import numpy as np
from app.config.database import get_db_pool
from app.services.feed_cache import get_feed, set_feed

RELIGION_MAP = {"Hindu": 0, "Muslim": 1, "Christian": 2, "Sikh": 3, "Buddhist": 4, "Jain": 5, "Other": 6}
EDUCATION_MAP = {"High School": 1, "Graduate": 2, "Post Graduate": 3, "Doctorate": 4, "Professional": 5}
INCOME_MAP = {"< 3 LPA": 1, "3-5 LPA": 2, "5-10 LPA": 3, "10-20 LPA": 4, "20+ LPA": 5}

DEFAULT_PLAN_ENTITLEMENTS = {
    "free": {"visibleMatches": 80},
    "bronze": {"visibleMatches": 80},
    "silver": {"visibleMatches": 80},
    "gold": {"visibleMatches": 80},
    "platinum": {"visibleMatches": 80},
}


def normalize_plan_id(plan_id):
    value = str(plan_id or "bronze").lower()
    return "bronze" if value == "free" else value if value in DEFAULT_PLAN_ENTITLEMENTS else "bronze"


def config_dict(value):
    if isinstance(value, dict):
        return value
    if isinstance(value, str):
        try:
            return json.loads(value)
        except Exception:
            return {}
    return {}


async def active_plan_id(conn, user_id: str) -> str:
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
    return normalize_plan_id(row)


async def visible_match_limit(conn, user_id: str) -> int:
    plan_id = await active_plan_id(conn, user_id)
    monetization = config_dict(
        await conn.fetchval("SELECT config_value FROM app_config WHERE config_key='monetization' LIMIT 1")
    )
    configured = config_dict(monetization.get("memberPlanEntitlements"))
    plan_config = config_dict(configured.get(plan_id)) or DEFAULT_PLAN_ENTITLEMENTS.get(plan_id, DEFAULT_PLAN_ENTITLEMENTS["bronze"])
    try:
        return max(0, int(plan_config.get("visibleMatches") or DEFAULT_PLAN_ENTITLEMENTS[plan_id]["visibleMatches"]))
    except Exception:
        return DEFAULT_PLAN_ENTITLEMENTS["bronze"]["visibleMatches"]


def number_or(value, fallback):
    return fallback if value is None else value


def clamp_score(value):
    return max(0, min(100, int(round(float(value or 0)))))


def normalize_list(value):
    if not value:
        return []
    if isinstance(value, str):
        return [item.strip().lower() for item in value.split(",") if item.strip()]
    return [str(item or "").strip().lower() for item in value if str(item or "").strip()]


def contains_any(values, *candidates):
    normalized = set(normalize_list(values))
    if not normalized:
        return True
    return any(str(candidate or "").strip().lower() in normalized for candidate in candidates if candidate)


def last_active_label(value):
    if not value:
        return "Recently Active"
    try:
        if isinstance(value, str):
            cleaned = value.replace("Z", "+00:00")
            active_at = datetime.fromisoformat(cleaned)
        else:
            active_at = value
        if active_at.tzinfo is None:
            active_at = active_at.replace(tzinfo=timezone.utc)
        minutes = (datetime.now(timezone.utc) - active_at.astimezone(timezone.utc)).total_seconds() / 60
        if minutes <= 15:
            return "Active"
        if minutes <= 60 * 24 * 3:
            return "Recently Active"
        return "Active Recently"
    except Exception:
        return "Recently Active"


def income_rank(value):
    if value is None:
        return None
    if isinstance(value, (int, float)):
        return int(value)
    text = str(value)
    if "20+" in text:
        return 20
    for marker in ["10-20", "5-10", "3-5", "< 3"]:
        if marker in text:
            return {"10-20": 10, "5-10": 5, "3-5": 3, "< 3": 1}[marker]
    digits = "".join(ch for ch in text if ch.isdigit())
    return int(digits) if digits else None


def build_vector(p):
    age = number_or(p.get("age"), 25)
    height_cm = number_or(p.get("height_cm"), 165)
    return np.array(
        [
            max(0, min(1, (age - 18) / 42)),
            RELIGION_MAP.get(p.get("religion", "Other"), 6) / 6.0,
            max(0, min(1, (height_cm - 140) / 60)),
            EDUCATION_MAP.get(p.get("education_level", "Graduate"), 2) / 5.0,
            INCOME_MAP.get(p.get("annual_income", "5-10 LPA"), 3) / 5.0,
            1.0 if p.get("is_manglik") else 0.0,
            1.0 if p.get("diet") == "vegetarian" else 0.0,
        ],
        dtype=np.float32,
    )


def pref_score(candidate, prefs):
    score = 100.0
    age = number_or(candidate.get("age"), 25)
    age_min = number_or(prefs.get("age_min"), 18)
    age_max = number_or(prefs.get("age_max"), 50)
    preferred_religion = prefs.get("pref_religion") or prefs.get("religion")
    preferred_education = normalize_list(prefs.get("education_levels"))
    preferred_occupations = normalize_list(prefs.get("occupations"))
    preferred_locations = normalize_list(prefs.get("locations"))
    preferred_diets = normalize_list(prefs.get("diet_prefs"))
    preferred_marital_statuses = normalize_list(prefs.get("marital_statuses"))
    preferred_family_types = normalize_list(prefs.get("family_types"))
    candidate_income = income_rank(candidate.get("annual_income"))
    income_min = income_rank(prefs.get("annual_income_min"))
    income_max = income_rank(prefs.get("annual_income_max"))
    if age < age_min or age > age_max:
        score -= 30
    if preferred_religion and preferred_religion != candidate.get("religion"):
        score -= 15
    if prefs.get("height_min_cm") and candidate.get("height_cm") and candidate.get("height_cm") < prefs.get("height_min_cm"):
        score -= 10
    if prefs.get("height_max_cm") and candidate.get("height_cm") and candidate.get("height_cm") > prefs.get("height_max_cm"):
        score -= 10
    if preferred_education and not contains_any(preferred_education, candidate.get("education_level")):
        score -= 10
    if preferred_occupations and not contains_any(preferred_occupations, candidate.get("occupation")):
        score -= 8
    if candidate_income is not None and income_min is not None and candidate_income < income_min:
        score -= 8
    if candidate_income is not None and income_max is not None and candidate_income > income_max:
        score -= 5
    if preferred_locations and not contains_any(preferred_locations, candidate.get("working_city"), candidate.get("family_city")):
        score -= 10
    if preferred_diets and not contains_any(preferred_diets, candidate.get("diet")):
        score -= 6
    if preferred_marital_statuses and not contains_any(preferred_marital_statuses, candidate.get("marital_status")):
        score -= 8
    if preferred_family_types and not contains_any(preferred_family_types, candidate.get("family_type")):
        score -= 5
    manglik_pref = str(prefs.get("manglik_pref") or "any").lower()
    if manglik_pref in ["yes", "manglik"] and not candidate.get("is_manglik"):
        score -= 12
    if manglik_pref in ["no", "non_manglik", "non-manglik"] and candidate.get("is_manglik"):
        score -= 12
    return max(0.0, score)


def horoscope_score(p1, p2):
    return 60.0 if p1.get("is_manglik") != p2.get("is_manglik") else 80.0


def cosine_sim(v1, v2):
    n1, n2 = np.linalg.norm(v1), np.linalg.norm(v2)
    if n1 == 0 or n2 == 0:
        return 0.5
    return float(np.dot(v1, v2) / (n1 * n2))


def build_trust_summary(profile):
    score = 0
    signals = []
    verification_status = str(profile.get("verification_status") or "pending").lower()
    completion_score = clamp_score(profile.get("completion_score"))
    photo_count = int(profile.get("photo_count") or 0)
    approved_verifications = int(profile.get("approved_verifications") or 0)
    approved_types = [str(item).lower() for item in (profile.get("approved_verification_types") or []) if item]
    pending_verifications = int(profile.get("pending_verifications") or 0)
    report_count = int(profile.get("report_count") or 0)

    if profile.get("is_phone_verified"):
        score += 15
        signals.append("Phone verified")
    if profile.get("firebase_verified"):
        score += 8
        signals.append("Firebase identity linked")
    if completion_score >= 90:
        score += 20
        signals.append("Highly complete profile")
    elif completion_score >= 70:
        score += 15
        signals.append("Strong profile detail")
    elif completion_score >= 50:
        score += 10
    if verification_status == "verified":
        score += 25
        signals.append("Admin verified")
    elif pending_verifications > 0 or verification_status == "pending":
        score += 8
    if approved_verifications > 0:
        score += min(15, approved_verifications * 5)
    if "education" in approved_types:
        score += 6
        signals.append("Education verified")
    if "income" in approved_types:
        score += 6
        signals.append("Income verified")
    if "family" in approved_types:
        score += 6
        signals.append("Family verified")
    if photo_count >= 3:
        score += 12
        signals.append("Multiple photos")
    elif photo_count >= 1 or profile.get("primary_photo_url"):
        score += 8
        signals.append("Photo added")
    if profile.get("family_city") or profile.get("family_pincode"):
        score += 8
        signals.append("Family location available")
    if (profile.get("profile_status") or "active") == "active":
        score += 5
    if report_count == 0:
        score += 10
        signals.append("No open safety reports")
    else:
        score -= min(35, report_count * 12)

    trust_score = clamp_score(score)
    return {
        "trustScore": trust_score,
        "trustLevel": "high" if trust_score >= 80 else "medium" if trust_score >= 55 else "low",
        "trustSignals": signals[:4],
    }


def build_match_reasons(user, candidate, preference_score, vector_score, horoscope, trust):
    reasons = []
    age = number_or(candidate.get("age"), 0)
    age_min = number_or(user.get("age_min"), 18)
    age_max = number_or(user.get("age_max"), 50)
    preferred_religion = user.get("pref_religion")

    if age_min <= age <= age_max:
        reasons.append("Fits your preferred age range")
    if preferred_religion and preferred_religion == candidate.get("religion"):
        reasons.append("Matches your religion preference")
    if contains_any(user.get("education_levels"), candidate.get("education_level")) and user.get("education_levels"):
        reasons.append("Education preference aligned")
    if contains_any(user.get("occupations"), candidate.get("occupation")) and user.get("occupations"):
        reasons.append("Career preference aligned")
    if contains_any(user.get("locations"), candidate.get("working_city"), candidate.get("family_city")) and user.get("locations"):
        reasons.append("Location preference aligned")
    if user.get("working_city") and user.get("working_city") == candidate.get("working_city"):
        reasons.append("Same working city")
    if user.get("diet") and user.get("diet") == candidate.get("diet"):
        reasons.append("Lifestyle preference aligned")
    if horoscope >= 80:
        reasons.append("Horoscope signal looks favorable")
    if trust["trustScore"] >= 80:
        reasons.append("High trust profile")
    elif candidate.get("verification_status") == "verified":
        reasons.append("Verified profile")
    if vector_score >= 80 and preference_score >= 80:
        reasons.append("Strong overall compatibility")
    return reasons[:4]


async def get_recommended_matches(user_id: str, page: int, limit: int, verified_only: bool = False) -> dict:
    page = max(int(page or 1), 1)
    limit = min(max(int(limit or 25), 1), 50)
    cached = await get_feed(user_id, page, limit, verified_only)
    if cached is not None:
        return cached
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        user_row = await conn.fetchrow(
            """
            SELECT p.*,EXTRACT(YEAR FROM AGE(p.dob))::int AS age,
                   ec.education_level,ec.annual_income,ec.working_city,
                   pd.height_cm,ld.diet,hd.is_manglik,
                   pp.age_min,pp.age_max,pp.religion AS pref_religion,pp.manglik_pref,
                   pp.education_levels,pp.occupations,pp.annual_income_min,pp.annual_income_max,
                   pp.height_min_cm,pp.height_max_cm,pp.locations,pp.diet_prefs,
                   pp.marital_statuses,pp.family_types
            FROM profiles p
            LEFT JOIN education_career ec ON p.profile_id=ec.profile_id
            LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id
            LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id
            LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id
            LEFT JOIN partner_preferences pp ON p.profile_id=pp.profile_id
            WHERE p.user_id=$1
            LIMIT 1
            """,
            user_id,
        )
        if not user_row:
            return {"matches": [], "total": 0, "page": page}
        user = dict(user_row)
        gender = (user.get("gender") or "").lower()
        opp_gender = "female" if gender == "male" else "male" if gender == "female" else None
        access_limit = await visible_match_limit(conn, user_id)
        candidates = await conn.fetch(
            """
            SELECT p.*,EXTRACT(YEAR FROM AGE(p.dob))::int AS age,
                   ec.education_level,ec.annual_income,ec.occupation,ec.working_city,
                   pd.height_cm,ld.diet,hd.is_manglik,fd.family_city,fd.family_state,fd.family_pincode,fd.family_type,
                   u.is_verified AS is_phone_verified,
                   (u.google_id IS NOT NULL) AS firebase_verified,
                   u.last_login,
                   COALESCE((SELECT COUNT(*)::int FROM profile_photos pp WHERE pp.profile_id=p.profile_id), 0) AS photo_count,
                   COALESCE((SELECT COUNT(*)::int FROM verifications v WHERE v.user_id=p.user_id AND v.status IN ('approved','verified')), 0) AS approved_verifications,
                   COALESCE((SELECT array_agg(DISTINCT v.type) FROM verifications v WHERE v.user_id=p.user_id AND v.status IN ('approved','verified')), ARRAY[]::text[]) AS approved_verification_types,
                   COALESCE((SELECT COUNT(*)::int FROM verifications v WHERE v.user_id=p.user_id AND v.status='pending'), 0) AS pending_verifications,
                   COALESCE((SELECT COUNT(*)::int FROM reports rp WHERE rp.reported_id=p.user_id AND rp.status IN ('pending','open','reviewing')), 0) AS report_count,
                   CASE
                     WHEN COALESCE(p.photo_privacy,'all')='all'
                       OR (
                         COALESCE(p.photo_privacy,'all')='matches_only'
                         AND EXISTS (
                           SELECT 1 FROM interests i
                           WHERE ((i.sender_id=$3 AND i.receiver_id=p.profile_id) OR (i.sender_id=p.profile_id AND i.receiver_id=$3))
                             AND i.status='accepted'
                         )
                       )
                       OR EXISTS (
                         SELECT 1 FROM profile_photo_access_requests par
                         WHERE par.target_profile_id=p.profile_id
                           AND par.requester_user_id=$1
                           AND par.status='approved'
                           AND (par.expires_at IS NULL OR par.expires_at > NOW())
                       )
                     THEN p.primary_photo_url
                     ELSE NULL
                   END AS visible_primary_photo_url,
                   CASE
                     WHEN COALESCE(p.photo_privacy,'all')='all'
                       OR (
                         COALESCE(p.photo_privacy,'all')='matches_only'
                         AND EXISTS (
                           SELECT 1 FROM interests i
                           WHERE ((i.sender_id=$3 AND i.receiver_id=p.profile_id) OR (i.sender_id=p.profile_id AND i.receiver_id=$3))
                             AND i.status='accepted'
                         )
                       )
                       OR EXISTS (
                         SELECT 1 FROM profile_photo_access_requests par
                         WHERE par.target_profile_id=p.profile_id
                           AND par.requester_user_id=$1
                           AND par.status='approved'
                           AND (par.expires_at IS NULL OR par.expires_at > NOW())
                       )
                     THEN FALSE
                     ELSE TRUE
                   END AS is_photo_private,
                   p.photo_privacy,COALESCE(p.verification_status,'pending')='verified' AS is_verified
            FROM profiles p
            JOIN users u ON u.user_id=p.user_id
            LEFT JOIN education_career ec ON p.profile_id=ec.profile_id
            LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id
            LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id
            LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id
            LEFT JOIN family_details fd ON p.profile_id=fd.profile_id
            WHERE ($2::text IS NULL OR p.gender=$2)
              AND p.is_published=true
              AND COALESCE(p.admin_status,'active')='active'
              AND COALESCE(p.profile_status,'active')='active'
              AND COALESCE(p.profile_visibility,'all')!='hidden'
              AND (
                COALESCE(p.profile_visibility,'all')!='matches_only'
                OR EXISTS (
                  SELECT 1 FROM interests i
                  WHERE ((i.sender_id=$3 AND i.receiver_id=p.profile_id) OR (i.sender_id=p.profile_id AND i.receiver_id=$3))
                    AND i.status='accepted'
                )
              )
              AND u.is_active=true
              AND COALESCE(u.is_banned,false)=false
              AND ($4::boolean=false OR COALESCE(p.verification_status,'pending')='verified')
              AND ($5::int IS NULL OR EXTRACT(YEAR FROM AGE(p.dob))::int >= $5)
              AND ($6::int IS NULL OR EXTRACT(YEAR FROM AGE(p.dob))::int <= $6)
              AND ($7::int IS NULL OR pd.height_cm >= $7)
              AND ($8::int IS NULL OR pd.height_cm <= $8)
              AND ($9::text IS NULL OR $9='' OR p.religion=$9)
              AND (COALESCE(array_length($10::text[], 1), 0)=0 OR LOWER(COALESCE(ec.education_level,''))=ANY($10::text[]))
              AND (COALESCE(array_length($11::text[], 1), 0)=0 OR LOWER(COALESCE(ec.occupation,''))=ANY($11::text[]))
              AND (COALESCE(array_length($12::text[], 1), 0)=0 OR LOWER(COALESCE(ec.working_city,''))=ANY($12::text[]) OR LOWER(COALESCE(fd.family_city,''))=ANY($12::text[]))
              AND (COALESCE(array_length($13::text[], 1), 0)=0 OR LOWER(COALESCE(ld.diet,''))=ANY($13::text[]))
              AND (COALESCE(array_length($14::text[], 1), 0)=0 OR LOWER(COALESCE(p.marital_status,''))=ANY($14::text[]))
              AND (COALESCE(array_length($15::text[], 1), 0)=0 OR LOWER(COALESCE(fd.family_type,''))=ANY($15::text[]))
              AND p.user_id!=$1
              AND NOT EXISTS (
                SELECT 1 FROM blocks b
                WHERE (b.blocker_id=$1 AND b.blocked_id=p.user_id)
                   OR (b.blocker_id=p.user_id AND b.blocked_id=$1)
              )
            ORDER BY p.completion_score DESC, u.last_login DESC NULLS LAST, p.updated_at DESC
            LIMIT 1000
            """,
            user_id,
            opp_gender,
            user["profile_id"],
            verified_only,
            user.get("age_min"),
            user.get("age_max"),
            user.get("height_min_cm"),
            user.get("height_max_cm"),
            user.get("pref_religion"),
            normalize_list(user.get("education_levels")),
            normalize_list(user.get("occupations")),
            normalize_list(user.get("locations")),
            normalize_list(user.get("diet_prefs")),
            normalize_list(user.get("marital_statuses")),
            normalize_list(user.get("family_types")),
        )
        user_vec = build_vector(user)
        scored = []
        for c in candidates:
            cd = dict(c)
            cv = build_vector(cd)
            vec_s = cosine_sim(user_vec, cv) * 100
            pre_s = pref_score(cd, user)
            hor_s = horoscope_score(user, cd)
            trust = build_trust_summary(cd)
            total = int(vec_s * 0.35 + pre_s * 0.40 + hor_s * 0.25)
            scored.append(
                {
                    "profileId": str(cd["profile_id"]),
                    "userId": str(cd["user_id"]),
                    "name": f"{cd.get('first_name') or 'Member'} {(cd.get('last_name') or '')[:1]}.".strip(),
                    "age": cd.get("age", 0),
                    "gender": cd.get("gender") or "",
                    "heightCm": cd.get("height_cm"),
                    "location": cd.get("working_city", ""),
                    "occupation": cd.get("occupation", ""),
                    "education": cd.get("education_level") or "",
                    "community": cd.get("caste") or cd.get("religion") or "",
                    "religion": cd.get("religion") or "",
                    "annualIncome": cd.get("annual_income") or "",
                    "familyCity": cd.get("family_city") or "",
                    "familyState": cd.get("family_state") or "",
                    "maritalStatus": cd.get("marital_status") or "",
                    "diet": cd.get("diet") or "",
                    "isManglik": bool(cd.get("is_manglik")),
                    "createdAt": cd.get("created_at").isoformat() if cd.get("created_at") else "",
                    "hideLastSeen": bool(cd.get("hide_last_seen")),
                    "primaryPhoto": cd.get("visible_primary_photo_url") or "",
                    "isVerified": bool(cd.get("is_verified")),
                    "isPhotoPrivate": bool(cd.get("is_photo_private")),
                    "profileCreatedBy": cd.get("profile_created_by") or "self",
                    "lastActiveLabel": last_active_label(cd.get("last_login")),
                    "trustScore": trust["trustScore"],
                    "trustLevel": trust["trustLevel"],
                    "trustSignals": trust["trustSignals"],
                    "matchReasons": build_match_reasons(user, cd, pre_s, vec_s, hor_s, trust),
                    "compatibilityScore": min(99, total),
                    "compatibilityBreakdown": {
                        "preferences": int(pre_s),
                        "personality": int(vec_s),
                        "horoscope": int(hor_s),
                    },
                }
            )
        scored.sort(key=lambda x: (x["compatibilityScore"], x["trustScore"]), reverse=True)
        scored = scored[:access_limit]
        start = (page - 1) * limit
        result = {"matches": scored[start : start + limit], "total": len(scored), "page": page, "limit": limit}
        await set_feed(user_id, page, limit, verified_only, result)
        return result


async def get_compatibility(user_id: str, target_profile_id: str) -> dict:
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        user_row = await conn.fetchrow(
            """
            SELECT p.*,EXTRACT(YEAR FROM AGE(p.dob))::int AS age,
                   ec.education_level,ec.annual_income,ec.occupation,ec.working_city,
                   pd.height_cm,ld.diet,hd.is_manglik,fd.family_city,fd.family_pincode,fd.family_type,
                   pp.age_min,pp.age_max,pp.religion AS pref_religion,pp.manglik_pref,
                   pp.education_levels,pp.occupations,pp.annual_income_min,pp.annual_income_max,
                   pp.height_min_cm,pp.height_max_cm,pp.locations,pp.diet_prefs,
                   pp.marital_statuses,pp.family_types
            FROM profiles p
            LEFT JOIN education_career ec ON p.profile_id=ec.profile_id
            LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id
            LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id
            LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id
            LEFT JOIN family_details fd ON p.profile_id=fd.profile_id
            LEFT JOIN partner_preferences pp ON p.profile_id=pp.profile_id
            WHERE p.user_id=$1
            LIMIT 1
            """,
            user_id,
        )
        target_row = await conn.fetchrow(
            """
            SELECT p.*,EXTRACT(YEAR FROM AGE(p.dob))::int AS age,
                   ec.education_level,ec.annual_income,ec.occupation,ec.working_city,
                   pd.height_cm,ld.diet,hd.is_manglik,fd.family_city,fd.family_pincode,fd.family_type,
                   u.is_verified AS is_phone_verified,
                   (u.google_id IS NOT NULL) AS firebase_verified,
                   u.last_login,
                   COALESCE((SELECT COUNT(*)::int FROM profile_photos pp WHERE pp.profile_id=p.profile_id), 0) AS photo_count,
                   COALESCE((SELECT COUNT(*)::int FROM verifications v WHERE v.user_id=p.user_id AND v.status IN ('approved','verified')), 0) AS approved_verifications,
                   COALESCE((SELECT array_agg(DISTINCT v.type) FROM verifications v WHERE v.user_id=p.user_id AND v.status IN ('approved','verified')), ARRAY[]::text[]) AS approved_verification_types,
                   COALESCE((SELECT COUNT(*)::int FROM verifications v WHERE v.user_id=p.user_id AND v.status='pending'), 0) AS pending_verifications,
                   COALESCE((SELECT COUNT(*)::int FROM reports rp WHERE rp.reported_id=p.user_id AND rp.status IN ('pending','open','reviewing')), 0) AS report_count
            FROM profiles p
            JOIN users u ON u.user_id=p.user_id
            LEFT JOIN education_career ec ON p.profile_id=ec.profile_id
            LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id
            LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id
            LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id
            LEFT JOIN family_details fd ON p.profile_id=fd.profile_id
            WHERE p.profile_id=$1
              AND p.is_published=true
              AND COALESCE(p.admin_status,'active')='active'
              AND COALESCE(p.profile_status,'active')='active'
              AND COALESCE(p.profile_visibility,'all')!='hidden'
              AND (
                COALESCE(p.profile_visibility,'all')!='matches_only'
                OR EXISTS (
                  SELECT 1 FROM interests i
                  JOIN profiles viewer ON viewer.user_id=$2
                  WHERE ((i.sender_id=viewer.profile_id AND i.receiver_id=p.profile_id) OR (i.sender_id=p.profile_id AND i.receiver_id=viewer.profile_id))
                    AND i.status='accepted'
                )
              )
              AND NOT EXISTS (
                SELECT 1 FROM blocks b
                WHERE (b.blocker_id=$2 AND b.blocked_id=p.user_id)
                   OR (b.blocker_id=p.user_id AND b.blocked_id=$2)
              )
            """,
            target_profile_id,
            user_id,
        )
        if not user_row or not target_row:
            return {"overallScore": 0}
        u = dict(user_row)
        t = dict(target_row)
        vec_s = cosine_sim(build_vector(u), build_vector(t)) * 100
        pre_s = pref_score(t, u)
        hor_s = horoscope_score(u, t)
        trust = build_trust_summary(t)
        total = int(vec_s * 0.35 + pre_s * 0.40 + hor_s * 0.25)
        return {
            "overallScore": min(99, total),
            "breakdown": {"preferences": int(pre_s), "personality": int(vec_s), "horoscope": int(hor_s)},
            "explanations": build_match_reasons(u, t, pre_s, vec_s, hor_s, trust),
        }
