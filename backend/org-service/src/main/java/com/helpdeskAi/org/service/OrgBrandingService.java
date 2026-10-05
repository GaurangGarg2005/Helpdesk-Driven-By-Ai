package com.helpdeskAi.org.service;

import com.helpdeskAi.org.dto.BrandingResponse;
import com.helpdeskAi.org.dto.UpdateBrandingRequest;
import com.helpdeskAi.org.model.OrgBranding;
import com.helpdeskAi.org.repository.OrgBrandingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrgBrandingService {

    private final OrgBrandingRepository orgBrandingRepository;

    public BrandingResponse getBranding(UUID organizationId) {
        OrgBranding branding = orgBrandingRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> {
                    OrgBranding defaultBranding = OrgBranding.builder()
                            .organizationId(organizationId)
                            .primaryColor("#4F46E5")
                            .accentColor("#14B8A6")
                            .build();
                    return orgBrandingRepository.save(defaultBranding);
                });
        return mapToDto(branding);
    }

    @Transactional
    public BrandingResponse updateBranding(UUID organizationId, UpdateBrandingRequest request) {
        OrgBranding branding = orgBrandingRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> OrgBranding.builder().organizationId(organizationId).build());

        if (request.getLogoUrl() != null) branding.setLogoUrl(request.getLogoUrl());
        if (request.getFaviconUrl() != null) branding.setFaviconUrl(request.getFaviconUrl());
        if (request.getPrimaryColor() != null) branding.setPrimaryColor(request.getPrimaryColor());
        if (request.getAccentColor() != null) branding.setAccentColor(request.getAccentColor());
        if (request.getHeadingFont() != null) branding.setHeadingFont(request.getHeadingFont());
        if (request.getBodyFont() != null) branding.setBodyFont(request.getBodyFont());
        if (request.getCustomDomain() != null) branding.setCustomDomain(request.getCustomDomain());
        if (request.getPortalTitle() != null) branding.setPortalTitle(request.getPortalTitle());
        if (request.getPortalSubtitle() != null) branding.setPortalSubtitle(request.getPortalSubtitle());

        branding = orgBrandingRepository.save(branding);
        return mapToDto(branding);
    }

    private BrandingResponse mapToDto(OrgBranding b) {
        BrandingResponse dto = new BrandingResponse();
        dto.setLogoUrl(b.getLogoUrl());
        dto.setFaviconUrl(b.getFaviconUrl());
        dto.setPrimaryColor(b.getPrimaryColor());
        dto.setAccentColor(b.getAccentColor());
        dto.setHeadingFont(b.getHeadingFont());
        dto.setBodyFont(b.getBodyFont());
        dto.setCustomDomain(b.getCustomDomain());
        dto.setPortalTitle(b.getPortalTitle());
        dto.setPortalSubtitle(b.getPortalSubtitle());
        return dto;
    }
}
