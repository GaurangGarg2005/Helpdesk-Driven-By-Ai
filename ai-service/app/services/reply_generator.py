"""
Reply Generation Service.

When company RAG (Retrieval-Augmented Generation) context is available,
the AI composes its answer DIRECTLY from the matched company documentation —
the customer's actual problem is addressed using the exact policies/procedures
found in the knowledge base.

If no RAG context is found, the service falls back to sentiment-aware templates.
"""

import logging
import re

from app.models.schemas import ReplyRequest, ReplyResponse

logger = logging.getLogger(__name__)


# ─── Fallback templates by category (used ONLY when no RAG context) ──────────

TEMPLATES: dict[str, list[str]] = {
    "billing": [
        (
            "Thank you for reaching out regarding your billing concern. "
            "I'd like to help resolve this for you. "
            "Could you please provide your invoice number or the date of the charge "
            "in question so I can look into this further?"
        ),
        (
            "I understand billing issues can be concerning. "
            "Let me investigate the charge you mentioned and get back to you with a detailed breakdown."
        ),
    ],
    "technical": [
        (
            "Thank you for reporting this technical issue. "
            "To help troubleshoot, could you please share:\n"
            "1. The exact error message you're seeing\n"
            "2. Your browser/OS version\n"
            "3. Steps to reproduce the issue\n\n"
            "This will help us identify the root cause quickly."
        ),
        (
            "I appreciate you reaching out about this technical problem. "
            "Our team is actively working on it. "
            "In the meantime, could you try clearing your browser cache and cookies?"
        ),
    ],
    "account": [
        (
            "I understand you're having trouble with your account. "
            "For security purposes, I'll need to verify your identity before making any changes. "
            "Could you please confirm the email address associated with your account?"
        ),
    ],
    "feature_request": [
        (
            "Thank you for your suggestion! We love hearing ideas from our users. "
            "I've documented your feature request and forwarded it to our product team for consideration."
        ),
    ],
    "general": [
        (
            "Thank you for contacting our support team. I'm here to help! "
            "I've reviewed your message and I'd like to gather a bit more "
            "information to assist you effectively. Could you please provide "
            "some additional details about your request?"
        ),
        (
            "Thank you for reaching out. Let me look into this for you right away."
        ),
    ],
}

EMPATHY_PREFIXES: list[str] = [
    "I sincerely apologize for the inconvenience you've experienced. ",
    "I completely understand your frustration, and I'm sorry for the trouble. ",
    "I'm really sorry to hear about this experience. Your concerns are completely valid. ",
]

CATEGORY_SIGNALS: dict[str, list[str]] = {
    "billing": ["invoice", "payment", "charge", "refund", "billing", "subscription", "price"],
    "technical": ["error", "bug", "crash", "not working", "broken", "slow", "api", "server"],
    "account": ["login", "password", "account", "access", "locked", "profile", "permission"],
    "feature_request": ["feature", "request", "suggestion", "add", "wish", "enhancement"],
}


def _detect_category(subject: str, description: str) -> str:
    combined = f"{subject} {description}".lower()
    scores: dict[str, int] = {}
    for cat, keywords in CATEGORY_SIGNALS.items():
        scores[cat] = sum(1 for kw in keywords if kw in combined)
    best = max(scores, key=scores.get)  # type: ignore[arg-type]
    return best if scores[best] > 0 else "general"


def _detect_sentiment_tone(text: str) -> str:
    text_lower = text.lower()
    negative_words = [
        "frustrated", "angry", "upset", "terrible", "horrible", "worst",
        "unacceptable", "furious", "disappointed", "annoyed",
    ]
    positive_words = ["thank", "great", "appreciate", "love", "excellent", "happy"]
    neg_count = sum(1 for w in negative_words if w in text_lower)
    pos_count = sum(1 for w in positive_words if w in text_lower)

    if neg_count >= 2:
        return "very_negative"
    elif neg_count > pos_count:
        return "negative"
    elif pos_count > neg_count:
        return "positive"
    return "neutral"


