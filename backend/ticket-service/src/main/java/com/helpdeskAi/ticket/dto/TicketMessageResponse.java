package com.helpdeskAi.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TicketMessageResponse {
    private UUID id;
    private UUID ticketId;
    private UUID senderId;
    private String senderType;
    private String senderName;
    private String senderEmail;
    private String body;
    private String bodyHtml;
    private Boolean isInternalNote;
    private String attachments;
    private Instant createdAt;
}
