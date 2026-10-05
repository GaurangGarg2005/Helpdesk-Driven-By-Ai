"""KB recommendations route."""

from fastapi import APIRouter, Depends
from app.core.security import verify_internal_key
from app.models.schemas import KBRecommendRequest, KBRecommendResponse
from app.services.kb_recommender import recommend_articles

router = APIRouter()


@router.post("/recommend-articles", response_model=KBRecommendResponse)
async def kb_recommend(request: KBRecommendRequest, _=Depends(verify_internal_key)):
    return recommend_articles(request.query, request.articles)
