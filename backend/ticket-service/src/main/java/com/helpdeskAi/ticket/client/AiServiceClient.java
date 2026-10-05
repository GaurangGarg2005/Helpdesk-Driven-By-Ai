package com.helpdeskAi.ticket.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * HTTP client for communicating with the FastAPI AI service.
 * Covers: classification, priority, sentiment, reply generation,
 * summarization, RAG ingestion/search, and satisfaction detection.
 */
@Slf4j
@Component
public class AiServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalKey;

    public AiServiceClient(
            @Value("${ai-service.base-url}") String baseUrl,
            @Value("${ai-service.internal-key:helpdesk-ai-internal-key-change-me}") String internalKey) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Internal-Key", internalKey);
        return h;
    }

    /** Classify a ticket into a category. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> classifyTicket(String subject, String description) {
        try {
            Map<String, String> body = Map.of("subject", subject, "description", description);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/classify", new HttpEntity<>(body, headers()), Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (RestClientException e) {
            log.warn("AI classification failed, using defaults: {}", e.getMessage());
        }
        return Map.of("category", "general", "confidence", 0.5);
    }

    /** Predict ticket priority. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> predictPriority(String subject, String description, String category) {
        try {
            Map<String, String> body = Map.of(
                    "subject", subject,
                    "description", description,
                    "category", category != null ? category : "");
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/predict-priority", new HttpEntity<>(body, headers()), Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (RestClientException e) {
            log.warn("AI priority prediction failed, using MEDIUM: {}", e.getMessage());
        }
        return Map.of("priority", "MEDIUM", "confidence", 0.5);
    }

    /** Analyze sentiment of text. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeSentiment(String text) {
        try {
            Map<String, String> body = Map.of("text", text);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/sentiment", new HttpEntity<>(body, headers()), Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (RestClientException e) {
            log.warn("AI sentiment analysis failed: {}", e.getMessage());
        }
        return Map.of("sentiment", "NEUTRAL", "score", 0.0);
    }

    /** Generate AI reply suggestion. Returns {suggested_reply, confidence, tone}. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> suggestReply(String subject, String description,
                                            List<Map<String, String>> history, String kbContext) {
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("ticket_subject", subject);
            body.put("ticket_description", description);
            body.put("conversation_history", history != null ? history : List.of());
            body.put("kb_context", kbContext);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/suggest-reply", new HttpEntity<>(body, headers()), Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (RestClientException e) {
            log.warn("AI reply suggestion failed: {}", e.getMessage());
        }
        return Map.of("suggested_reply", "", "confidence", 0.0);
    }

    /** Summarize a ticket thread. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> summarizeThread(List<Map<String, String>> messages) {
        try {
            Map<String, Object> body = Map.of("messages", messages);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/summarize", new HttpEntity<>(body, headers()), Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (RestClientException e) {
            log.warn("AI summarization failed: {}", e.getMessage());
        }
        return Map.of("summary", "No summary available", "key_points", List.of());
    }

    // ── RAG Methods ────────────────────────────────────────────────────────────

    /**
     * Ingest a company document into the RAG vector store.
     * Called by KB service when admin uploads a document.
     */
    public void ingestDocument(String orgId, String docId, String title, String content) {
        try {
            Map<String, String> body = Map.of(
                    "org_id", orgId,
                    "doc_id", docId,
                    "title", title,
                    "content", content);
            restTemplate.postForEntity(
                    baseUrl + "/rag/ingest", new HttpEntity<>(body, headers()), Map.class);
            log.info("RAG ingest: org={} doc={}", orgId, docId);
        } catch (RestClientException e) {
            log.warn("RAG ingest failed for doc {}: {}", docId, e.getMessage());
        }
    }

    /**
     * Remove a document from the RAG vector store.
     */
    public void deleteRagDocument(String orgId, String docId) {
        try {
            Map<String, String> body = Map.of("org_id", orgId, "doc_id", docId);
            restTemplate.postForEntity(
                    baseUrl + "/rag/delete", new HttpEntity<>(body, headers()), Map.class);
            log.info("RAG delete: org={} doc={}", orgId, docId);
        } catch (RestClientException e) {
            log.warn("RAG delete failed for doc {}: {}", docId, e.getMessage());
        }
    }

    /**
     * Retrieve relevant context from the company's RAG store for a given query.
     * Returns the top matching chunks joined as a single string for kb_context injection.
     */
    @SuppressWarnings("unchecked")
    public String getRagContext(String orgId, String query) {
        try {
            Map<String, Object> body = Map.of("org_id", orgId, "query", query, "top_k", 3);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/rag/search", new HttpEntity<>(body, headers()), Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object chunks = response.getBody().get("chunks");
                if (chunks instanceof List<?> list && !list.isEmpty()) {
                    return String.join("\n\n", list.stream()
                            .map(Object::toString).toList());
                }
            }
        } catch (RestClientException e) {
            log.warn("RAG search failed for org {}: {}", orgId, e.getMessage());
        }
        return null;
    }

    // ── Satisfaction Detection ─────────────────────────────────────────────────

    /**
     * Check if a customer message indicates they are satisfied / issue is resolved.
     * Returns {satisfied: bool, confidence: float}.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkSatisfaction(String message) {
        try {
            Map<String, String> body = Map.of("message", message);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/check-satisfaction", new HttpEntity<>(body, headers()), Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (RestClientException e) {
            log.warn("Satisfaction check failed: {}", e.getMessage());
        }
        return Map.of("satisfied", false, "confidence", 0.0);
    }
}
