"""Reply generation route."""

from fastapi import APIRouter, Depends
from app.core.security import verify_internal_key
from app.models.schemas import ReplyRequest, ReplyResponse
from app.services.reply_generator import generate_reply

router = APIRouter()


@router.post("/suggest-reply", response_model=ReplyResponse)
async def suggest_reply(request: ReplyRequest, _=Depends(verify_internal_key)):
    return generate_reply(request)
