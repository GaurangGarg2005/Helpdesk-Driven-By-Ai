"""
Priority Prediction Service (heuristic-based MVP).

Predicts ticket priority using urgency keywords, frustration indicators,
and category signals. Will be replaced with ML model later.
"""

import logging
import re

from app.models.schemas import PriorityRequest, PriorityResponse

logger = logging.getLogger(__name__)

# ─── Keyword signal lists ────────────────────────────────────────────────────

URGENCY_KEYWORDS: list[str] = [
    "urgent", "asap", "critical", "down", "broken", "emergency",
    "immediately", "right now", "production down", "outage",
    "cannot access", "blocked", "showstopper", "deadline",
    "time-sensitive", "losing money", "data loss", "security breach",
]

FRUSTRATION_INDICATORS: list[str] = [
    "been waiting", "multiple times", "very frustrated", "unacceptable",
    "worst experience", "still not resolved", "no response",
    "terrible", "ridiculous", "incompetent", "fed up", "last straw",
    "going to cancel", "switching to", "never again", "hours ago",
    "days ago", "weeks ago", "how many times",
]

HIGH_PRIORITY_CATEGORIES: set[str] = {"billing", "technical"}

_URGENCY_PATTERNS = [re.compile(rf"\b{re.escape(kw)}\b", re.IGNORECASE) for kw in URGENCY_KEYWORDS]
_FRUSTRATION_PATTERNS = [re.compile(rf"\b{re.escape(kw)}\b", re.IGNORECASE) for kw in FRUSTRATION_INDICATORS]


def predict_priority(request: PriorityRequest) -> PriorityResponse:
    """
    Predict ticket priority using heuristic signals.

    Scoring factors:
      - Urgency keywords: +2 points each (up to 10)
      - Frustration indicators: +1.5 points each (up to 7.5)
      - High-priority category (billing/technical): +2 points
      - ALL-CAPS words (>3 chars): +0.5 per word (up to 3)
      - Exclamation marks: +0.3 each (up to 1.5)

    Priority thresholds:
      - URGENT:  score >= 8
      - HIGH:    score >= 4
      - MEDIUM:  score >= 1.5
      - LOW:     score <  1.5
    """
    combined = f"{request.subject} {request.description}"
    reasons: list[str] = []
    score: float = 0.0

    # ── Urgency keywords ─────────────────────────────────────────────────
    urgency_matches = [kw for kw, p in zip(URGENCY_KEYWORDS, _URGENCY_PATTERNS) if p.search(combined)]
    if urgency_matches:
        pts = min(len(urgency_matches) * 2.0, 10.0)
        score += pts
        reasons.append(f"Urgency keywords detected: {', '.join(urgency_matches[:5])}")

    # ── Frustration indicators ───────────────────────────────────────────
    frustration_matches = [
        kw for kw, p in zip(FRUSTRATION_INDICATORS, _FRUSTRATION_PATTERNS) if p.search(combined)
    ]
    if frustration_matches:
        pts = min(len(frustration_matches) * 1.5, 7.5)
        score += pts
        reasons.append(f"Frustration signals: {', '.join(frustration_matches[:5])}")

    # ── Category signal ──────────────────────────────────────────────────
    if request.category and request.category.lower() in HIGH_PRIORITY_CATEGORIES:
        score += 2.0
        reasons.append(f"Category '{request.category}' is flagged as higher priority")

    # ── ALL-CAPS words ───────────────────────────────────────────────────
    caps_words = [w for w in combined.split() if w.isupper() and len(w) > 3]
    if caps_words:
        pts = min(len(caps_words) * 0.5, 3.0)
        score += pts
        reasons.append(f"Emphasised words (ALL CAPS): {', '.join(caps_words[:5])}")

    # ── Exclamation marks ────────────────────────────────────────────────
    excl_count = combined.count("!")
    if excl_count > 0:
        pts = min(excl_count * 0.3, 1.5)
        score += pts
        reasons.append(f"Exclamation marks detected ({excl_count})")

    # ── Determine priority level ─────────────────────────────────────────
    if score >= 8.0:
        priority = "URGENT"
    elif score >= 4.0:
        priority = "HIGH"
    elif score >= 1.5:
        priority = "MEDIUM"
    else:
        priority = "LOW"
        if not reasons:
            reasons.append("No urgency or frustration signals detected")

    # Confidence is higher when the score is firmly within a bracket
    max_score = 20.0
    confidence = round(min(score / max_score + 0.3, 1.0), 4)
    if priority == "LOW":
        confidence = round(max(0.6, confidence), 4)

    reasoning = "; ".join(reasons) + f" (heuristic score: {score:.1f})"

    logger.info(
        "Priority prediction: priority=%s confidence=%.3f score=%.1f",
        priority, confidence, score,
    )

    return PriorityResponse(
        priority=priority,
        confidence=confidence,
        reasoning=reasoning,
    )
