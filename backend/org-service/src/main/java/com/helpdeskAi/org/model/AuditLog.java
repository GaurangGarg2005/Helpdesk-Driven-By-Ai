package com.helpdeskAi.org.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_org_date", columnList = "organization_id, created_at DESC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AuditLog extends TenantAwareEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(columnDefinition = "JSONB")
    private String changes;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
}
