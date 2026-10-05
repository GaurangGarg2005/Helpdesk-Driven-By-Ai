package com.helpdeskAi.org.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "webhooks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Webhook extends TenantAwareEntity {

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String events;  // Comma-separated event types

    @Column(nullable = false)
    private String secret;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String description;
}
