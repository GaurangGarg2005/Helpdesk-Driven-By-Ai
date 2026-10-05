"""Ticket classification route."""

from fastapi import APIRouter, Depends
from app.core.security import verify_internal_key
from app.models.schemas import ClassificationRequest, ClassificationResponse
from app.services.classifier import classify_ticket

router = APIRouter()


@router.post("/classify", response_model=ClassificationResponse)
async def classify(request: ClassificationRequest, _=Depends(verify_internal_key)):
    return classify_ticket(request.subject, request.description)
