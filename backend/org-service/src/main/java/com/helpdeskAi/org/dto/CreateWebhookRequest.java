package com.helpdeskAi.org.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateWebhookRequest {
    @NotBlank(message = "Webhook URL is required")
    private String url;
    private List<String> events;
    private String description;
}
