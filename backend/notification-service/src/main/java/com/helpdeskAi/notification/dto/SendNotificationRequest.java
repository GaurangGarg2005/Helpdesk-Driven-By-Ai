package com.helpdeskAi.notification.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SendNotificationRequest {
    private UUID organizationId;
    private UUID recipientId;
    private String recipientEmail;
    private String type;          // TICKET_ASSIGNED | TICKET_REPLIED | TICKET_RESOLVED | TICKET_SLA_BREACH | MENTION
    private String title;
    private String body;
    private String actionUrl;
    private UUID referenceId;
    private boolean sendEmail = true;
}
