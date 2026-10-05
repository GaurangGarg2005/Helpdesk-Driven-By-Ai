package com.helpdeskAi.kb.repository;

import com.helpdeskAi.kb.model.KbArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KbArticleRepository extends JpaRepository<KbArticle, UUID> {

    Page<KbArticle> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Page<KbArticle> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
            UUID organizationId, String status, Pageable pageable);

    Page<KbArticle> findByOrganizationIdAndCategoryIdOrderByCreatedAtDesc(
            UUID organizationId, UUID categoryId, Pageable pageable);

    Optional<KbArticle> findByOrganizationIdAndSlug(UUID organizationId, String slug);

    List<KbArticle> findByOrganizationIdAndStatusAndIsFeaturedTrueOrderByViewCountDesc(
            UUID organizationId, String status);

    // Full-text search over title, excerpt and tags
    @Query("SELECT a FROM KbArticle a WHERE a.organizationId = :orgId AND a.status = 'PUBLISHED' " +
           "AND (LOWER(a.title) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "  OR LOWER(a.excerpt) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "  OR LOWER(a.tags) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<KbArticle> search(@Param("orgId") UUID organizationId,
                            @Param("q") String query,
                            Pageable pageable);

    long countByOrganizationIdAndStatus(UUID organizationId, String status);
}
