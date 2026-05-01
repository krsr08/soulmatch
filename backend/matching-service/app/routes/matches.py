from fastapi import APIRouter, Depends, Query
from app.services.matching_engine import get_recommended_matches, get_compatibility
from app.middleware.auth import get_current_user
router = APIRouter()
@router.get("/recommended")
async def recommended_matches(page: int = Query(default=1, ge=1), limit: int = Query(default=20, ge=1, le=50), current_user: dict = Depends(get_current_user)):
    result = await get_recommended_matches(current_user["userId"], page, limit)
    return {"success": True, "data": result}
@router.get("/compatibility/{profile_id}")
async def compatibility_score(profile_id: str, current_user: dict = Depends(get_current_user)):
    score = await get_compatibility(current_user["userId"], profile_id)
    return {"success": True, "data": score}
