from fastapi import APIRouter, Depends
from app.services.interest_service import send_interest, get_received_interests, get_sent_interests, respond_to_interest, toggle_shortlist, get_shortlist
from app.middleware.auth import get_current_user
from pydantic import BaseModel
router = APIRouter()
class InterestRequest(BaseModel):
    receiverId: str
class InterestResponse(BaseModel):
    status: str
@router.post("/send")
async def send(req: InterestRequest, current_user: dict = Depends(get_current_user)):
    result = await send_interest(current_user["userId"], req.receiverId)
    return {"success": True, "data": result}
@router.get("/received")
async def received(current_user: dict = Depends(get_current_user)):
    return {"success": True, "data": await get_received_interests(current_user["userId"])}
@router.get("/sent")
async def sent(current_user: dict = Depends(get_current_user)):
    return {"success": True, "data": await get_sent_interests(current_user["userId"])}
@router.put("/{interest_id}/respond")
async def respond(interest_id: str, req: InterestResponse, current_user: dict = Depends(get_current_user)):
    result = await respond_to_interest(interest_id, req.status, current_user["userId"])
    return {"success": True, "data": result}
@router.post("/shortlist/{profile_id}")
async def shortlist(profile_id: str, current_user: dict = Depends(get_current_user)):
    result = await toggle_shortlist(current_user["userId"], profile_id)
    return {"success": True, "data": result}
@router.get("/shortlist")
async def get_shortlisted(current_user: dict = Depends(get_current_user)):
    return {"success": True, "data": await get_shortlist(current_user["userId"])}
