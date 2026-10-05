"""Priority prediction route."""

from fastapi import APIRouter, Depends
from app.core.security import verify_internal_key
from app.models.schemas import PriorityRequest, PriorityResponse
from app.services.priority_predictor import predict_priority

router = APIRouter()


@router.post("/predict-priority", response_model=PriorityResponse)
async def predict(request: PriorityRequest, _=Depends(verify_internal_key)):
    return predict_priority(request.subject, request.description, request.category)
