package com.helpdeskAi.org.controller;

import com.helpdeskAi.common.dto.ApiResponse;
import com.helpdeskAi.org.dto.AuditLogResponse;
import com.helpdeskAi.org.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/org/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID userId) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<AuditLogResponse> logs;
        if (userId != null) {
            logs = auditLogService.listAuditLogsByUser(userId, pageable);
        } else {
            logs = auditLogService.listAuditLogs(pageable);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Audit logs retrieved", logs, Instant.now()));
    }
}
