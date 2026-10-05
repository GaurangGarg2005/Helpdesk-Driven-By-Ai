package com.helpdeskAi.org.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BrandingResponse {
    private UUID id;
    private UUID organizationId;
    private String logoUrl;
    private String faviconUrl;
    private String primaryColor;
    private String accentColor;
    private String headingFont;
    private String bodyFont;
    private String customDomain;
    private String portalTitle;
    private String portalSubtitle;
}
