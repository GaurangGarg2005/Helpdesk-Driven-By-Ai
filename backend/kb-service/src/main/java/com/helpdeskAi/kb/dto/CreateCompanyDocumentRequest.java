package com.helpdeskAi.kb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCompanyDocumentRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    /** TXT, MARKDOWN, PDF, URL */
    private String fileType;

    private String sourceUrl;

    private String description;
}
