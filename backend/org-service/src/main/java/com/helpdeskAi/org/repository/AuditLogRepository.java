package com.helpdeskAi.org.repository;

import com.helpdeskAi.org.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Page<AuditLog> findByOrganizationIdAndActionContainingIgnoreCaseOrderByCreatedAtDesc(
            UUID organizationId, String action, Pageable pageable);

    Page<AuditLog> findByOrganizationIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID organizationId, Instant from, Instant to, Pageable pageable);

    Page<AuditLog> findByOrganizationIdAndUserIdOrderByCreatedAtDesc(
            UUID organizationId, UUID userId, Pageable pageable);
}
