package com.helpdeskAi.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "role_permissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"role", "permission_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String role;

    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", insertable = false, updatable = false)
    private Permission permission;
}
