package com.helpdeskAi.org.service;

import com.helpdeskAi.org.dto.CreateWebhookRequest;
import com.helpdeskAi.org.dto.WebhookResponse;
import com.helpdeskAi.org.model.Webhook;
import com.helpdeskAi.org.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;

    @Transactional
    public WebhookResponse createWebhook(UUID organizationId, CreateWebhookRequest request) {
        Webhook webhook = Webhook.builder()
                .organizationId(organizationId)
                .url(request.getUrl())
                .events(String.join(",", request.getEvents()))
                .secret(UUID.randomUUID().toString().replace("-", "")) // simple random secret
                .isActive(true)
                .description(request.getDescription())
                .build();
        webhook = webhookRepository.save(webhook);
        return mapToDto(webhook);
    }

    public List<WebhookResponse> listWebhooks(UUID organizationId) {
        return webhookRepository.findAll().stream()
                .filter(w -> w.getOrganizationId().equals(organizationId))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteWebhook(UUID organizationId, UUID webhookId) {
        webhookRepository.findById(webhookId)
                .filter(w -> w.getOrganizationId().equals(organizationId))
                .ifPresent(webhookRepository::delete);
    }

    private WebhookResponse mapToDto(Webhook w) {
        WebhookResponse dto = new WebhookResponse();
        dto.setId(w.getId());
        dto.setUrl(w.getUrl());
        dto.setEvents(List.of(w.getEvents().split(",")));
        dto.setSecret(w.getSecret());
        dto.setActive(w.getIsActive());
        dto.setDescription(w.getDescription());
        return dto;
    }
}
