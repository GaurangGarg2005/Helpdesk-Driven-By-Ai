package com.helpdeskAi.org.service;

import com.helpdeskAi.common.security.TenantContext;
import com.helpdeskAi.org.dto.AuditLogResponse;
import com.helpdeskAi.org.model.AuditLog;
import com.helpdeskAi.org.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Record an audit log entry. Called by controllers/services after mutations.
     */
    public void log(UUID userId, String userEmail, String action,
                    String entityType, UUID entityId, String changes, String ipAddress) {
        UUID orgId = TenantContext.getTenantId();
        AuditLog entry = AuditLog.builder()
                .organizationId(orgId)
                .userId(userId)
                .userEmail(userEmail)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .changes(changes)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(entry);
    }

    public Page<AuditLogResponse> listAuditLogs(Pageable pageable) {
        UUID orgId = TenantContext.getTenantId();
        return auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId, pageable)
                .map(this::toResponse);
    }

    public Page<AuditLogResponse> listAuditLogsByUser(UUID userId, Pageable pageable) {
        UUID orgId = TenantContext.getTenantId();
        return auditLogRepository.findByOrganizationIdAndUserIdOrderByCreatedAtDesc(orgId, userId, pageable)
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .userEmail(a.getUserEmail())
                .action(a.getAction())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .changes(a.getChanges())
                .ipAddress(a.getIpAddress())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
