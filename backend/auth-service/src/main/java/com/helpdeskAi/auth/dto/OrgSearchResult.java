package com.helpdeskAi.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class OrgSearchResult {
    private UUID id;
    private String name;
    private String slug;
}
