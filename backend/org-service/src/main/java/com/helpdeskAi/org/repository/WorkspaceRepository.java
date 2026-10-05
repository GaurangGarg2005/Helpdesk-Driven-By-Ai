package com.helpdeskAi.org.repository;

import com.helpdeskAi.org.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    List<Workspace> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
