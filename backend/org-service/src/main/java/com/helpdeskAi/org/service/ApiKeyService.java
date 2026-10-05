package com.helpdeskAi.org.service;

import com.helpdeskAi.org.dto.ApiKeyCreatedResponse;
import com.helpdeskAi.org.dto.ApiKeyResponse;
import com.helpdeskAi.org.dto.CreateApiKeyRequest;
import com.helpdeskAi.org.model.ApiKey;
import com.helpdeskAi.org.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ApiKeyCreatedResponse createApiKey(UUID organizationId, UUID userId, CreateApiKeyRequest req) {
        String rawKey = generateKey();
        String prefix = rawKey.substring(0, 8);
        String hash = hashKey(rawKey);

        ApiKey apiKey = ApiKey.builder()
                .organizationId(organizationId)
                .name(req.getName())
                .keyPrefix(prefix)
                .keyHash(hash)
                .scopes(req.getScopes() != null ? String.join(",", req.getScopes()) : "")
                .createdBy(userId)
                .isActive(true)
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        return new ApiKeyCreatedResponse(mapToDto(apiKey), rawKey);
    }

    public List<ApiKeyResponse> listApiKeys(UUID organizationId) {
        return apiKeyRepository.findAll().stream()
                .filter(k -> k.getOrganizationId().equals(organizationId))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteApiKey(UUID organizationId, UUID keyId) {
        apiKeyRepository.findById(keyId)
                .filter(k -> k.getOrganizationId().equals(organizationId))
                .ifPresent(apiKeyRepository::delete);
    }

    private String generateKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "hk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }

    private ApiKeyResponse mapToDto(ApiKey key) {
        ApiKeyResponse dto = new ApiKeyResponse();
        dto.setId(key.getId());
        dto.setName(key.getName());
        dto.setKeyPrefix(key.getKeyPrefix());
        dto.setScopes(key.getScopes());
        dto.setExpiresAt(key.getExpiresAt());
        dto.setIsActive(key.getIsActive());
        dto.setCreatedAt(key.getCreatedAt());
        dto.setLastUsedAt(key.getLastUsedAt());
        return dto;
    }
}
