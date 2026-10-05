package com.helpdeskAi.kb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateArticleRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String slug;      // Auto-generated from title if blank
    private String excerpt;

    @NotBlank(message = "Content is required")
    private String content;

    private String contentHtml;
    private UUID   categoryId;
    private String tags;
    private Boolean isFeatured = false;
    private String seoTitle;
    private String seoDescription;
    private String status = "DRAFT";  // DRAFT | PUBLISHED
}
