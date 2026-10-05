package com.helpdeskAi.org.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddTeamMemberRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;
    private String role = "MEMBER";
}
