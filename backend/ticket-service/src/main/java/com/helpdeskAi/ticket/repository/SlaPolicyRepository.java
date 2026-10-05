package com.helpdeskAi.ticket.repository;

import com.helpdeskAi.ticket.model.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, UUID> {
    List<SlaPolicy> findByOrganizationId(UUID organizationId);
    Optional<SlaPolicy> findByOrganizationIdAndPriorityAndIsDefaultTrue(UUID organizationId, String priority);
}
