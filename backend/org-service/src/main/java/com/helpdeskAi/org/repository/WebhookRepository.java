package com.helpdeskAi.org.repository;

import com.helpdeskAi.org.model.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, UUID> {
    List<Webhook> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<Webhook> findByOrganizationIdAndIsActiveTrue(UUID organizationId);
}
