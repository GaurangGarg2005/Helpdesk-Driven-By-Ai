package com.helpdeskAi.kb.repository;

import com.helpdeskAi.kb.model.CompanyDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompanyDocumentRepository extends JpaRepository<CompanyDocument, UUID> {
    Page<CompanyDocument> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);
    long countByOrganizationIdAndIndexedInRag(UUID organizationId, boolean indexed);
}
