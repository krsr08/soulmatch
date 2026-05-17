from fastapi import APIRouter, Depends, HTTPException, Query
from app.services.matching_engine import get_recommended_matches, get_compatibility
from app.middleware.auth import get_current_user
import os
import time

router = APIRouter()
_RATE_BUCKETS = {}


def rate_limited_user(current_user: dict = Depends(get_current_user)):
    user_id = current_user.get("userId") or current_user.get("sub")
    if not user_id:
        raise HTTPException(status_code=401, detail="Invalid user token")
    window_seconds = int(os.getenv("MATCH_RATE_WINDOW_SECONDS", "60"))
    max_requests = int(os.getenv("MATCH_RATE_LIMIT", "60"))
    now = time.monotonic()
    bucket = [stamp for stamp in _RATE_BUCKETS.get(user_id, []) if now - stamp < window_seconds]
    if len(bucket) >= max_requests:
        raise HTTPException(status_code=429, detail="Too many match requests. Please wait and try again.")
    bucket.append(now)
    _RATE_BUCKETS[user_id] = bucket
    return current_user


@router.get("/recommended")
async def recommended_matches(
    page: int = Query(default=1, ge=1),
    limit: int = Query(default=50, ge=1, le=50),
    verifiedOnly: bool = Query(default=False),
    current_user: dict = Depends(rate_limited_user)
):
    try:
        result = await get_recommended_matches(current_user["userId"], page, limit, verified_only=verifiedOnly)
    except PermissionError as exc:
        raise HTTPException(status_code=403, detail=str(exc))
    return {"success": True, "data": result}
@router.get("/compatibility/{profile_id}")
async def compatibility_score(profile_id: str, current_user: dict = Depends(rate_limited_user)):
    score = await get_compatibility(current_user["userId"], profile_id)
    return {"success": True, "data": score}
