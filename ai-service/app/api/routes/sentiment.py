"""Sentiment analysis route."""

from fastapi import APIRouter, Depends
from app.core.security import verify_internal_key
from app.models.schemas import SentimentRequest, SentimentResponse
from app.services.sentiment_analyzer import analyze_sentiment

router = APIRouter()


@router.post("/sentiment", response_model=SentimentResponse)
async def sentiment(request: SentimentRequest, _=Depends(verify_internal_key)):
    return analyze_sentiment(request.text)
