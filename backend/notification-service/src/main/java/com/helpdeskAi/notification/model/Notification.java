package com.helpdeskAi.notification.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
    @Index(columnList = "recipient_id, is_read"),
    @Index(columnList = "organization_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Notification extends TenantAwareEntity {

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(nullable = false, length = 100)
    private String type;  // TICKET_ASSIGNED | TICKET_REPLIED | TICKET_RESOLVED | TICKET_SLA_BREACH | MENTION

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "action_url")
    private String actionUrl;

    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false;

    @Builder.Default
    @Column(name = "email_sent")
    private Boolean emailSent = false;

    @Column(name = "reference_id")  // ticketId, chatSessionId, etc.
    private UUID referenceId;
}
