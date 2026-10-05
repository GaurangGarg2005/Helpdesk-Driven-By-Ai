package com.helpdeskAi.kb.service;

import com.helpdeskAi.common.exception.ResourceNotFoundException;
import com.helpdeskAi.common.security.TenantContext;
import com.helpdeskAi.kb.dto.*;
import com.helpdeskAi.kb.model.*;
import com.helpdeskAi.kb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbService {

    private final KbArticleRepository  articleRepo;
    private final KbCategoryRepository categoryRepo;

    // ── Categories ──────────────────────────────────────────────────────

    public List<CategoryResponse> listCategories() {
        UUID orgId = TenantContext.getTenantId();
        return categoryRepo.findByOrganizationIdOrderBySortOrderAsc(orgId)
                .stream().map(c -> toCategory(c, orgId)).collect(Collectors.toList());
    }

    public List<CategoryResponse> listPublicCategories(UUID orgId) {
        return categoryRepo.findByOrganizationIdAndIsPublicTrueOrderBySortOrderAsc(orgId)
                .stream().map(c -> toCategory(c, orgId)).collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest req) {
        UUID orgId = TenantContext.getTenantId();
        KbCategory cat = KbCategory.builder()
                .organizationId(orgId)
                .name(req.getName())
                .description(req.getDescription())
                .iconName(req.getIconName())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .isPublic(req.getIsPublic() != null ? req.getIsPublic() : true)
                .build();
        cat = categoryRepo.save(cat);
        return toCategory(cat, orgId);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        if (!categoryRepo.existsById(id))
            throw new ResourceNotFoundException("Category", "id", id);
        categoryRepo.deleteById(id);
    }

    // ── Articles ─────────────────────────────────────────────────────────

    public Page<ArticleResponse> listArticles(String status, UUID categoryId, Pageable pageable) {
        UUID orgId = TenantContext.getTenantId();
        if (status != null && categoryId != null) {
            return articleRepo.findByOrganizationIdAndCategoryIdOrderByCreatedAtDesc(orgId, categoryId, pageable)
                    .map(this::toArticle);
        }
        if (status != null) {
            return articleRepo.findByOrganizationIdAndStatusOrderByCreatedAtDesc(orgId, status, pageable)
                    .map(this::toArticle);
        }
        return articleRepo.findByOrganizationIdOrderByCreatedAtDesc(orgId, pageable)
                .map(this::toArticle);
    }

    public ArticleResponse getArticleById(UUID id) {
        KbArticle a = articleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "id", id));
        incrementView(a);
        return toArticle(a);
    }

    public ArticleResponse getPublicArticleBySlug(UUID orgId, String slug) {
        KbArticle a = articleRepo.findByOrganizationIdAndSlug(orgId, slug)
                .filter(art -> "PUBLISHED".equals(art.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Article", "slug", slug));
        incrementView(a);
        return toArticle(a);
    }

    public Page<ArticleResponse> searchPublic(UUID orgId, String q, Pageable pageable) {
        return articleRepo.search(orgId, q, pageable).map(this::toArticle);
    }

    public Page<ArticleResponse> searchInternal(String q, Pageable pageable) {
        UUID orgId = TenantContext.getTenantId();
        return articleRepo.search(orgId, q, pageable).map(this::toArticle);
    }

    public List<ArticleResponse> getFeaturedPublished() {
        UUID orgId = TenantContext.getTenantId();
        return articleRepo
                .findByOrganizationIdAndStatusAndIsFeaturedTrueOrderByViewCountDesc(orgId, "PUBLISHED")
                .stream().map(this::toArticle).collect(Collectors.toList());
    }

    @Transactional
    public ArticleResponse createArticle(CreateArticleRequest req, UUID authorId, String authorName) {
        UUID orgId = TenantContext.getTenantId();
        String slug = req.getSlug() != null && !req.getSlug().isBlank()
                ? req.getSlug()
                : slugify(req.getTitle());

        KbArticle article = KbArticle.builder()
                .organizationId(orgId)
                .title(req.getTitle())
                .slug(slug)
                .excerpt(req.getExcerpt())
                .content(req.getContent())
                .contentHtml(req.getContentHtml())
                .status(req.getStatus() != null ? req.getStatus() : "DRAFT")
                .categoryId(req.getCategoryId())
                .authorId(authorId)
                .authorName(authorName)
                .tags(req.getTags())
                .isFeatured(Boolean.TRUE.equals(req.getIsFeatured()))
                .seoTitle(req.getSeoTitle())
                .seoDescription(req.getSeoDescription())
                .build();
        article = articleRepo.save(article);
        log.info("Created KB article '{}' [{}]", article.getTitle(), article.getStatus());
        return toArticle(article);
    }

    @Transactional
    public ArticleResponse updateArticle(UUID id, CreateArticleRequest req) {
        KbArticle article = articleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "id", id));
        if (req.getTitle()          != null) article.setTitle(req.getTitle());
        if (req.getExcerpt()        != null) article.setExcerpt(req.getExcerpt());
        if (req.getContent()        != null) article.setContent(req.getContent());
        if (req.getContentHtml()    != null) article.setContentHtml(req.getContentHtml());
        if (req.getStatus()         != null) article.setStatus(req.getStatus());
        if (req.getCategoryId()     != null) article.setCategoryId(req.getCategoryId());
        if (req.getTags()           != null) article.setTags(req.getTags());
        if (req.getIsFeatured()     != null) article.setIsFeatured(req.getIsFeatured());
        if (req.getSeoTitle()       != null) article.setSeoTitle(req.getSeoTitle());
        if (req.getSeoDescription() != null) article.setSeoDescription(req.getSeoDescription());
        article = articleRepo.save(article);
        return toArticle(article);
    }

    @Transactional
    public ArticleResponse publishArticle(UUID id) {
        KbArticle article = articleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "id", id));
        article.setStatus("PUBLISHED");
        return toArticle(articleRepo.save(article));
    }

    @Transactional
    public void deleteArticle(UUID id) {
        if (!articleRepo.existsById(id))
            throw new ResourceNotFoundException("Article", "id", id);
        articleRepo.deleteById(id);
    }

    @Transactional
    public void rateArticle(UUID id, boolean helpful) {
        KbArticle a = articleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", "id", id));
        if (helpful) a.setHelpfulCount(a.getHelpfulCount() + 1);
        else         a.setNotHelpfulCount(a.getNotHelpfulCount() + 1);
        articleRepo.save(a);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private void incrementView(KbArticle a) {
        a.setViewCount(a.getViewCount() + 1);
        articleRepo.save(a);
    }

    private String slugify(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private ArticleResponse toArticle(KbArticle a) {
        return ArticleResponse.builder()
                .id(a.getId()).organizationId(a.getOrganizationId())
                .categoryId(a.getCategoryId()).authorId(a.getAuthorId())
                .authorName(a.getAuthorName()).title(a.getTitle()).slug(a.getSlug())
                .excerpt(a.getExcerpt()).content(a.getContent()).contentHtml(a.getContentHtml())
                .status(a.getStatus()).viewCount(a.getViewCount())
                .helpfulCount(a.getHelpfulCount()).notHelpfulCount(a.getNotHelpfulCount())
                .tags(a.getTags()).isFeatured(a.getIsFeatured())
                .seoTitle(a.getSeoTitle()).seoDescription(a.getSeoDescription())
                .createdAt(a.getCreatedAt()).updatedAt(a.getUpdatedAt()).build();
    }

    private CategoryResponse toCategory(KbCategory c, UUID orgId) {
        long count = articleRepo.countByOrganizationIdAndStatus(orgId, "PUBLISHED");
        return CategoryResponse.builder()
                .id(c.getId()).organizationId(c.getOrganizationId())
                .name(c.getName()).description(c.getDescription())
                .iconName(c.getIconName()).sortOrder(c.getSortOrder())
                .isPublic(c.getIsPublic()).articleCount(count)
                .createdAt(c.getCreatedAt()).build();
    }
}
