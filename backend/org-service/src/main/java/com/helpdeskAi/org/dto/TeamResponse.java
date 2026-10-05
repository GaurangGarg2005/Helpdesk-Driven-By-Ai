package com.helpdeskAi.org.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TeamResponse {
    private UUID id;
    private UUID organizationId;
    private UUID workspaceId;
    private String name;
    private String description;
    private int memberCount;
    private Instant createdAt;
}
