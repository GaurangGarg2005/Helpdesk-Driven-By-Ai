package com.helpdeskAi.kb.dto;

import lombok.Data;

@Data
public class UpdateCompanyDocumentRequest {
    private String title;
    private String content;
    private String description;
    private String fileType;
    private String sourceUrl;
}
