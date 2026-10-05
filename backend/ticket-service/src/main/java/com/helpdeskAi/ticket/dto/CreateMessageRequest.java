package com.helpdeskAi.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMessageRequest {
    @NotBlank(message = "Message body is required")
    private String body;
    private String bodyHtml;
    private boolean internalNote = false;
    private String attachments;  // JSON array of attachment objects
}
