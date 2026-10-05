"""
RAG Ingest / Search / Delete API routes.
"""

import logging
from fastapi import APIRouter, HTTPException, Request

from app.models.schemas import RagIngestRequest, RagSearchRequest, RagDeleteRequest, RagSearchResponse
from app.services import rag_store

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/ingest", summary="Index a company document into the org's RAG store")
def ingest_document(request: RagIngestRequest):
    try:
        chunks = rag_store.index_document(
            org_id=request.org_id,
            doc_id=request.doc_id,
            title=request.title,
            content=request.content,
        )
        return {"success": True, "chunks_indexed": chunks}
    except Exception as e:
        logger.error("RAG ingest error: %s", e)
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/search", response_model=RagSearchResponse, summary="Search org's RAG store")
def search_rag(request: RagSearchRequest):
    try:
        chunks = rag_store.search(
            org_id=request.org_id,
            query=request.query,
            top_k=request.top_k,
        )
        return RagSearchResponse(chunks=chunks)
    except Exception as e:
        logger.error("RAG search error: %s", e)
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/delete", summary="Remove a document from the org's RAG store")
def delete_document(request: RagDeleteRequest):
    try:
        success = rag_store.delete_document(
            org_id=request.org_id,
            doc_id=request.doc_id,
        )
        return {"success": success}
    except Exception as e:
        logger.error("RAG delete error: %s", e)
        raise HTTPException(status_code=500, detail=str(e))
