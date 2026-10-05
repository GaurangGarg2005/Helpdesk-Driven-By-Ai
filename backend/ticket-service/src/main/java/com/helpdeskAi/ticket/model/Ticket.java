package com.helpdeskAi.ticket.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets", indexes = {
    @Index(name = "idx_tickets_org_status", columnList = "organization_id, status"),
    @Index(name = "idx_tickets_org_assigned", columnList = "organization_id, assigned_agent_id"),
    @Index(name = "idx_tickets_org_created", columnList = "organization_id, created_at DESC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Ticket extends TenantAwareEntity {

    @Column(name = "ticket_number")
    private Long ticketNumber;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "OPEN";

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String priority = "MEDIUM";

    @Column(length = 100)
    private String category;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String channel = "PORTAL";

    @Column(length = 20)
    private String sentiment;

    @Column(name = "sentiment_score", precision = 4, scale = 3)
    private BigDecimal sentimentScore;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Column(name = "assigned_team_id")
    private UUID assignedTeamId;

    @Column(name = "requester_id")
    private UUID requesterId;

    @Column(name = "requester_email")
    private String requesterEmail;

    @Column(name = "requester_name")
    private String requesterName;

    @Column(name = "sla_policy_id")
    private UUID slaPolicyId;

    @Column(name = "first_response_at")
    private Instant firstResponseAt;

    @Column(name = "first_response_due_at")
    private Instant firstResponseDueAt;

    @Column(name = "resolution_due_at")
    private Instant resolutionDueAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Builder.Default
    @Column(name = "sla_first_response_breached")
    private Boolean slaFirstResponseBreached = false;

    @Builder.Default
    @Column(name = "sla_resolution_breached")
    private Boolean slaResolutionBreached = false;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(name = "custom_fields", columnDefinition = "TEXT")
    private String customFields;

    @Column(name = "duplicate_of_ticket_id")
    private UUID duplicateOfTicketId;

    // ── AI handling fields ──────────────────────────────────────────────────

    /** Number of times the customer has asked to speak to a human agent. */
    @Builder.Default
    @Column(name = "human_escalation_count")
    private Integer humanEscalationCount = 0;

    /** Timestamp of the customer's most recent reply — used for 2-day auto-close. */
    @Column(name = "last_customer_reply_at")
    private Instant lastCustomerReplyAt;

    /**
     * Whether the AI is still handling this ticket.
     * Set to false when escalated to NEEDS_AGENT_REVIEW or when agent takes over.
     */
    @Builder.Default
    @Column(name = "ai_handling_active")
    private Boolean aiHandlingActive = true;
}
