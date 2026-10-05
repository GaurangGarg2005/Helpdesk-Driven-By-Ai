package com.helpdeskAi.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TicketResponse {
    private UUID id;
    private UUID organizationId;
    private Long ticketNumber;
    private String subject;
    private String description;
    private String status;
    private String priority;
    private String category;
    private String channel;
    private String sentiment;
    private BigDecimal sentimentScore;
    private String aiSummary;
    private UUID departmentId;
    private UUID workspaceId;
    private UUID assignedAgentId;
    private UUID assignedTeamId;
    private UUID requesterId;
    private String requesterEmail;
    private String requesterName;
    private Instant firstResponseDueAt;
    private Instant resolutionDueAt;
    private Instant firstResponseAt;
    private Instant resolvedAt;
    private Boolean slaFirstResponseBreached;
    private Boolean slaResolutionBreached;
    private String tags;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean aiHandlingActive;
    private Integer humanEscalationCount;
    private Instant lastCustomerReplyAt;
    private List<TicketMessageResponse> messages;
    private List<TicketActivityResponse> activities;
}

