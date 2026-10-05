package com.helpdeskAi.ticket.controller;

import com.helpdeskAi.common.dto.ApiResponse;
import com.helpdeskAi.common.security.UserPrincipal;
import com.helpdeskAi.ticket.dto.*;
import com.helpdeskAi.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // ── Agent / Admin / Owner endpoints ──────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        UserPrincipal user = getCurrentUser();
        if (request.getRequesterEmail() == null) request.setRequesterEmail(user.getEmail());
        if (request.getRequesterName() == null) request.setRequesterName(user.getUsername());
        TicketResponse ticket = ticketService.createTicket(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Ticket created with AI classification", ticket, Instant.now()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> listTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID assignedAgentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<TicketResponse> tickets = ticketService.listTickets(status, priority, category, assignedAgentId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tickets retrieved", tickets, Instant.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@PathVariable UUID id) {
        TicketResponse ticket = ticketService.getTicket(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ticket retrieved", ticket, Instant.now()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicket(
            @PathVariable UUID id, @RequestBody UpdateTicketRequest request) {
        UserPrincipal user = getCurrentUser();
        TicketResponse ticket = ticketService.updateTicket(id, request, user.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Ticket updated", ticket, Instant.now()));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> addMessage(
            @PathVariable UUID id, @Valid @RequestBody CreateMessageRequest request) {
        UserPrincipal user = getCurrentUser();
        String role = user.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("AGENT");
        // CUSTOMER messages handled separately via /my/tickets — here it's always an agent/admin
        TicketMessageResponse message = ticketService.addMessage(
                id, request, user.getId(), user.getFullName(), user.getEmail(),
                "CUSTOMER".equals(role) ? "CUSTOMER" : "AGENT");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Message added", message, Instant.now()));
    }

    @GetMapping("/{id}/ai/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAiSummary(@PathVariable UUID id) {
        Map<String, Object> summary = ticketService.getAiSummary(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "AI summary generated", summary, Instant.now()));
    }

    @GetMapping("/{id}/ai/suggestions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAiSuggestions(@PathVariable UUID id) {
        Map<String, Object> suggestion = ticketService.getAiReplySuggestion(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "AI suggestion generated", suggestion, Instant.now()));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTicketStats() {
        Map<String, Object> stats = ticketService.getTicketStats();
        return ResponseEntity.ok(new ApiResponse<>(true, "Ticket stats retrieved", stats, Instant.now()));
    }

    // ── Customer Portal Endpoints ─────────────────────────────────────────────

    /** Customer creates their own ticket — email/name auto-filled from JWT. */
    @PostMapping("/my")
    public ResponseEntity<ApiResponse<TicketResponse>> createMyTicket(@Valid @RequestBody CreateTicketRequest request) {
        UserPrincipal user = getCurrentUser();
        // Force-fill from JWT — customer should not be able to spoof these
        request.setRequesterEmail(user.getEmail());
        request.setRequesterName(user.getFullName());
        TicketResponse ticket = ticketService.createTicket(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Your ticket has been submitted. AI will respond shortly.", ticket, Instant.now()));
    }

    /** Customer lists their own tickets. */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> listMyTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserPrincipal user = getCurrentUser();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<TicketResponse> tickets = ticketService.listMyTickets(user.getId(), pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Your tickets retrieved", tickets, Instant.now()));
    }

    /** Customer replies in their own ticket. */
    @PostMapping("/my/{id}/messages")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> addMyMessage(
            @PathVariable UUID id, @Valid @RequestBody CreateMessageRequest request) {
        UserPrincipal user = getCurrentUser();
        TicketMessageResponse message = ticketService.addMessage(
                id, request, user.getId(), user.getFullName(),
                user.getEmail(), "CUSTOMER");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Reply sent", message, Instant.now()));
    }

    // ── Public Portal Endpoints (no auth) ─────────────────────────────────────

    @PostMapping("/public")
    public ResponseEntity<ApiResponse<TicketResponse>> createPublicTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @RequestParam UUID orgId) {
        TicketResponse ticket = ticketService.createPublicTicket(request, orgId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Ticket submitted successfully", ticket, Instant.now()));
    }

    private UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) auth.getPrincipal();
    }
}
