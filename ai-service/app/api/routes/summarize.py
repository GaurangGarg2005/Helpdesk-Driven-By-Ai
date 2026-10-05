"""Summarization route."""

from fastapi import APIRouter, Depends
from app.core.security import verify_internal_key
from app.models.schemas import SummarizeRequest, SummarizeResponse
from app.services.summarizer import summarize_thread

router = APIRouter()


@router.post("/summarize", response_model=SummarizeResponse)
async def summarize(request: SummarizeRequest, _=Depends(verify_internal_key)):
    return summarize_thread(request)
