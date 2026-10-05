package com.helpdeskAi.ticket.model;

import com.helpdeskAi.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "ticket_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TicketMessage extends BaseEntity {

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "sender_id")
    private UUID senderId;

    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType;  // CUSTOMER, AGENT, SYSTEM, AI

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Builder.Default
    @Column(name = "is_internal_note")
    private Boolean isInternalNote = false;

    @Column(columnDefinition = "TEXT")
    private String attachments;  // [{url, filename, size, type}]

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_email")
    private String senderEmail;
}
