package com.helpdeskAi.common.entity;

import com.helpdeskAi.common.security.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (this.organizationId == null && TenantContext.getTenantId() != null) {
            this.organizationId = TenantContext.getTenantId();
        }
    }
}
