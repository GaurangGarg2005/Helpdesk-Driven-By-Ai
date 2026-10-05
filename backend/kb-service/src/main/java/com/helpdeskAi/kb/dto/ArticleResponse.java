package com.helpdeskAi.kb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ArticleResponse {
    private UUID id;
    private UUID organizationId;
    private UUID categoryId;
    private UUID authorId;
    private String authorName;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String contentHtml;
    private String status;
    private Long viewCount;
    private Long helpfulCount;
    private Long notHelpfulCount;
    private String tags;
    private Boolean isFeatured;
    private String seoTitle;
    private String seoDescription;
    private Instant createdAt;
    private Instant updatedAt;
}
