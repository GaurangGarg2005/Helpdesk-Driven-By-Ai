package com.helpdeskAi.auth.repository;

import com.helpdeskAi.auth.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Optional<Organization> findByNameIgnoreCase(String name);
    List<Organization> findByNameContainingIgnoreCaseOrderByName(String name);
}

