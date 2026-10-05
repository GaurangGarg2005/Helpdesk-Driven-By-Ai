package com.helpdeskAi.kb.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "kb_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class KbCategory extends TenantAwareEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_name", length = 50)
    private String iconName;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "is_public")
    private Boolean isPublic = true;
}
