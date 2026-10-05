package com.helpdeskAi.org.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WebhookResponse {
    private UUID id;
    private UUID organizationId;
    private String url;
    private List<String> events;
    private boolean isActive;
    private String secret;
    private String description;
    private Instant createdAt;
}
