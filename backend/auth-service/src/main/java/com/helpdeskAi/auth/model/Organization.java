package com.helpdeskAi.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Builder.Default
    @Column(nullable = false)
    private String plan = "free";

    @Column(name = "logo_url")
    private String logoUrl;

    @Builder.Default
    @Column(name = "primary_color")
    private String primaryColor = "#4F46E5";

    @Column(name = "custom_domain")
    private String customDomain;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
