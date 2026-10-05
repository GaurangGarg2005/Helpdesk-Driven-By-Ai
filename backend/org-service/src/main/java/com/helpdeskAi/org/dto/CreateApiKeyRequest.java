package com.helpdeskAi.org.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateApiKeyRequest {
    private String name;
    private List<String> scopes;
}
