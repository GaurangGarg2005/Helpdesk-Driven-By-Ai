package com.helpdeskAi.org.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateTeamRequest {
    @NotBlank(message = "Team name is required")
    private String name;
    private String description;
    private UUID workspaceId;
}
