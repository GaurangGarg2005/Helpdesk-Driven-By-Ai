package com.helpdeskAi.org.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "teams")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Team extends TenantAwareEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "workspace_id")
    private UUID workspaceId;
}
