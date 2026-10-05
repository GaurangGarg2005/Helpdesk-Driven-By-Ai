package com.helpdeskAi.kb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CategoryResponse {
    private UUID    id;
    private UUID    organizationId;
    private String  name;
    private String  description;
    private String  iconName;
    private Integer sortOrder;
    private Boolean isPublic;
    private long    articleCount;
    private Instant createdAt;
}
