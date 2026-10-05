package com.helpdeskAi.org.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ApiKey extends TenantAwareEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "key_hash", nullable = false)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false, length = 10)
    private String keyPrefix;

    @Column(columnDefinition = "TEXT")
    private String scopes;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}
