package com.helpdeskAi.kb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCategoryRequest {
    @NotBlank
    private String name;
    private String description;
    private String iconName;
    private Integer sortOrder = 0;
    private Boolean isPublic = true;
}
