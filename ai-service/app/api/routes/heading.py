"""
Heading generation route.
Generates a concise 3-6 word label for a company knowledge document card.
"""

import logging
from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.core.security import verify_internal_key

logger = logging.getLogger(__name__)
router = APIRouter()


class HeadingRequest(BaseModel):
    title: str
    content_snippet: str


class HeadingResponse(BaseModel):
    heading: str


# Common heading patterns by first-word signals
_TOPIC_PATTERNS = [
    (["refund", "return", "money back", "reimburse"], "Refund & Return Policy"),
    (["shipping", "delivery", "dispatch", "arrive"], "Shipping & Delivery Info"),
    (["warranty", "guarantee", "defect", "replace"], "Warranty & Replacement"),
    (["pricing", "price", "cost", "plan", "subscription", "tier"], "Pricing & Plans"),
    (["onboarding", "getting started", "setup", "install", "first step"], "Getting Started Guide"),
    (["faq", "frequently asked", "common question"], "FAQ & Common Questions"),
    (["contact", "support", "reach us", "help center"], "Contact & Support Info"),
    (["privacy", "gdpr", "data protection", "personal data"], "Privacy & Data Policy"),
    (["terms", "conditions", "agreement", "legal"], "Terms & Conditions"),
    (["password", "login", "account", "sign in", "auth"], "Account & Login Help"),
    (["billing", "invoice", "payment", "charge"], "Billing & Payments"),
    (["api", "integration", "webhook", "developer"], "API & Integration Docs"),
    (["cancellation", "cancel", "unsubscribe", "downgrade"], "Cancellation Policy"),
    (["feature", "how to", "tutorial", "guide", "steps"], "Feature Guide"),
    (["update", "release", "changelog", "version", "new"], "Product Updates"),
    (["error", "bug", "issue", "troubleshoot", "not working"], "Troubleshooting Guide"),
    (["team", "collaboration", "member", "invite", "role"], "Team Management"),
    (["export", "import", "data", "backup", "migrate"], "Data Management"),
    (["discount", "coupon", "promo", "offer"], "Discounts & Promotions"),
    (["compliance", "audit", "security", "soc2", "iso"], "Security & Compliance"),
]


def _extract_heading(title: str, content_snippet: str) -> str:
    """
    Generates a concise heading from title and content.
    Tries to match a known topic first, then falls back to title-based trimming.
    """
    combined = f"{title} {content_snippet}".lower()

    for keywords, label in _TOPIC_PATTERNS:
        if any(kw in combined for kw in keywords):
            return label

    # Fallback: clean up the title into max 5 words
    words = title.strip().split()
    if len(words) <= 6:
        return title.strip()

    # Take first 5 meaningful words
    stop_words = {"the", "a", "an", "and", "or", "of", "for", "in", "on", "to", "is", "are"}
    meaningful = [w for w in words if w.lower() not in stop_words]
    if meaningful:
        return " ".join(meaningful[:5])
    return " ".join(words[:5])


@router.post("/generate-heading", response_model=HeadingResponse)
async def generate_heading(
    request: HeadingRequest, _=Depends(verify_internal_key)
):
    """
    Generate a concise, human-friendly heading for a company document card.
    Used by the admin UI to label knowledge base document cards.
    """
    heading = _extract_heading(request.title, request.content_snippet)
    logger.info("Generated heading '%s' for doc titled '%s'", heading, request.title)
    return HeadingResponse(heading=heading)
