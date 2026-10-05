package com.helpdeskAi.org.repository;

import com.helpdeskAi.org.model.OrgBranding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgBrandingRepository extends JpaRepository<OrgBranding, UUID> {
    Optional<OrgBranding> findByOrganizationId(UUID organizationId);
}
