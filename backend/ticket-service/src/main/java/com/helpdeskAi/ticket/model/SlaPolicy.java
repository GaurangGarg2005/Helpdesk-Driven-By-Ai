package com.helpdeskAi.ticket.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sla_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SlaPolicy extends TenantAwareEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 20)
    private String priority;  // LOW, MEDIUM, HIGH, URGENT

    @Column(name = "first_response_hours", nullable = false)
    private Integer firstResponseHours;

    @Column(name = "resolution_hours", nullable = false)
    private Integer resolutionHours;

    @Builder.Default
    @Column(name = "is_default")
    private Boolean isDefault = false;
}
