package com.helpdeskAi.kb.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "kb_articles", indexes = {
    @Index(name = "idx_kb_articles_org_status", columnList = "organization_id, status"),
    @Index(name = "idx_kb_articles_slug",       columnList = "organization_id, slug", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class KbArticle extends TenantAwareEntity {

    @Column(nullable = false, length = 500)
    private String title;

    @Column(unique = false, length = 255)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";   // DRAFT | PUBLISHED | ARCHIVED

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "helpful_count")
    @Builder.Default
    private Long helpfulCount = 0L;

    @Column(name = "not_helpful_count")
    @Builder.Default
    private Long notHelpfulCount = 0L;

    @Column(columnDefinition = "TEXT")
    private String tags;  // Comma-separated

    @Builder.Default
    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "seo_title", length = 200)
    private String seoTitle;

    @Column(name = "seo_description", length = 500)
    private String seoDescription;
}
