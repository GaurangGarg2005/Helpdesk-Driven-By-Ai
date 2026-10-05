package com.helpdeskAi.org.repository;

import com.helpdeskAi.org.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByOrganizationIdAndIsActiveTrueOrderByCreatedAtDesc(UUID organizationId);
    Optional<ApiKey> findByKeyHash(String keyHash);
}
