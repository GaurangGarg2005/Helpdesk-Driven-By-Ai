"""
Sentiment Analysis Service (keyword-based MVP).

Analyses text sentiment using curated keyword lists and emotion detection.
Returns sentiment label, score (-1.0 to 1.0), and emotion intensities.
"""

import logging
import re

from app.models.schemas import SentimentRequest, SentimentResponse

logger = logging.getLogger(__name__)

# ─── Keyword lists with weights ──────────────────────────────────────────────

POSITIVE_KEYWORDS: dict[str, float] = {
    "thank": 0.3, "thanks": 0.3, "thank you": 0.4, "great": 0.4,
    "excellent": 0.5, "awesome": 0.5, "amazing": 0.5, "love": 0.4,
    "wonderful": 0.5, "fantastic": 0.5, "perfect": 0.5, "good": 0.2,
    "happy": 0.4, "pleased": 0.3, "appreciate": 0.4, "helpful": 0.3,
    "solved": 0.3, "resolved": 0.3, "works": 0.2, "working": 0.2,
    "impressive": 0.4, "satisfied": 0.3, "well done": 0.4,
}

NEGATIVE_KEYWORDS: dict[str, float] = {
    "bad": 0.3, "poor": 0.3, "disappointed": 0.4, "frustrating": 0.5,
    "annoying": 0.4, "slow": 0.2, "broken": 0.4, "not working": 0.5,
    "fail": 0.4, "failed": 0.4, "failure": 0.4, "issue": 0.2,
    "problem": 0.2, "bug": 0.3, "error": 0.2, "wrong": 0.3,
    "confusing": 0.3, "unclear": 0.2, "difficult": 0.2, "hate": 0.5,
    "unhappy": 0.4, "upset": 0.4, "angry": 0.5, "complaint": 0.3,
}

VERY_NEGATIVE_KEYWORDS: dict[str, float] = {
    "terrible": 0.7, "horrible": 0.7, "worst": 0.8, "unacceptable": 0.7,
    "outraged": 0.8, "furious": 0.8, "disgusted": 0.7, "scam": 0.8,
    "fraud": 0.8, "lawsuit": 0.9, "sue": 0.8, "incompetent": 0.7,
    "useless": 0.7, "garbage": 0.8, "trash": 0.7, "pathetic": 0.7,
    "absurd": 0.6, "ridiculous": 0.6, "rip off": 0.7, "ripoff": 0.7,
}

# ─── Emotion keyword lists ──────────────────────────────────────────────────

EMOTION_KEYWORDS: dict[str, list[str]] = {
    "frustration": [
        "frustrated", "frustrating", "annoyed", "annoying", "sick of",
        "tired of", "fed up", "enough", "again", "still", "keep having",
        "multiple times", "how many times", "unbelievable",
    ],
    "urgency": [
        "urgent", "asap", "immediately", "right now", "hurry",
        "time-sensitive", "deadline", "critical", "emergency", "quickly",
        "as soon as possible", "now",
    ],
    "satisfaction": [
        "satisfied", "happy", "pleased", "glad", "thankful",
        "grateful", "appreciate", "love it", "well done", "perfect",
        "solved", "resolved", "works great",
    ],
    "confusion": [
        "confused", "confusing", "unclear", "don't understand",
        "what does", "how do", "where is", "lost", "no idea",
        "makes no sense", "not sure", "help me understand",
    ],
}

# Precompile emotion patterns
_EMOTION_PATTERNS: dict[str, list[re.Pattern]] = {
    emotion: [re.compile(rf"\b{re.escape(kw)}\b", re.IGNORECASE) for kw in keywords]
    for emotion, keywords in EMOTION_KEYWORDS.items()
}


def _compute_sentiment_score(text: str) -> float:
    """
    Compute a raw sentiment score between -1.0 and 1.0.
    Positive keywords contribute positively, negative/very-negative subtract.
    """
    text_lower = text.lower()
    pos_score = sum(
        weight for kw, weight in POSITIVE_KEYWORDS.items()
        if re.search(rf"\b{re.escape(kw)}\b", text_lower)
    )
    neg_score = sum(
        weight for kw, weight in NEGATIVE_KEYWORDS.items()
        if re.search(rf"\b{re.escape(kw)}\b", text_lower)
    )
    very_neg_score = sum(
        weight for kw, weight in VERY_NEGATIVE_KEYWORDS.items()
        if re.search(rf"\b{re.escape(kw)}\b", text_lower)
    )

    total_negative = neg_score + very_neg_score * 1.5
    raw = pos_score - total_negative

    # Clamp to [-1.0, 1.0]
    return round(max(-1.0, min(1.0, raw)), 4)


def _detect_emotions(text: str) -> dict[str, float]:
    """Detect emotion intensities from 0.0 to 1.0."""
    emotions: dict[str, float] = {}
    for emotion, patterns in _EMOTION_PATTERNS.items():
        matches = sum(1 for p in patterns if p.search(text))
        if matches > 0:
            intensity = round(min(1.0, matches / len(patterns) * 3.0), 4)
            emotions[emotion] = intensity
        else:
            emotions[emotion] = 0.0
    return emotions


def analyze_sentiment(request: SentimentRequest) -> SentimentResponse:
    """
    Analyse sentiment of the given text.

    Returns:
      - sentiment: POSITIVE / NEUTRAL / NEGATIVE / VERY_NEGATIVE
      - score: float from -1.0 to 1.0
      - emotions: dict of emotion intensities
    """
    text = request if isinstance(request, str) else request.text
    score = _compute_sentiment_score(text)
    emotions = _detect_emotions(text)

    # Determine label
    if score >= 0.3:
        sentiment = "POSITIVE"
    elif score >= -0.1:
        sentiment = "NEUTRAL"
    elif score >= -0.5:
        sentiment = "NEGATIVE"
    else:
        sentiment = "VERY_NEGATIVE"

    # Override to VERY_NEGATIVE if strong very-negative keywords present
    text_lower = text.lower()
    very_neg_hits = sum(
        1 for kw in VERY_NEGATIVE_KEYWORDS
        if re.search(rf"\b{re.escape(kw)}\b", text_lower)
    )
    if very_neg_hits >= 2:
        sentiment = "VERY_NEGATIVE"

    logger.info("Sentiment analysis: sentiment=%s score=%.3f", sentiment, score)

    return SentimentResponse(
        sentiment=sentiment,
        score=score,
        emotions=emotions,
    )
