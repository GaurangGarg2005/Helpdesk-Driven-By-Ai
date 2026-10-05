package com.helpdeskAi.org.controller;

import com.helpdeskAi.common.dto.ApiResponse;
import com.helpdeskAi.common.security.TenantContext;
import com.helpdeskAi.org.dto.BrandingResponse;
import com.helpdeskAi.org.dto.UpdateBrandingRequest;
import com.helpdeskAi.org.service.OrgBrandingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/org/branding")
@RequiredArgsConstructor
public class OrgBrandingController {

    private final OrgBrandingService orgBrandingService;

    @GetMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasAuthority('org:manage')")
    public ResponseEntity<ApiResponse<BrandingResponse>> getBranding() {
        UUID orgId = TenantContext.getTenantId();
        return ok("Branding retrieved", orgBrandingService.getBranding(orgId));
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasAuthority('org:manage')")
    public ResponseEntity<ApiResponse<BrandingResponse>> updateBranding(@RequestBody UpdateBrandingRequest request) {
        UUID orgId = TenantContext.getTenantId();
        return ok("Branding updated successfully", orgBrandingService.updateBranding(orgId, request));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, message, data, Instant.now()));
    }
}
