package com.helpdeskAi.org.controller;

import com.helpdeskAi.common.dto.ApiResponse;
import com.helpdeskAi.common.security.TenantContext;
import com.helpdeskAi.common.security.UserPrincipal;
import com.helpdeskAi.org.dto.ApiKeyCreatedResponse;
import com.helpdeskAi.org.dto.ApiKeyResponse;
import com.helpdeskAi.org.dto.CreateApiKeyRequest;
import com.helpdeskAi.org.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/org/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasAuthority('org:manage')")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> listApiKeys() {
        UUID orgId = TenantContext.getTenantId();
        List<ApiKeyResponse> keys = apiKeyService.listApiKeys(orgId);
        return ok("API Keys retrieved", keys);
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasAuthority('org:manage')")
    public ResponseEntity<ApiResponse<ApiKeyCreatedResponse>> createApiKey(@RequestBody CreateApiKeyRequest request) {
        UUID orgId = TenantContext.getTenantId();
        UserPrincipal user = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ApiKeyCreatedResponse response = apiKeyService.createApiKey(orgId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "API Key created successfully", response, Instant.now()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasAuthority('org:manage')")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(@PathVariable UUID id) {
        UUID orgId = TenantContext.getTenantId();
        apiKeyService.deleteApiKey(orgId, id);
        return ok("API Key deleted", null);
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, message, data, Instant.now()));
    }
}
