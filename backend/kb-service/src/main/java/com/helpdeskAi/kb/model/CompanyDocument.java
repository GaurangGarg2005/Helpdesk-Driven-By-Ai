package com.helpdeskAi.kb.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Company-specific document uploaded by an ADMIN/OWNER for RAG training.
 * Content is indexed into the AI service's per-org FAISS vector store.
 */
@Entity
@Table(name = "company_documents", indexes = {
    @Index(name = "idx_company_docs_org", columnList = "organization_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CompanyDocument extends TenantAwareEntity {

    @Column(nullable = false, length = 500)
    private String title;

    /** Raw text content (may be extracted from PDF or pasted directly). */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Source type: TXT, MARKDOWN, PDF, URL.
     * Used to display the appropriate icon in the admin UI.
     */
    @Builder.Default
    @Column(name = "file_type", length = 50)
    private String fileType = "TXT";

    /** Optional URL if the document came from a web page. */
    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    /** True once the document has been successfully indexed in the RAG vector store. */
    @Builder.Default
    @Column(name = "indexed_in_rag")
    private Boolean indexedInRag = false;

    /** Short description shown in the admin list view. */
    @Column(length = 500)
    private String description;

    /**
     * AI-generated short heading (3-6 words) that names this card in the UI.
     * Generated on upload so admins can instantly identify each knowledge chunk.
     */
    @Column(name = "ai_heading", length = 200)
    private String aiHeading;
}

