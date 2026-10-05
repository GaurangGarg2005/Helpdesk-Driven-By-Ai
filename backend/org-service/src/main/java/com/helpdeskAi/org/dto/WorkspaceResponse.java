package com.helpdeskAi.org.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkspaceResponse {
    private UUID id;
    private UUID organizationId;
    private String name;
    private String description;
    private Instant createdAt;
}
