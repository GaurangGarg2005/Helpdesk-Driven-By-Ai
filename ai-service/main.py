"""HelpDeskAI AI Service — FastAPI application entry point."""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import classification, priority, sentiment, reply, summarize, duplicates, kb_recommendations, rag_ingest, satisfaction, heading

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("🚀 HelpDeskAI AI Service starting up...")
    logger.info("📦 Loading ML models and NLP pipelines...")
    yield
    logger.info("🛑 HelpDeskAI AI Service shutting down...")


app = FastAPI(
    title="HelpDeskAI AI Service",
    description="Internal AI microservice for ticket classification, priority prediction, sentiment analysis, reply generation, summarization, duplicate detection, and KB recommendations.",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(classification.router, prefix="/api/v1/ai", tags=["Classification"])
app.include_router(priority.router, prefix="/api/v1/ai", tags=["Priority"])
app.include_router(sentiment.router, prefix="/api/v1/ai", tags=["Sentiment"])
app.include_router(reply.router, prefix="/api/v1/ai", tags=["Reply Generation"])
app.include_router(summarize.router, prefix="/api/v1/ai", tags=["Summarization"])
app.include_router(duplicates.router, prefix="/api/v1/ai", tags=["Duplicates"])
app.include_router(kb_recommendations.router, prefix="/api/v1/ai", tags=["KB Recommendations"])
app.include_router(rag_ingest.router, prefix="/api/v1/ai/rag", tags=["RAG"])
app.include_router(satisfaction.router, prefix="/api/v1/ai", tags=["Satisfaction"])
app.include_router(heading.router, prefix="/api/v1/ai", tags=["Heading"])


@app.get("/api/v1/ai/health", tags=["Health"])
async def health_check():
    return {"status": "healthy", "service": "ai-service", "version": "1.0.0"}
