package com.helpdeskAi.ticket.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateTicketRequest {
    private String subject;
    private String description;
    private String status;
    private String priority;
    private String category;
    private UUID assignedAgentId;
    private UUID assignedTeamId;
    private UUID departmentId;
    private String tags;
}
