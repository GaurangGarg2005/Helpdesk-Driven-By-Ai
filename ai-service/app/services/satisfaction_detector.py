"""
Satisfaction Detector Service.

Detects whether a customer message signals that their issue is resolved
or that they are satisfied with the AI's response.
Uses keyword matching with confidence scoring.
"""

import logging
import re

logger = logging.getLogger(__name__)

# High-confidence satisfaction signals
_HIGH_CONF_SIGNALS: list[str] = [
    "that worked", "it worked", "problem solved", "issue resolved",
    "got it working", "fixed now", "all good now", "works now",
    "that fixed it", "thank you so much", "thanks so much", "perfect thank",
    "great that helped", "exactly what i needed",
]

# Medium-confidence satisfaction signals
_MED_CONF_SIGNALS: list[str] = [
    "thank you", "thanks", "solved", "fixed", "resolved", "working",
    "great", "perfect", "got it", "all good", "no more issues", "appreciate",
    "helpful", "helped", "understood", "makes sense", "figured it out",
    "that's all", "thats all", "no more questions", "that will do",
]

# Negative signals that cancel satisfaction (customer is still frustrated)
_NEGATIVE_OVERRIDES: list[str] = [
    "not working", "still not", "doesn't work", "doesnt work",
    "still having", "still broken", "no it didn't", "nope", "not fixed",
    "not helpful", "didn't help", "didnt help", "wrong", "incorrect",
    "still the same", "same problem", "same issue",
]


def check_satisfaction(message: str) -> dict:
    """
    Determine if a message indicates customer satisfaction.

    Returns:
        {satisfied: bool, confidence: float}
    """
    if not message or len(message.strip()) < 3:
        return {"satisfied": False, "confidence": 0.0}

    text = message.lower().strip()
    # Remove punctuation for cleaner matching
    clean = re.sub(r"[^\w\s]", " ", text)

    # Check for negative overrides first
    for neg in _NEGATIVE_OVERRIDES:
        if neg in text:
            logger.debug("Satisfaction: negative override matched '%s'", neg)
            return {"satisfied": False, "confidence": 0.85}

    # Check high-confidence signals
    for signal in _HIGH_CONF_SIGNALS:
        if signal in text:
            logger.debug("Satisfaction: high-confidence match '%s'", signal)
            return {"satisfied": True, "confidence": 0.92}

    # Check medium-confidence signals
    matched_med = [s for s in _MED_CONF_SIGNALS if s in clean]
    if len(matched_med) >= 2:
        return {"satisfied": True, "confidence": 0.78}
    elif len(matched_med) == 1:
        # Single weak signal — only trigger if message is short (not a long complaint)
        if len(message.split()) <= 15:
            return {"satisfied": True, "confidence": 0.62}

    return {"satisfied": False, "confidence": 0.0}
