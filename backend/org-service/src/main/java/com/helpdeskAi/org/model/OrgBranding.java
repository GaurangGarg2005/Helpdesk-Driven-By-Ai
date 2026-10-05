package com.helpdeskAi.org.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "org_branding")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrgBranding extends TenantAwareEntity {

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "favicon_url", columnDefinition = "TEXT")
    private String faviconUrl;

    @Column(name = "primary_color", length = 7)
    @Builder.Default
    private String primaryColor = "#4F46E5";

    @Column(name = "accent_color", length = 7)
    @Builder.Default
    private String accentColor = "#14B8A6";

    @Column(name = "heading_font")
    private String headingFont;

    @Column(name = "body_font")
    private String bodyFont;

    @Column(name = "custom_domain")
    private String customDomain;

    @Column(name = "portal_title")
    private String portalTitle;

    @Column(name = "portal_subtitle", columnDefinition = "TEXT")
    private String portalSubtitle;
}
