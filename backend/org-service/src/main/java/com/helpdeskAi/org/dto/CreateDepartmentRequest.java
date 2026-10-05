package com.helpdeskAi.org.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDepartmentRequest {
    @NotBlank(message = "Department name is required")
    private String name;
    private String description;
}
