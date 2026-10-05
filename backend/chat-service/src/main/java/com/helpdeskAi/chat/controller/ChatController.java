package com.helpdeskAi.chat.controller;

import com.helpdeskAi.chat.model.ChatMessage;
import com.helpdeskAi.chat.model.ChatSession;
import com.helpdeskAi.chat.repository.ChatMessageRepository;
import com.helpdeskAi.chat.repository.ChatSessionRepository;
import com.helpdeskAi.common.dto.ApiResponse;
import com.helpdeskAi.common.security.TenantContext;
import com.helpdeskAi.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatSessionRepository  sessionRepo;
    private final ChatMessageRepository  messageRepo;
    private final SimpMessagingTemplate  messagingTemplate;

    // ── REST: Session management ───────────────────────────────────────

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<ChatSession>> startSession(@RequestBody Map<String, String> body) {
        UUID orgId = UUID.fromString(body.getOrDefault("orgId", "00000000-0000-0000-0000-000000000000"));
        ChatSession session = ChatSession.builder()
                .organizationId(orgId)
                .visitorName(body.get("visitorName"))
                .visitorEmail(body.get("visitorEmail"))
                .visitorId(body.getOrDefault("visitorId", UUID.randomUUID().toString()))
                .channel(body.getOrDefault("channel", "WEB"))
                .status("QUEUED")
                .build();
        session = sessionRepo.save(session);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Chat session started", session, Instant.now()));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Page<ChatSession>>> listSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        UUID orgId = TenantContext.getTenantId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Sessions retrieved",
                sessionRepo.findByOrganizationIdOrderByCreatedAtDesc(orgId, PageRequest.of(page, size)),
                Instant.now()));
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getMessages(@PathVariable UUID id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Messages retrieved",
                messageRepo.findBySessionIdOrderByCreatedAtAsc(id), Instant.now()));
    }

    @PostMapping("/sessions/{id}/assign")
    public ResponseEntity<ApiResponse<ChatSession>> assignAgent(
            @PathVariable UUID id) {
        UserPrincipal user = currentUser();
        ChatSession session = sessionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setAssignedAgentId(user.getId());
        session.setAgentName(user.getUsername());
        session.setStatus("ACTIVE");
        session = sessionRepo.save(session);

        // Notify client via WebSocket
        messagingTemplate.convertAndSend("/topic/chat/" + id, Map.of(
                "type", "AGENT_JOINED",
                "agentName", user.getUsername()
        ));
        return ResponseEntity.ok(new ApiResponse<>(true, "Agent assigned", session, Instant.now()));
    }

    @PostMapping("/sessions/{id}/close")
    public ResponseEntity<ApiResponse<ChatSession>> closeSession(@PathVariable UUID id) {
        ChatSession session = sessionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setStatus("CLOSED");
        session = sessionRepo.save(session);

        messagingTemplate.convertAndSend("/topic/chat/" + id, Map.of("type", "SESSION_CLOSED"));
        return ResponseEntity.ok(new ApiResponse<>(true, "Session closed", session, Instant.now()));
    }

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<List<ChatSession>>> getQueue() {
        UUID orgId = TenantContext.getTenantId();
        List<ChatSession> queued = sessionRepo.findByOrganizationIdAndStatusOrderByCreatedAtAsc(orgId, "QUEUED");
        return ResponseEntity.ok(new ApiResponse<>(true, "Queue retrieved", queued, Instant.now()));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        UUID orgId = TenantContext.getTenantId();
        long queued = sessionRepo.countByOrganizationIdAndStatus(orgId, "QUEUED");
        long active = sessionRepo.countByOrganizationIdAndStatus(orgId, "ACTIVE");
        return ResponseEntity.ok(new ApiResponse<>(true, "Chat stats",
                Map.of("queued", queued, "active", active), Instant.now()));
    }

    // ── WebSocket: Real-time messaging ────────────────────────────────

    /**
     * Client sends to /app/chat/{sessionId}/send
     * Message is broadcast to /topic/chat/{sessionId}
     */
    @MessageMapping("/chat/{sessionId}/send")
    public void handleMessage(@DestinationVariable String sessionId, @Payload Map<String, String> payload) {
        UUID sessId = UUID.fromString(sessionId);

        // Determine sender context — could be CUSTOMER or AGENT
        String body       = payload.getOrDefault("body", "");
        String senderName = payload.getOrDefault("senderName", "Anonymous");
        String senderType = payload.getOrDefault("senderType", "CUSTOMER");
        String senderId   = payload.getOrDefault("senderId", UUID.randomUUID().toString());

        if (body.isBlank()) return;

        ChatMessage msg = ChatMessage.builder()
                .sessionId(sessId)
                .body(body)
                .senderId(senderId)
                .senderName(senderName)
                .senderType(senderType)
                .isRead(false)
                .build();

        // Persist (need orgId — fetched from session)
        sessionRepo.findById(sessId).ifPresent(s -> {
            msg.setOrganizationId(s.getOrganizationId());
            messageRepo.save(msg);
        });

        // Broadcast to all subscribers of this chat room
        messagingTemplate.convertAndSend("/topic/chat/" + sessionId, Map.of(
                "type",       "MESSAGE",
                "id",         msg.getId() != null ? msg.getId().toString() : "",
                "body",       body,
                "senderName", senderName,
                "senderType", senderType,
                "sentAt",     Instant.now().toString()
        ));
    }

    /**
     * Typing indicator: /app/chat/{sessionId}/typing
     */
    @MessageMapping("/chat/{sessionId}/typing")
    public void handleTyping(@DestinationVariable String sessionId, @Payload Map<String, String> payload) {
        messagingTemplate.convertAndSend("/topic/chat/" + sessionId, Map.of(
                "type",       "TYPING",
                "senderName", payload.getOrDefault("senderName", ""),
                "senderType", payload.getOrDefault("senderType", "CUSTOMER")
        ));
    }

    private UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) auth.getPrincipal();
    }
}
