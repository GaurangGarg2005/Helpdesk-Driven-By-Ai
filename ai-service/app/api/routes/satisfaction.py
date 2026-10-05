"""
Satisfaction Detection API route.
"""

import logging
from fastapi import APIRouter

from app.models.schemas import SatisfactionCheckRequest, SatisfactionCheckResponse
from app.services.satisfaction_detector import check_satisfaction

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/check-satisfaction", response_model=SatisfactionCheckResponse,
             summary="Detect if a customer message signals satisfaction / issue resolved")
def check_customer_satisfaction(request: SatisfactionCheckRequest):
    result = check_satisfaction(request.message)
    return SatisfactionCheckResponse(**result)
