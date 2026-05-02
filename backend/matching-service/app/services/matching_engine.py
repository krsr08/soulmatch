import numpy as np
from app.config.database import get_db_pool
RELIGION_MAP = {"Hindu":0,"Muslim":1,"Christian":2,"Sikh":3,"Buddhist":4,"Jain":5,"Other":6}
EDUCATION_MAP = {"High School":1,"Graduate":2,"Post Graduate":3,"Doctorate":4,"Professional":5}
INCOME_MAP = {"< 3 LPA":1,"3-5 LPA":2,"5-10 LPA":3,"10-20 LPA":4,"20+ LPA":5}

def number_or(value, fallback):
    return fallback if value is None else value

def build_vector(p):
    age = number_or(p.get("age"), 25)
    height_cm = number_or(p.get("height_cm"), 165)
    return np.array([max(0,min(1,(age-18)/42)), RELIGION_MAP.get(p.get("religion","Other"),6)/6.0, max(0,min(1,(height_cm-140)/60)), EDUCATION_MAP.get(p.get("education_level","Graduate"),2)/5.0, INCOME_MAP.get(p.get("annual_income","5-10 LPA"),3)/5.0, 1.0 if p.get("is_manglik") else 0.0, 1.0 if p.get("diet")=="vegetarian" else 0.0], dtype=np.float32)
def pref_score(candidate, prefs):
    score = 100.0
    age = number_or(candidate.get("age"), 25)
    age_min = number_or(prefs.get("age_min"), 18)
    age_max = number_or(prefs.get("age_max"), 50)
    preferred_religion = prefs.get("pref_religion") or prefs.get("religion")
    if age < age_min or age > age_max: score -= 30
    if preferred_religion and preferred_religion != candidate.get("religion"): score -= 15
    return max(0.0, score)
def horoscope_score(p1, p2):
    return 60.0 if p1.get("is_manglik") != p2.get("is_manglik") else 80.0
def cosine_sim(v1, v2):
    n1, n2 = np.linalg.norm(v1), np.linalg.norm(v2)
    if n1 == 0 or n2 == 0: return 0.5
    return float(np.dot(v1, v2) / (n1 * n2))
async def get_recommended_matches(user_id: str, page: int, limit: int) -> dict:
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        user_row = await conn.fetchrow('SELECT p.*,EXTRACT(YEAR FROM AGE(p.dob))::int AS age,ec.education_level,ec.annual_income,pd.height_cm,ld.diet,hd.is_manglik,pp.age_min,pp.age_max,pp.religion AS pref_religion FROM profiles p LEFT JOIN education_career ec ON p.profile_id=ec.profile_id LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id LEFT JOIN partner_preferences pp ON p.profile_id=pp.profile_id WHERE p.user_id=$1 LIMIT 1', user_id)
        if not user_row: return {"matches":[],"total":0,"page":page}
        user = dict(user_row)
        gender = (user.get("gender") or "").lower()
        opp_gender = "female" if gender == "male" else "male" if gender == "female" else None
        page = max(int(page or 1), 1)
        limit = min(max(int(limit or 25), 15), 100)
        candidates = await conn.fetch(
            """
            SELECT p.*,EXTRACT(YEAR FROM AGE(p.dob))::int AS age,ec.education_level,ec.annual_income,
                   ec.occupation,ec.working_city,pd.height_cm,ld.diet,hd.is_manglik,p.primary_photo_url,
                   p.photo_privacy,COALESCE(p.verification_status,'pending')='verified' AS is_verified
            FROM profiles p
            LEFT JOIN education_career ec ON p.profile_id=ec.profile_id
            LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id
            LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id
            LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id
            WHERE ($2::text IS NULL OR p.gender=$2)
              AND p.is_published=true
              AND COALESCE(p.admin_status,'active')='active'
              AND COALESCE(p.profile_status,'active')='active'
              AND COALESCE(p.profile_visibility,'all')!='hidden'
              AND p.user_id!=$1
              AND NOT EXISTS (
                SELECT 1 FROM blocks b
                WHERE (b.blocker_id=$1 AND b.blocked_id=p.user_id)
                   OR (b.blocker_id=p.user_id AND b.blocked_id=$1)
              )
            ORDER BY p.completion_score DESC, p.updated_at DESC
            LIMIT 1000
            """,
            user_id,
            opp_gender
        )
        user_vec = build_vector(user); scored = []
        for c in candidates:
            cd = dict(c); cv = build_vector(cd)
            vec_s = cosine_sim(user_vec, cv) * 100
            pre_s = pref_score(cd, user); hor_s = horoscope_score(user, cd)
            total = int(vec_s*0.35 + pre_s*0.40 + hor_s*0.25)
            scored.append({"profileId":str(cd["profile_id"]),"userId":str(cd["user_id"]),"name":cd["first_name"]+" "+cd["last_name"][:1]+".","age":cd.get("age",0),"heightCm":cd.get("height_cm"),"location":cd.get("working_city",""),"occupation":cd.get("occupation",""),"primaryPhoto":cd.get("primary_photo_url",""),"isVerified":bool(cd.get("is_verified")),"isPhotoPrivate":cd.get("photo_privacy")=="matches_only","profileCreatedBy":cd.get("profile_created_by") or "self","compatibilityScore":min(99,total),"compatibilityBreakdown":{"preferences":int(pre_s),"personality":int(vec_s),"horoscope":int(hor_s)}})
        scored.sort(key=lambda x: x["compatibilityScore"], reverse=True)
        start = (page-1)*limit
        return {"matches":scored[start:start+limit],"total":len(scored),"page":page,"limit":limit}
async def get_compatibility(user_id: str, target_profile_id: str) -> dict:
    pool = await get_db_pool()
    async with pool.acquire() as conn:
        user_row = await conn.fetchrow('SELECT p.*,EXTRACT(YEAR FROM AGE(p.dob))::int AS age,ec.education_level,ec.annual_income,pd.height_cm,ld.diet,hd.is_manglik FROM profiles p LEFT JOIN education_career ec ON p.profile_id=ec.profile_id LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id WHERE p.user_id=$1', user_id)
        target_row = await conn.fetchrow(
            """
            SELECT p.*,EXTRACT(YEAR FROM AGE(p.dob))::int AS age,ec.education_level,ec.annual_income,pd.height_cm,ld.diet,hd.is_manglik
            FROM profiles p
            LEFT JOIN education_career ec ON p.profile_id=ec.profile_id
            LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id
            LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id
            LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id
            WHERE p.profile_id=$1
              AND p.is_published=true
              AND COALESCE(p.admin_status,'active')='active'
              AND COALESCE(p.profile_status,'active')='active'
              AND COALESCE(p.profile_visibility,'all')!='hidden'
              AND NOT EXISTS (
                SELECT 1 FROM blocks b
                WHERE (b.blocker_id=$2 AND b.blocked_id=p.user_id)
                   OR (b.blocker_id=p.user_id AND b.blocked_id=$2)
              )
            """,
            target_profile_id,
            user_id
        )
        if not user_row or not target_row: return {"overallScore":0}
        u = dict(user_row); t = dict(target_row)
        vec_s = cosine_sim(build_vector(u), build_vector(t)) * 100
        pre_s = pref_score(t, u); hor_s = horoscope_score(u, t)
        total = int(vec_s*0.35 + pre_s*0.40 + hor_s*0.25)
        return {"overallScore":min(99,total),"breakdown":{"preferences":int(pre_s),"personality":int(vec_s),"horoscope":int(hor_s)}}
