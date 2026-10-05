"""
Knowledge Base Article Recommender Service (TF-IDF similarity).

Recommends the most relevant KB articles for a given query using
TF-IDF vectorization and cosine similarity scoring.
"""

import logging

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

from app.models.schemas import KBRecommendRequest, KBRecommendResponse

logger = logging.getLogger(__name__)

TOP_K: int = 3
MIN_RELEVANCE: float = 0.1


def _generate_match_reason(score: float, title: str) -> str:
    """Generate a human-readable match reason."""
    if score >= 0.7:
        return f"Highly relevant — \"{title[:60]}\" closely matches the query"
    elif score >= 0.4:
        return f"Good match — \"{title[:60]}\" covers related topics"
    elif score >= 0.2:
        return f"Partial match — \"{title[:60]}\" may contain useful information"
    else:
        return f"Weak match — \"{title[:60]}\" has some keyword overlap"


def recommend_articles(request: KBRecommendRequest) -> KBRecommendResponse:
    """
    Recommend KB articles relevant to the query using TF-IDF similarity.

    Process:
      1. Build a corpus from the query and all article texts (title + content)
      2. Fit TF-IDF vectorizer on the corpus
      3. Compute cosine similarity between query and each article
      4. Return top-3 articles above the minimum relevance threshold
    """
    if not request.articles:
        logger.info("No articles provided for recommendation")
        return KBRecommendResponse(recommendations=[])

    # Build article texts: combine title (weighted) + content
    article_texts: list[str] = []
    valid_articles: list[dict] = []
    for article in request.articles:
        title = article.get("title", "")
        content = article.get("content", "")
        if title or content:
            # Title is repeated for emphasis
            combined = f"{title} {title} {content}"
            article_texts.append(combined)
            valid_articles.append(article)

    if not article_texts:
        logger.info("No valid articles with text content")
        return KBRecommendResponse(recommendations=[])

    # Corpus: query first, then articles
    corpus = [request.query] + article_texts

    try:
        vectorizer = TfidfVectorizer(
            stop_words="english",
            max_features=5000,
            ngram_range=(1, 2),
            min_df=1,
        )
        tfidf_matrix = vectorizer.fit_transform(corpus)

        # Similarity of query (row 0) vs all articles (rows 1+)
        similarities = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:]).flatten()

    except Exception as e:
        logger.error("TF-IDF computation failed: %s", e)
        return KBRecommendResponse(recommendations=[])

    # Build scored list
    scored: list[tuple[int, float]] = [
        (i, float(score))
        for i, score in enumerate(similarities)
        if score >= MIN_RELEVANCE
    ]
    scored.sort(key=lambda x: x[1], reverse=True)

    # Take top-K
    recommendations: list[dict] = []
    for i, score in scored[:TOP_K]:
        article = valid_articles[i]
        recommendations.append({
            "article_id": article.get("id", f"unknown-{i}"),
            "relevance_score": round(score, 4),
            "reason": _generate_match_reason(score, article.get("title", "")),
        })

    logger.info(
        "KB recommendation: %d articles recommended from %d candidates",
        len(recommendations), len(valid_articles),
    )

    return KBRecommendResponse(recommendations=recommendations)
