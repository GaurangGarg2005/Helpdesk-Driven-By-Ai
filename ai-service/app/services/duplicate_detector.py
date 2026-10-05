"""
Duplicate Ticket Detection Service (TF-IDF + cosine similarity).

Identifies potential duplicate tickets by computing text similarity
between a new ticket and a list of existing tickets using scikit-learn's
TfidfVectorizer and cosine similarity.
"""

import logging

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

from app.models.schemas import DuplicateRequest, DuplicateResponse

logger = logging.getLogger(__name__)

SIMILARITY_THRESHOLD: float = 0.5


def _combine_ticket_text(subject: str, description: str) -> str:
    """Combine subject and description into a single searchable string."""
    return f"{subject} {subject} {description}"  # Subject weighted 2x


def _generate_reason(score: float, new_subject: str, existing_subject: str) -> str:
    """Generate a human-readable reason for the duplicate match."""
    if score >= 0.8:
        return f"Very high similarity ({score:.0%}) — likely the same issue as \"{existing_subject[:60]}\""
    elif score >= 0.65:
        return f"High similarity ({score:.0%}) — closely related to \"{existing_subject[:60]}\""
    else:
        return f"Moderate similarity ({score:.0%}) — may be related to \"{existing_subject[:60]}\""


def detect_duplicates(request: DuplicateRequest) -> DuplicateResponse:
    """
    Detect duplicate tickets using TF-IDF cosine similarity.

    Process:
      1. Combine subject + description for the new ticket and all existing tickets
      2. Fit a TF-IDF vectorizer on all texts
      3. Compute cosine similarity between the new ticket and each existing one
      4. Return tickets with similarity above the threshold (0.5)
    """
    if not request.existing_tickets:
        logger.info("No existing tickets to compare against")
        return DuplicateResponse(duplicates=[])

    new_text = _combine_ticket_text(request.subject, request.description)

    existing_texts: list[str] = []
    valid_tickets: list[dict] = []
    for ticket in request.existing_tickets:
        subj = ticket.get("subject", "")
        desc = ticket.get("description", "")
        if subj or desc:
            existing_texts.append(_combine_ticket_text(subj, desc))
            valid_tickets.append(ticket)

    if not existing_texts:
        logger.info("No valid existing tickets with text content")
        return DuplicateResponse(duplicates=[])

    # Build corpus: new ticket first, then existing tickets
    corpus = [new_text] + existing_texts

    try:
        vectorizer = TfidfVectorizer(
            stop_words="english",
            max_features=5000,
            ngram_range=(1, 2),  # Unigrams + bigrams for better matching
            min_df=1,
        )
        tfidf_matrix = vectorizer.fit_transform(corpus)

        # Similarity of new ticket (row 0) vs all existing (rows 1+)
        similarities = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:]).flatten()

    except Exception as e:
        logger.error("TF-IDF computation failed: %s", e)
        return DuplicateResponse(duplicates=[])

    # Collect duplicates above threshold
    duplicates: list[dict] = []
    for i, score in enumerate(similarities):
        if score >= SIMILARITY_THRESHOLD:
            ticket = valid_tickets[i]
            duplicates.append({
                "ticket_id": ticket.get("id", f"unknown-{i}"),
                "similarity_score": round(float(score), 4),
                "reason": _generate_reason(
                    float(score),
                    request.subject,
                    ticket.get("subject", ""),
                ),
            })

    # Sort by similarity descending
    duplicates.sort(key=lambda d: d["similarity_score"], reverse=True)

    logger.info(
        "Duplicate detection: %d/%d tickets above %.0f%% threshold",
        len(duplicates), len(valid_tickets), SIMILARITY_THRESHOLD * 100,
    )

    return DuplicateResponse(duplicates=duplicates)
