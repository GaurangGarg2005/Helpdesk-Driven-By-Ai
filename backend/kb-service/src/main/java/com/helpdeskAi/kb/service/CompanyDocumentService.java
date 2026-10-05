package com.helpdeskAi.kb.service;

import com.helpdeskAi.common.exception.ResourceNotFoundException;
import com.helpdeskAi.common.security.TenantContext;
import com.helpdeskAi.kb.dto.*;
import com.helpdeskAi.kb.model.CompanyDocument;
import com.helpdeskAi.kb.repository.CompanyDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Manages company knowledge documents.
 * After saving, calls the AI service to index/delete from the RAG vector store.
 * Each document gets an AI-generated heading for easy identification in the UI.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyDocumentService {

    private final CompanyDocumentRepository documentRepository;
    private final RestTemplate restTemplate;

    @Value("${ai-service.base-url}")
    private String aiServiceBaseUrl;

    @Value("${ai-service.internal-key:helpdesk-ai-internal-key-change-me}")
    private String internalKey;

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Internal-Key", internalKey);
        return h;
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    @Transactional
    public CompanyDocumentResponse uploadDocument(CreateCompanyDocumentRequest request) {
        UUID orgId = TenantContext.getTenantId();

        // Generate AI heading from title + first 500 chars of content
        String aiHeading = generateAiHeading(request.getTitle(), request.getContent());

        CompanyDocument doc = CompanyDocument.builder()
                .organizationId(orgId)
                .title(request.getTitle())
                .content(request.getContent())
                .fileType(request.getFileType() != null ? request.getFileType() : "TXT")
                .sourceUrl(request.getSourceUrl())
                .description(request.getDescription())
                .aiHeading(aiHeading)
                .indexedInRag(false)
                .build();
        doc = documentRepository.save(doc);

        // Trigger RAG indexing
        boolean indexed = ingestIntoRag(orgId, doc.getId(), doc.getTitle(), doc.getContent());
        doc.setIndexedInRag(indexed);
        doc = documentRepository.save(doc);

        log.info("Company document '{}' [heading: '{}'] uploaded and {} for org {}",
                doc.getTitle(), aiHeading, indexed ? "indexed in RAG" : "RAG indexing failed", orgId);

        return toResponse(doc, false);
    }

    // ── List (summary cards) ──────────────────────────────────────────────────

    public Page<CompanyDocumentResponse> listDocuments(Pageable pageable) {
        UUID orgId = TenantContext.getTenantId();
        return documentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId, pageable)
                .map(d -> toResponse(d, false));
    }

    // ── Get single doc (full content for popup) ───────────────────────────────

    public CompanyDocumentResponse getDocument(UUID docId) {
        UUID orgId = TenantContext.getTenantId();
        CompanyDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyDocument", "id", docId));
        if (!doc.getOrganizationId().equals(orgId)) {
            throw new ResourceNotFoundException("CompanyDocument", "id", docId);
        }
        return toResponse(doc, true); // include full content
    }

    // ── Update (with optional RAG re-index) ───────────────────────────────────

    @Transactional
    public CompanyDocumentResponse updateDocument(UUID docId, UpdateCompanyDocumentRequest request) {
        UUID orgId = TenantContext.getTenantId();
        CompanyDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyDocument", "id", docId));
        if (!doc.getOrganizationId().equals(orgId)) {
            throw new ResourceNotFoundException("CompanyDocument", "id", docId);
        }

        boolean contentChanged = false;

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            doc.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            doc.setDescription(request.getDescription());
        }
        if (request.getFileType() != null) {
            doc.setFileType(request.getFileType());
        }
        if (request.getSourceUrl() != null) {
            doc.setSourceUrl(request.getSourceUrl());
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            doc.setContent(request.getContent());
            contentChanged = true;
            // Regenerate AI heading when content changes
            doc.setAiHeading(generateAiHeading(doc.getTitle(), request.getContent()));
        }

        doc = documentRepository.save(doc);

        if (contentChanged) {
            boolean indexed = ingestIntoRag(orgId, doc.getId(), doc.getTitle(), doc.getContent());
            doc.setIndexedInRag(indexed);
            doc = documentRepository.save(doc);
            log.info("Re-indexed doc {} after content update for org {}", docId, orgId);
        }

        return toResponse(doc, true);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteDocument(UUID docId) {
        UUID orgId = TenantContext.getTenantId();
        CompanyDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyDocument", "id", docId));

        if (!doc.getOrganizationId().equals(orgId)) {
            throw new ResourceNotFoundException("CompanyDocument", "id", docId);
        }

        deleteFromRag(orgId, docId);
        documentRepository.delete(doc);
        log.info("Company document {} deleted from org {}", docId, orgId);
    }

    // ── Re-index ──────────────────────────────────────────────────────────────

    @Transactional
    public CompanyDocumentResponse reindexDocument(UUID docId) {
        UUID orgId = TenantContext.getTenantId();
        CompanyDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyDocument", "id", docId));

        boolean indexed = ingestIntoRag(orgId, docId, doc.getTitle(), doc.getContent());
        doc.setIndexedInRag(indexed);
        doc = documentRepository.save(doc);
        log.info("Reindexed doc {} for org {}: {}", docId, orgId, indexed);
        return toResponse(doc, false);
    }

    // ── RAG HTTP calls ────────────────────────────────────────────────────────

    private boolean ingestIntoRag(UUID orgId, UUID docId, String title, String content) {
        try {
            Map<String, String> body = Map.of(
                    "org_id", orgId.toString(),
                    "doc_id", docId.toString(),
                    "title", title,
                    "content", content);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServiceBaseUrl + "/api/v1/ai/rag/ingest",
                    new HttpEntity<>(body, headers()), Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.warn("RAG ingest failed for doc {}: {}", docId, e.getMessage());
            return false;
        }
    }

    private void deleteFromRag(UUID orgId, UUID docId) {
        try {
            Map<String, String> body = Map.of(
                    "org_id", orgId.toString(),
                    "doc_id", docId.toString());
            restTemplate.postForEntity(
                    aiServiceBaseUrl + "/api/v1/ai/rag/delete",
                    new HttpEntity<>(body, headers()), Map.class);
        } catch (RestClientException e) {
            log.warn("RAG delete failed for doc {}: {}", docId, e.getMessage());
        }
    }

    /**
     * Calls the AI service to generate a concise 3-6 word heading for the document card.
     * Falls back to the document title if the AI service is unavailable.
     */
    @SuppressWarnings("unchecked")
    private String generateAiHeading(String title, String content) {
        try {
            String snippet = content != null && content.length() > 500
                    ? content.substring(0, 500) : content;
            Map<String, String> body = Map.of(
                    "title", title,
                    "content_snippet", snippet != null ? snippet : "");
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServiceBaseUrl + "/api/v1/ai/generate-heading",
                    new HttpEntity<>(body, headers()), Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object heading = response.getBody().get("heading");
                if (heading instanceof String h && !h.isBlank()) {
                    return h;
                }
            }
        } catch (Exception e) {
            log.warn("AI heading generation failed, using title fallback: {}", e.getMessage());
        }
        // Fallback: first 6 words of title
        String[] words = title.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(6, words.length); i++) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(words[i]);
        }
        return sb.toString();
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private CompanyDocumentResponse toResponse(CompanyDocument doc, boolean includeFullContent) {
        String content = doc.getContent();
        int wordCount = content != null ? content.trim().split("\\s+").length : 0;
        String preview = content != null && content.length() > 300
                ? content.substring(0, 300) + "…"
                : content;

        return CompanyDocumentResponse.builder()
                .id(doc.getId())
                .organizationId(doc.getOrganizationId())
                .title(doc.getTitle())
                .aiHeading(doc.getAiHeading() != null ? doc.getAiHeading() : doc.getTitle())
                .description(doc.getDescription())
                .fileType(doc.getFileType())
                .sourceUrl(doc.getSourceUrl())
                .indexedInRag(doc.getIndexedInRag())
                .contentPreview(preview)
                .fullContent(includeFullContent ? content : null)
                .wordCount(wordCount)
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
