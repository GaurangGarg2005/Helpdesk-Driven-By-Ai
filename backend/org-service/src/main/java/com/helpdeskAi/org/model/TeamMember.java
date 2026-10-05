package com.helpdeskAi.org.model;

import com.helpdeskAi.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "team_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"team_id", "user_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TeamMember extends BaseEntity {

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Builder.Default
    @Column(nullable = false)
    private String role = "MEMBER";
}
