package com.helpdeskAi.ticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateTicketRequest {
    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Description is required")
    private String description;

    private String priority;  // Optional — AI will predict if not provided
    private String category;  // Optional — AI will classify if not provided
    private String channel = "PORTAL";
    private UUID departmentId;
    private UUID workspaceId;
    private UUID assignedAgentId;
    private UUID assignedTeamId;
    private String tags;

    // For public portal submissions (no auth)
    @Email
    private String requesterEmail;
    private String requesterName;
}
