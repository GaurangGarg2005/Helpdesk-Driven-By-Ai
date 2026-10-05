"""
Ticket Classification Service (MVP — keyword-based).

Classifies support tickets into categories based on keyword matching
in the subject and description. Will be replaced with ML model later.
"""

import logging
import re

from app.models.schemas import ClassificationRequest, ClassificationResponse

logger = logging.getLogger(__name__)

# ─── Category keyword lists ─────────────────────────────────────────────────
CATEGORY_KEYWORDS: dict[str, list[str]] = {
    "billing": [
        "invoice", "payment", "charge", "charged", "refund", "subscription",
        "pricing", "plan", "upgrade", "downgrade", "billing", "bill", "receipt",
        "credit card", "overcharged", "discount", "coupon", "promo", "renewal",
        "cancel subscription", "trial", "free trial", "cost", "fee", "price",
        "transaction", "autopay", "auto-pay", "paypal", "stripe",
    ],
    "technical": [
        "error", "bug", "crash", "not working", "broken", "issue", "problem",
        "slow", "loading", "timeout", "500", "404", "api", "integration",
        "install", "setup", "configure", "configuration", "ssl", "certificate",
        "server", "database", "connection", "dns", "deploy", "deployment",
        "performance", "latency", "downtime", "outage", "log", "debug",
        "stack trace", "exception", "import", "export", "sync", "webhook",
    ],
    "account": [
        "login", "log in", "sign in", "password", "reset password",
        "forgot password", "account", "profile", "username", "email change",
        "two-factor", "2fa", "mfa", "authentication", "access", "permission",
        "role", "locked out", "suspended", "deactivated", "delete account",
        "sso", "single sign-on", "oauth", "session", "token", "verify",
    ],
    "feature_request": [
        "feature", "request", "suggestion", "would be nice", "please add",
        "can you add", "wish", "enhancement", "improve", "improvement",
        "roadmap", "upcoming", "planned", "new feature", "idea", "proposal",
        "ability to", "option to", "support for", "integration with",
        "would love", "it would be great", "consider adding",
    ],
}

# Precompile patterns for performance
_COMPILED_PATTERNS: dict[str, list[re.Pattern]] = {
    category: [re.compile(rf"\b{re.escape(kw)}\b", re.IGNORECASE) for kw in keywords]
    for category, keywords in CATEGORY_KEYWORDS.items()
}


def _score_category(text: str, category: str) -> float:
    """Count keyword matches for a category, normalised to 0-1."""
    patterns = _COMPILED_PATTERNS[category]
    if not patterns:
        return 0.0
    matches = sum(1 for p in patterns if p.search(text))
    # Normalise: cap at 1.0 and give diminishing returns after 3 matches
    raw = matches / len(patterns)
    # Boost: more matches = higher confidence, but cap at 1.0
    boosted = min(1.0, raw * 3.0 + (0.1 * min(matches, 5)))
    return round(boosted, 4)


def classify_ticket(request: ClassificationRequest) -> ClassificationResponse:
    """
    Classify a ticket into a category based on keyword matching.

    Combines subject (weighted 1.5x) and description to score each category.
    Returns the highest-scoring category with confidence and alternatives.
    """
    combined_text = f"{request.subject} {request.subject} {request.description}"
    # Subject is included twice for 1.5x weight effect

    logger.info("Classifying ticket: subject=%r", request.subject[:80])

    scores: dict[str, float] = {}
    for category in CATEGORY_KEYWORDS:
        scores[category] = _score_category(combined_text, category)

    # Sort categories by score descending
    sorted_categories = sorted(scores.items(), key=lambda x: x[1], reverse=True)

    best_category, best_score = sorted_categories[0]

    # If no keywords matched at all, default to 'general'
    if best_score == 0.0:
        logger.info("No keyword matches found — defaulting to 'general'")
        return ClassificationResponse(
            category="general",
            confidence=0.5,
            alternatives=[
                {"category": cat, "confidence": round(sc, 4)}
                for cat, sc in sorted_categories
            ],
        )

    # Normalise confidence relative to total score mass
    total_score = sum(s for _, s in sorted_categories) or 1.0
    confidence = round(best_score / total_score, 4)

    alternatives = [
        {"category": cat, "confidence": round(sc / total_score, 4)}
        for cat, sc in sorted_categories[1:]
        if sc > 0
    ]

    logger.info(
        "Classification result: category=%s confidence=%.3f",
        best_category, confidence,
    )

    return ClassificationResponse(
        category=best_category,
        confidence=confidence,
        alternatives=alternatives,
    )
