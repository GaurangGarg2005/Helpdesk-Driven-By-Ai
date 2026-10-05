"""
RAG (Retrieval-Augmented Generation) vector store.

Manages per-organization FAISS indexes so the AI can retrieve
company-specific documentation when generating ticket replies.

Each org has its own isolated index keyed by org_id. Documents are
chunked into ~500 token segments before embedding.
"""

import logging
import os
import pickle
from pathlib import Path
from typing import Optional

import numpy as np

logger = logging.getLogger(__name__)

# Persistence directory for FAISS indexes
_INDEX_DIR = Path(os.getenv("RAG_INDEX_DIR", "./rag_indexes"))
_INDEX_DIR.mkdir(parents=True, exist_ok=True)

# Lazy imports so the service starts even if sentence-transformers is slow to load
_model = None
_faiss = None


def _get_model():
    global _model
    if _model is None:
        from sentence_transformers import SentenceTransformer
        _model = SentenceTransformer("all-MiniLM-L6-v2")
        logger.info("Loaded sentence-transformer model: all-MiniLM-L6-v2")
    return _model


def _get_faiss():
    global _faiss
    if _faiss is None:
        import faiss as _f
        _faiss = _f
    return _faiss


def _chunk_text(text: str, chunk_size: int = 500, overlap: int = 50) -> list[str]:
    """Split text into overlapping word-chunks."""
    words = text.split()
    chunks = []
    i = 0
    while i < len(words):
        chunk = " ".join(words[i : i + chunk_size])
        chunks.append(chunk)
        i += chunk_size - overlap
    return [c for c in chunks if len(c.strip()) > 20]


# In-memory store: org_id -> {index, doc_map, chunk_texts}
_stores: dict[str, dict] = {}


def _store_path(org_id: str) -> Path:
    return _INDEX_DIR / f"{org_id}.pkl"


def _load_store(org_id: str) -> Optional[dict]:
    path = _store_path(org_id)
    if path.exists():
        try:
            with open(path, "rb") as f:
                return pickle.load(f)
        except Exception as e:
            logger.error("Failed to load RAG store for org %s: %s", org_id, e)
    return None


def _save_store(org_id: str, store: dict) -> None:
    try:
        with open(_store_path(org_id), "wb") as f:
            pickle.dump(store, f)
    except Exception as e:
        logger.error("Failed to save RAG store for org %s: %s", org_id, e)


def _get_store(org_id: str) -> dict:
    """Load or create the in-memory store for an org."""
    if org_id not in _stores:
        loaded = _load_store(org_id)
        if loaded:
            _stores[org_id] = loaded
        else:
            faiss = _get_faiss()
            dim = 384  # all-MiniLM-L6-v2 output dim
            _stores[org_id] = {
                "index": faiss.IndexFlatL2(dim),
                "chunk_texts": [],      # list[str]  indexed by faiss row id
                "doc_chunks": {},       # doc_id -> list of faiss row ids
            }
    return _stores[org_id]


def index_document(org_id: str, doc_id: str, title: str, content: str) -> int:
    """
    Chunk, embed, and index a document into the org's FAISS store.
    Returns the number of chunks indexed.
    """
    store = _get_store(org_id)

    # Remove old chunks if doc already exists
    _remove_doc_chunks(store, doc_id)

    full_text = f"{title}\n\n{content}"
    chunks = _chunk_text(full_text)
    if not chunks:
        logger.warning("No chunks produced for doc %s", doc_id)
        return 0

    model = _get_model()
    embeddings = model.encode(chunks, show_progress_bar=False)
    embeddings = np.array(embeddings, dtype="float32")

    start_id = len(store["chunk_texts"])
    store["chunk_texts"].extend(chunks)
    store["doc_chunks"][doc_id] = list(range(start_id, start_id + len(chunks)))
    store["index"].add(embeddings)

    _save_store(org_id, store)
    logger.info("Indexed %d chunks for doc=%s org=%s", len(chunks), doc_id, org_id)
    return len(chunks)


def search(org_id: str, query: str, top_k: int = 3) -> list[str]:
    """
    Retrieve the top-k most relevant text chunks for a query.
    Returns an empty list if the org has no documents indexed.
    """
    store = _get_store(org_id)
    if store["index"].ntotal == 0:
        return []

    model = _get_model()
    query_embedding = model.encode([query], show_progress_bar=False)
    query_embedding = np.array(query_embedding, dtype="float32")

    k = min(top_k, store["index"].ntotal)
    distances, indices = store["index"].search(query_embedding, k)

    results = []
    for idx in indices[0]:
        if 0 <= idx < len(store["chunk_texts"]):
            results.append(store["chunk_texts"][idx])

    return results


def delete_document(org_id: str, doc_id: str) -> bool:
    """
    Remove a document's chunks from the org's index.
    Note: FAISS FlatL2 doesn't support direct deletion, so we rebuild the index.
    """
    if org_id not in _stores:
        stored = _load_store(org_id)
        if not stored:
            return False
        _stores[org_id] = stored

    store = _stores[org_id]
    _remove_doc_chunks(store, doc_id)
    _rebuild_index(org_id, store)
    _save_store(org_id, store)
    logger.info("Deleted doc=%s from org=%s", doc_id, org_id)
    return True


def _remove_doc_chunks(store: dict, doc_id: str) -> None:
    """Mark chunks belonging to a doc for removal."""
    if doc_id in store["doc_chunks"]:
        del store["doc_chunks"][doc_id]


def _rebuild_index(org_id: str, store: dict) -> None:
    """Rebuild FAISS index from the remaining doc chunks."""
    faiss = _get_faiss()
    dim = 384
    new_index = faiss.IndexFlatL2(dim)

    # Collect all surviving chunk ids
    surviving_ids = set()
    for ids in store["doc_chunks"].values():
        surviving_ids.update(ids)

    if not surviving_ids:
        store["index"] = new_index
        store["chunk_texts"] = []
        store["doc_chunks"] = {}
        return

    surviving_texts = [store["chunk_texts"][i] for i in sorted(surviving_ids)
                       if i < len(store["chunk_texts"])]

    if surviving_texts:
        model = _get_model()
        embeddings = model.encode(surviving_texts, show_progress_bar=False)
        embeddings = np.array(embeddings, dtype="float32")
        new_index.add(embeddings)

    # Remap doc_chunks to new sequential ids
    new_chunk_texts = surviving_texts
    new_doc_chunks: dict[str, list[int]] = {}
    old_id_to_new = {old: new for new, old in enumerate(sorted(surviving_ids))}
    for doc_id, old_ids in store["doc_chunks"].items():
        new_doc_chunks[doc_id] = [old_id_to_new[i] for i in old_ids if i in old_id_to_new]

    store["index"] = new_index
    store["chunk_texts"] = new_chunk_texts
    store["doc_chunks"] = new_doc_chunks
