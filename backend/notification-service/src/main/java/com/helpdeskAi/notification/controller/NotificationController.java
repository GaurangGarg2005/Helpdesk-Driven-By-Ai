package com.helpdeskAi.notification.controller;

import com.helpdeskAi.common.dto.ApiResponse;
import com.helpdeskAi.common.security.UserPrincipal;
import com.helpdeskAi.notification.dto.SendNotificationRequest;
import com.helpdeskAi.notification.model.Notification;
import com.helpdeskAi.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** Called by other services (ticket-service, chat-service) to emit a notification */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Notification>> send(@RequestBody SendNotificationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Notification sent",
                        notificationService.send(req), Instant.now()));
    }

    /** Agent fetches their own notifications */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Notification>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        UUID recipientId = currentUser().getId();
        return ok("Notifications retrieved",
                notificationService.listForUser(recipientId, PageRequest.of(page, size)));
    }

    /** Unread badge count */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        UUID recipientId = currentUser().getId();
        return ok("Unread count", Map.of("count", notificationService.countUnread(recipientId)));
    }

    /** Mark single notification read */
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable UUID id) {
        notificationService.markRead(id);
        return ok("Marked read", null);
    }

    /** Mark ALL as read */
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllRead(currentUser().getId());
        return ok("All marked read", null);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private <T> ResponseEntity<ApiResponse<T>> ok(String msg, T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, msg, data, Instant.now()));
    }

    private UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) auth.getPrincipal();
    }
}