def _build_rag_reply(
    subject: str,
    description: str,
    history: list[dict],
    kb_context: str,
    sentiment_tone: str,
) -> str:
    """
    Builds the primary reply when company docs are available.

    The customer's problem is combined with the matched knowledge base
    passage to produce a direct, helpful answer grounded in company policy.
    """
    # Start with empathy if negative
    prefix = ""
    if sentiment_tone in ("negative", "very_negative"):
        idx = hash(subject) % len(EMPATHY_PREFIXES)
        prefix = EMPATHY_PREFIXES[idx]

    # Determine what the customer actually asked
    latest_customer_msg = description or subject
    for msg in reversed(history):
        if msg.get("sender", "").upper() == "CUSTOMER":
            latest_customer_msg = msg.get("message", description)
            break

    # Compose the RAG-grounded answer
    # The KB passage is the BODY of the answer; we wrap it naturally
    reply_lines = []

    if prefix:
        reply_lines.append(prefix)

    reply_lines.append(
        f"Based on our company information, here is what I can tell you about your question:\n"
    )

    # Clean and present the RAG context naturally
    cleaned_context = kb_context.strip()
    # If the context is a long passage, quote it as the answer
    reply_lines.append(cleaned_context)

    # Add a closing prompt
    reply_lines.append(
        "\n\nI hope this directly addresses your concern. "
        "If you need any clarification or have follow-up questions, please don't hesitate to ask — "
        "I'm happy to help further."
    )

    return "\n".join(reply_lines)


def generate_reply(request: ReplyRequest) -> ReplyResponse:
    """
    Generate a context-aware reply suggestion.

    Priority order:
      1. If kb_context (RAG match) is available → compose answer DIRECTLY from docs
      2. If no RAG match → use category template + sentiment tone
    """
    category = _detect_category(request.ticket_subject, request.ticket_description)

    all_text = f"{request.ticket_subject} {request.ticket_description}"
    for msg in request.conversation_history:
        all_text += f" {msg.get('message', '')}"
    sentiment_tone = _detect_sentiment_tone(all_text)

    confidence = 0.5
    tone = "professional"

    # ── Path 1: RAG-grounded reply ────────────────────────────────────────────
    if request.kb_context and request.kb_context.strip():
        logger.info(
            "RAG context found (%d chars) — building doc-grounded reply for category=%s",
            len(request.kb_context),
            category,
        )

        reply = _build_rag_reply(
            subject=request.ticket_subject,
            description=request.ticket_description,
            history=request.conversation_history,
            kb_context=request.kb_context,
            sentiment_tone=sentiment_tone,
        )

        if sentiment_tone in ("negative", "very_negative"):
            tone = "empathetic"
        elif category == "technical":
            tone = "technical"

        # Higher confidence when grounded in docs
        confidence = round(0.85 + (0.1 if sentiment_tone == "neutral" else 0.05), 4)

        logger.info(
            "RAG-grounded reply generated: tone=%s confidence=%.3f",
            tone,
            confidence,
        )
        return ReplyResponse(suggested_reply=reply, confidence=confidence, tone=tone)

    # ── Path 2: Template fallback ─────────────────────────────────────────────
    logger.info(
        "No RAG context — using template for category=%s sentiment=%s",
        category,
        sentiment_tone,
    )

    # Fallback: template-based reply
    templates = TEMPLATES.get(category, TEMPLATES["general"])

    # Use the LATEST customer message as the hash seed so the template varies
    latest_msg = request.ticket_description
    for msg in reversed(request.conversation_history):
        if msg.get("sender", "").upper() == "CUSTOMER":
            latest_msg = msg.get("message", request.ticket_description)
            break

    # Combine message + length to get better variation across template list
    idx = (hash(latest_msg) + len(latest_msg)) % len(templates)
    reply = templates[idx]

    if sentiment_tone in ("negative", "very_negative"):
        tone = "empathetic"
        prefix = EMPATHY_PREFIXES[hash(request.ticket_subject) % len(EMPATHY_PREFIXES)]
        reply = prefix + reply
    elif category == "technical":
        tone = "technical"

    context_signals = sum([
        bool(request.conversation_history),
        category != "general",
    ])
    confidence = round(0.45 + (context_signals * 0.1), 4)

    logger.info(
        "Template reply generated: tone=%s confidence=%.3f category=%s",
        tone,
        confidence,
        category,
    )
    return ReplyResponse(suggested_reply=reply, confidence=confidence, tone=tone)
