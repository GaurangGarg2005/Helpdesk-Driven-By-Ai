package com.helpdeskAi.kb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CompanyDocumentResponse {
    private UUID id;
    private UUID organizationId;
    private String title;
    private String aiHeading;       // AI-generated card heading
    private String description;
    private String fileType;
    private String sourceUrl;
    private Boolean indexedInRag;
    private String contentPreview;  // First ~200 chars for card summary
    private String fullContent;     // Full content — only populated on GET /{id}
    private int wordCount;          // Total words for display
    private Instant createdAt;
    private Instant updatedAt;
}

