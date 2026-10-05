package com.helpdeskAi.org.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiKeyCreatedResponse {
    private ApiKeyResponse apiKey;
    private String plainTextKey; // Only returned once upon creation!
}
