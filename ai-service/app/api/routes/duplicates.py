"""Duplicate detection route."""

from fastapi import APIRouter, Depends
from app.core.security import verify_internal_key
from app.models.schemas import DuplicateRequest, DuplicateResponse
from app.services.duplicate_detector import detect_duplicates

router = APIRouter()


@router.post("/detect-duplicates", response_model=DuplicateResponse)
async def duplicates(request: DuplicateRequest, _=Depends(verify_internal_key)):
    return detect_duplicates(request.subject, request.description, request.existing_tickets)
