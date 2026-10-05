package com.helpdeskAi.kb.controller;

import com.helpdeskAi.common.dto.ApiResponse;
import com.helpdeskAi.kb.dto.CompanyDocumentResponse;
import com.helpdeskAi.kb.dto.CreateCompanyDocumentRequest;
import com.helpdeskAi.kb.dto.UpdateCompanyDocumentRequest;
import com.helpdeskAi.kb.service.CompanyDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Company Knowledge Document endpoints — accessible to ADMIN and OWNER roles only.
 */
@RestController
@RequestMapping("/api/v1/kb/company-docs")
@RequiredArgsConstructor
public class CompanyDocumentController {

    private final CompanyDocumentService documentService;

    /** Upload a new company document. */
    @PostMapping
    public ResponseEntity<ApiResponse<CompanyDocumentResponse>> uploadDocument(
            @Valid @RequestBody CreateCompanyDocumentRequest request) {
        CompanyDocumentResponse doc = documentService.uploadDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Document uploaded and indexed", doc, Instant.now()));
    }

    /** List all company documents for the current org (summary cards — no full content). */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CompanyDocumentResponse>>> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<CompanyDocumentResponse> docs = documentService.listDocuments(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Documents retrieved", docs, Instant.now()));
    }

    /** Get a single document with FULL content (for the popup/editor view). */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyDocumentResponse>> getDocument(@PathVariable UUID id) {
        CompanyDocumentResponse doc = documentService.getDocument(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document retrieved", doc, Instant.now()));
    }

    /** Update a document's content/title — re-indexes in RAG automatically if content changes. */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyDocumentResponse>> updateDocument(
            @PathVariable UUID id, @RequestBody UpdateCompanyDocumentRequest request) {
        CompanyDocumentResponse doc = documentService.updateDocument(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document updated", doc, Instant.now()));
    }

    /** Delete a company document and remove it from the RAG index. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document deleted", null, Instant.now()));
    }

    /** Force re-index a document into the RAG store. */
    @PostMapping("/{id}/reindex")
    public ResponseEntity<ApiResponse<CompanyDocumentResponse>> reindexDocument(@PathVariable UUID id) {
        CompanyDocumentResponse doc = documentService.reindexDocument(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document re-indexed", doc, Instant.now()));
    }
}
