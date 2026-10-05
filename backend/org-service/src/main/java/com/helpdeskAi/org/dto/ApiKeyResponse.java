package com.helpdeskAi.org.dto;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class ApiKeyResponse {
    private UUID id;
    private String name;
    private String keyPrefix;
    private String scopes;
    private Instant expiresAt;
    private Boolean isActive;
    private Instant createdAt;
    private Instant lastUsedAt;
}
