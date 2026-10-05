package com.helpdeskAi.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String role;
    private UUID organizationId;
    private String organizationName;
    private String organizationSlug;
    private List<String> permissions;
}
