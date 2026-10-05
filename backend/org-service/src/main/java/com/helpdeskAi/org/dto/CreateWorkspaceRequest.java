package com.helpdeskAi.org.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateWorkspaceRequest {
    @NotBlank(message = "Workspace name is required")
    private String name;
    private String description;
}
