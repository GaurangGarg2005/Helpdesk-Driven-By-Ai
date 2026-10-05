"""
Pydantic request/response schemas for all AI service endpoints.
"""

from pydantic import BaseModel, Field


# ─── Classification ──────────────────────────────────────────────────────────

class ClassificationRequest(BaseModel):
    subject: str = Field(..., description="Ticket subject line")
    description: str = Field(..., description="Ticket body / description")


class ClassificationResponse(BaseModel):
    category: str = Field(
        ..., description="Predicted category: billing, technical, account, feature_request, general"
    )
    confidence: float = Field(..., ge=0.0, le=1.0, description="Confidence score 0-1")
    alternatives: list[dict] = Field(
        default_factory=list,
        description="Alternative categories with confidence: [{category, confidence}]",
    )


# ─── Priority ────────────────────────────────────────────────────────────────

class PriorityRequest(BaseModel):
    subject: str = Field(..., description="Ticket subject line")
    description: str = Field(..., description="Ticket body / description")
    category: str | None = Field(None, description="Pre-classified category, if available")


class PriorityResponse(BaseModel):
    priority: str = Field(
        ..., description="Predicted priority: LOW, MEDIUM, HIGH, URGENT"
    )
    confidence: float = Field(..., ge=0.0, le=1.0)
    reasoning: str = Field(..., description="Explanation for the predicted priority")


# ─── Sentiment ───────────────────────────────────────────────────────────────

class SentimentRequest(BaseModel):
    text: str = Field(..., description="Text to analyze for sentiment")


class SentimentResponse(BaseModel):
    sentiment: str = Field(
        ..., description="Sentiment label: POSITIVE, NEUTRAL, NEGATIVE, VERY_NEGATIVE"
    )
    score: float = Field(
        ..., ge=-1.0, le=1.0, description="Sentiment score from -1.0 (very negative) to 1.0 (positive)"
    )
    emotions: dict = Field(
        default_factory=dict,
        description="Detected emotion intensities: {frustration, urgency, satisfaction, confusion}",
    )


# ─── Reply Suggestion ───────────────────────────────────────────────────────

class ReplyRequest(BaseModel):
    ticket_subject: str = Field(..., description="Original ticket subject")
    ticket_description: str = Field(..., description="Original ticket description")
    conversation_history: list[dict] = Field(
        default_factory=list,
        description="Conversation history: [{sender, message}]",
    )
    kb_context: str | None = Field(
        None, description="Relevant knowledge base context for grounding the reply"
    )


class ReplyResponse(BaseModel):
    suggested_reply: str = Field(..., description="AI-generated reply suggestion")
    confidence: float = Field(..., ge=0.0, le=1.0)
    tone: str = Field(
        ..., description="Detected tone of the reply: professional, empathetic, technical"
    )


# ─── Summarization ──────────────────────────────────────────────────────────

class SummarizeRequest(BaseModel):
    messages: list[dict] = Field(
        ..., description="Messages to summarize: [{sender, message, timestamp}]"
    )


class SummarizeResponse(BaseModel):
    summary: str = Field(..., description="Concise thread summary")
    key_points: list[str] = Field(default_factory=list, description="Extracted key points")
    action_items: list[str] = Field(default_factory=list, description="Identified action items")


# ─── Duplicate Detection ────────────────────────────────────────────────────

class DuplicateRequest(BaseModel):
    subject: str = Field(..., description="New ticket subject")
    description: str = Field(..., description="New ticket description")
    existing_tickets: list[dict] = Field(
        ..., description="Existing tickets to compare: [{id, subject, description}]"
    )


class DuplicateResponse(BaseModel):
    duplicates: list[dict] = Field(
        default_factory=list,
        description="Potential duplicates: [{ticket_id, similarity_score, reason}]",
    )


# ─── KB Recommendations ─────────────────────────────────────────────────────

class KBRecommendRequest(BaseModel):
    query: str = Field(..., description="User query or ticket text")
    articles: list[dict] = Field(
        ..., description="Available KB articles: [{id, title, content}]"
    )


class KBRecommendResponse(BaseModel):
    recommendations: list[dict] = Field(
        default_factory=list,
        description="Recommended articles: [{article_id, relevance_score, reason}]",
    )


# ─── RAG ──────────────────────────────────────────────────────────────────────

class RagIngestRequest(BaseModel):
    org_id: str = Field(..., description="Organization UUID")
    doc_id: str = Field(..., description="Document UUID (used for updates/deletes)")
    title: str = Field(..., description="Document title")
    content: str = Field(..., description="Full document text content")


class RagSearchRequest(BaseModel):
    org_id: str = Field(..., description="Organization UUID")
    query: str = Field(..., description="Query text to search for")
    top_k: int = Field(default=3, ge=1, le=10, description="Number of chunks to return")


class RagSearchResponse(BaseModel):
    chunks: list[str] = Field(default_factory=list, description="Relevant text chunks")


class RagDeleteRequest(BaseModel):
    org_id: str = Field(..., description="Organization UUID")
    doc_id: str = Field(..., description="Document UUID to remove from the index")


# ─── Satisfaction Detection ──────────────────────────────────────────────

class SatisfactionCheckRequest(BaseModel):
    message: str = Field(..., description="Customer message to analyze")


class SatisfactionCheckResponse(BaseModel):
    satisfied: bool = Field(..., description="True if customer appears satisfied")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Confidence 0-1")
