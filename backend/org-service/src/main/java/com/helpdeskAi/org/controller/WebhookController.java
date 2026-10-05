package com.helpdeskAi.org.controller;

import com.helpdeskAi.common.dto.ApiResponse;
import com.helpdeskAi.common.security.TenantContext;
import com.helpdeskAi.org.dto.CreateWebhookRequest;
import com.helpdeskAi.org.dto.WebhookResponse;
import com.helpdeskAi.org.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/org/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @GetMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasAuthority('org:manage')")
    public ResponseEntity<ApiResponse<List<WebhookResponse>>> listWebhooks() {
        UUID orgId = TenantContext.getTenantId();
        return ok("Webhooks retrieved", webhookService.listWebhooks(orgId));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasAuthority('org:manage')")
    public ResponseEntity<ApiResponse<WebhookResponse>> createWebhook(@RequestBody CreateWebhookRequest request) {
        UUID orgId = TenantContext.getTenantId();
        WebhookResponse response = webhookService.createWebhook(orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Webhook created successfully", response, Instant.now()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasAuthority('org:manage')")
    public ResponseEntity<ApiResponse<Void>> deleteWebhook(@PathVariable UUID id) {
        UUID orgId = TenantContext.getTenantId();
        webhookService.deleteWebhook(orgId, id);
        return ok("Webhook deleted", null);
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, message, data, Instant.now()));
    }
}
