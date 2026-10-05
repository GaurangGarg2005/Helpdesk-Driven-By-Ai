"""
Thread Summarization Service.

Extracts key points and action items from a list of conversation messages.
Produces a structured summary useful for agents reviewing ticket history.
"""

import logging
import re

from app.models.schemas import SummarizeRequest, SummarizeResponse

logger = logging.getLogger(__name__)

# ─── Action item indicator patterns ─────────────────────────────────────────

ACTION_PATTERNS: list[re.Pattern] = [
    re.compile(r"\b(?:please|pls)\b.{5,80}", re.IGNORECASE),
    re.compile(r"\b(?:need to|needs to)\b.{5,80}", re.IGNORECASE),
    re.compile(r"\b(?:should|must)\b.{5,80}", re.IGNORECASE),
    re.compile(r"\b(?:will|i'll|we'll|we will)\b.{5,80}", re.IGNORECASE),
    re.compile(r"\b(?:action required|follow up|to-do|todo)\b.{0,80}", re.IGNORECASE),
    re.compile(r"\b(?:can you|could you)\b.{5,80}", re.IGNORECASE),
    re.compile(r"\b(?:make sure|ensure)\b.{5,80}", re.IGNORECASE),
]


def _extract_key_points(messages: list[dict]) -> list[str]:
    """
    Extract key points from messages.

    Heuristics:
      - First message is the initial request (always a key point)
      - Messages with questions (?) are key interaction points
      - Messages mentioning resolution/solution keywords are key
      - Longer messages likely contain important details
    """
    key_points: list[str] = []

    if not messages:
        return key_points

    # The first message is always the initial request
    first_msg = messages[0].get("message", "").strip()
    if first_msg:
        sender = messages[0].get("sender", "User")
        truncated = first_msg[:150] + ("..." if len(first_msg) > 150 else "")
        key_points.append(f"{sender} reported: {truncated}")

    # Scan for key signals in remaining messages
    resolution_keywords = [
        "resolved", "fixed", "solution", "solved", "working now",
        "confirmed", "completed", "done", "closed",
    ]
    question_pattern = re.compile(r"\?")
    escalation_keywords = ["escalat", "manager", "supervisor", "urgent"]

    for i, msg in enumerate(messages[1:], start=1):
        text = msg.get("message", "").strip()
        sender = msg.get("sender", "Unknown")
        if not text:
            continue

        text_lower = text.lower()

        # Questions from the user signal important interactions
        if question_pattern.search(text) and sender.lower() in ("user", "customer", "client"):
            truncated = text[:120] + ("..." if len(text) > 120 else "")
            key_points.append(f"{sender} asked: {truncated}")

        # Resolution indicators
        elif any(kw in text_lower for kw in resolution_keywords):
            truncated = text[:120] + ("..." if len(text) > 120 else "")
            key_points.append(f"{sender} indicated resolution: {truncated}")

        # Escalation
        elif any(kw in text_lower for kw in escalation_keywords):
            truncated = text[:120] + ("..." if len(text) > 120 else "")
            key_points.append(f"Escalation noted from {sender}: {truncated}")

        # Long messages (>200 chars) from agents likely contain important details
        elif len(text) > 200 and sender.lower() in ("agent", "support", "admin"):
            truncated = text[:120] + ("..." if len(text) > 120 else "")
            key_points.append(f"{sender} provided details: {truncated}")

    # Deduplicate while preserving order
    seen: set[str] = set()
    unique_points: list[str] = []
    for point in key_points:
        if point not in seen:
            seen.add(point)
            unique_points.append(point)

    return unique_points[:10]  # Cap at 10 key points


def _extract_action_items(messages: list[dict]) -> list[str]:
    """Extract action items from messages using regex patterns."""
    action_items: list[str] = []

    for msg in messages:
        text = msg.get("message", "").strip()
        sender = msg.get("sender", "Unknown")
        if not text:
            continue

        for pattern in ACTION_PATTERNS:
            matches = pattern.findall(text)
            for match in matches:
                cleaned = match.strip().rstrip(".")
                if len(cleaned) > 10:  # Skip very short matches
                    action_items.append(f"[{sender}] {cleaned}")

    # Deduplicate
    seen: set[str] = set()
    unique_items: list[str] = []
    for item in action_items:
        normalised = item.lower().strip()
        if normalised not in seen:
            seen.add(normalised)
            unique_items.append(item)

    return unique_items[:8]  # Cap at 8 action items


def _build_summary(messages: list[dict], key_points: list[str]) -> str:
    """Build a concise summary paragraph from the conversation."""
    if not messages:
        return "No messages to summarize."

    total = len(messages)
    senders = set(msg.get("sender", "Unknown") for msg in messages)
    first_msg = messages[0].get("message", "")[:100]
    last_msg = messages[-1].get("message", "")[:100]

    # Check for resolution signals in the last few messages
    resolved = False
    resolution_keywords = ["resolved", "fixed", "solved", "closed", "working now", "done"]
    for msg in messages[-3:]:
        text_lower = msg.get("message", "").lower()
        if any(kw in text_lower for kw in resolution_keywords):
            resolved = True
            break

    participants = ", ".join(sorted(senders))
    summary_parts = [
        f"Conversation with {total} messages between {participants}.",
        f"Started with: \"{first_msg}{'...' if len(messages[0].get('message', '')) > 100 else ''}\"",
    ]

    if resolved:
        summary_parts.append("The issue appears to have been resolved.")
    elif total > 5:
        summary_parts.append("The conversation is still ongoing.")

    if key_points:
        summary_parts.append(f"{len(key_points)} key points were identified.")

    return " ".join(summary_parts)


def summarize_thread(request: SummarizeRequest) -> SummarizeResponse:
    """
    Summarize a conversation thread.

    Returns:
      - summary: Concise paragraph overview
      - key_points: Important moments in the conversation
      - action_items: Tasks and follow-ups mentioned
    """
    logger.info("Summarizing thread with %d messages", len(request.messages))

    key_points = _extract_key_points(request.messages)
    action_items = _extract_action_items(request.messages)
    summary = _build_summary(request.messages, key_points)

    logger.info(
        "Summary complete: %d key points, %d action items",
        len(key_points), len(action_items),
    )

    return SummarizeResponse(
        summary=summary,
        key_points=key_points,
        action_items=action_items,
    )
