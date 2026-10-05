package com.helpdeskAi.kb.repository;

import com.helpdeskAi.kb.model.KbCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KbCategoryRepository extends JpaRepository<KbCategory, UUID> {
    List<KbCategory> findByOrganizationIdOrderBySortOrderAsc(UUID organizationId);
    List<KbCategory> findByOrganizationIdAndIsPublicTrueOrderBySortOrderAsc(UUID organizationId);
}
