package com.helpdeskAi.org.dto;

import lombok.Data;

@Data
public class UpdateBrandingRequest {
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
