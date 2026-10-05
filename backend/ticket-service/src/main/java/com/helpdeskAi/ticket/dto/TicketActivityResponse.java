package com.helpdeskAi.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TicketActivityResponse {
    private UUID id;
    private UUID ticketId;
    private UUID userId;
    private String activityType;
    private String oldValue;
    private String newValue;
    private String description;
    private Instant createdAt;
}
