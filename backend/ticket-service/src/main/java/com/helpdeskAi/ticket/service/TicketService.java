package com.helpdeskAi.ticket.service;

import com.helpdeskAi.common.exception.ResourceNotFoundException;
import com.helpdeskAi.common.security.TenantContext;
import com.helpdeskAi.ticket.client.AiServiceClient;
import com.helpdeskAi.ticket.dto.*;
import com.helpdeskAi.ticket.model.*;
import com.helpdeskAi.ticket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final TicketActivityRepository activityRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final AiPredictionRepository aiPredictionRepository;
    private final TicketFeedbackRepository feedbackRepository;
    private final AiServiceClient aiServiceClient;

    // Keywords that indicate the customer wants to escalate to a human agent
    private static final List<String> ESCALATION_KEYWORDS = List.of(
            // Direct agent requests
            "talk to the agent", "talk to agent", "speak to the agent", "speak to agent",
            "talk to a human", "speak to a human", "speak to an agent",
            "talk to a person", "talk to someone", "want to talk to",
            "want to speak to", "want to speak with", "need an agent",
            "need a human", "connect me to", "transfer me to", "transfer to agent",
            // Frustration/escalation signals
            "real person", "human support", "actual person", "live agent", "human agent",
            "escalate", "supervisor", "manager", "not helpful", "not helping",
            "this is not working", "speak to someone", "customer service representative"
    );

    // ── Ticket Creation ──────────────────────────────────────────────────────

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, UUID requesterId) {
        UUID orgId = TenantContext.getTenantId();
        Long ticketNumber = ticketRepository.getNextTicketNumber(orgId);

        Map<String, Object> classification = aiServiceClient.classifyTicket(
                request.getSubject(), request.getDescription());
        String aiCategory = (String) classification.getOrDefault("category", "general");
        double aiCategoryConfidence = ((Number) classification.getOrDefault("confidence", 0.5)).doubleValue();

        Map<String, Object> priorityPrediction = aiServiceClient.predictPriority(
                request.getSubject(), request.getDescription(), aiCategory);
        String aiPriority = (String) priorityPrediction.getOrDefault("priority", "MEDIUM");
        double aiPriorityConfidence = ((Number) priorityPrediction.getOrDefault("confidence", 0.5)).doubleValue();

        Map<String, Object> sentimentResult = aiServiceClient.analyzeSentiment(
                request.getSubject() + " " + request.getDescription());
        String sentiment = (String) sentimentResult.getOrDefault("sentiment", "NEUTRAL");
        double sentimentScore = ((Number) sentimentResult.getOrDefault("score", 0.0)).doubleValue();

        String finalCategory = request.getCategory() != null ? request.getCategory() : aiCategory;
        String finalPriority = request.getPriority() != null ? request.getPriority() : aiPriority;

        Instant now = Instant.now();
        SlaPolicy slaPolicy = slaPolicyRepository
                .findByOrganizationIdAndPriorityAndIsDefaultTrue(orgId, finalPriority)
                .orElse(null);

        Ticket ticket = Ticket.builder()
                .organizationId(orgId)
                .ticketNumber(ticketNumber)
                .subject(request.getSubject())
                .description(request.getDescription())
                .status("OPEN")
                .priority(finalPriority)
                .category(finalCategory)
                .channel(request.getChannel() != null ? request.getChannel() : "PORTAL")
                .sentiment(sentiment)
                .sentimentScore(BigDecimal.valueOf(sentimentScore))
                .departmentId(request.getDepartmentId())
                .workspaceId(request.getWorkspaceId())
                .assignedAgentId(request.getAssignedAgentId())
                .assignedTeamId(request.getAssignedTeamId())
                .requesterId(requesterId)
                .requesterEmail(request.getRequesterEmail())
                .requesterName(request.getRequesterName())
                .slaPolicyId(slaPolicy != null ? slaPolicy.getId() : null)
                .firstResponseDueAt(slaPolicy != null ? now.plus(slaPolicy.getFirstResponseHours(), ChronoUnit.HOURS) : null)
                .resolutionDueAt(slaPolicy != null ? now.plus(slaPolicy.getResolutionHours(), ChronoUnit.HOURS) : null)
                .tags(request.getTags())
                .aiHandlingActive(true)
                .humanEscalationCount(0)
                .build();
        ticket = ticketRepository.save(ticket);

        saveAiPrediction(ticket, "CATEGORY", aiCategory, aiCategoryConfidence);
        saveAiPrediction(ticket, "PRIORITY", aiPriority, aiPriorityConfidence);
        saveAiPrediction(ticket, "SENTIMENT", sentiment, sentimentScore);
        recordActivity(ticket.getId(), requesterId, "CREATED", null, "OPEN",
                "Ticket created via " + ticket.getChannel());

        // ── Save the customer's initial message so it shows in the conversation thread ──
        // Both the agent view and the customer portal fetch ticket_messages; without this
        // the customer's original query (ticket description) would be invisible there.
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            TicketMessage initialMsg = TicketMessage.builder()
                    .ticketId(ticket.getId())
                    .senderId(requesterId)
                    .senderType("CUSTOMER")
                    .senderName(request.getRequesterName() != null ? request.getRequesterName() : "Customer")
                    .senderEmail(request.getRequesterEmail())
                    .body(request.getDescription())
                    .isInternalNote(false)
                    .build();
            messageRepository.save(initialMsg);
        }

        log.info("Created ticket #{} for org {} — AI: category={}, priority={}, sentiment={}",
                ticketNumber, orgId, aiCategory, aiPriority, sentiment);

        // Auto AI reply — async so it doesn't block the creation response
        triggerAiAutoReply(ticket);

        return toResponse(ticket, null, null);
    }

    @Transactional
    public TicketResponse createPublicTicket(CreateTicketRequest request, UUID orgId) {
        TenantContext.setTenantId(orgId);
        try {
            Long ticketNumber = ticketRepository.getNextTicketNumber(orgId);

            Map<String, Object> classification = aiServiceClient.classifyTicket(
                    request.getSubject(), request.getDescription());
            String aiCategory = (String) classification.getOrDefault("category", "general");

            Map<String, Object> priorityPrediction = aiServiceClient.predictPriority(
                    request.getSubject(), request.getDescription(), aiCategory);
            String aiPriority = (String) priorityPrediction.getOrDefault("priority", "MEDIUM");

            Map<String, Object> sentimentResult = aiServiceClient.analyzeSentiment(
                    request.getSubject() + " " + request.getDescription());
            String sentiment = (String) sentimentResult.getOrDefault("sentiment", "NEUTRAL");

            Ticket ticket = Ticket.builder()
                    .organizationId(orgId)
                    .ticketNumber(ticketNumber)
                    .subject(request.getSubject())
                    .description(request.getDescription())
                    .status("OPEN")
                    .priority(aiPriority)
                    .category(aiCategory)
                    .channel("PORTAL")
                    .sentiment(sentiment)
                    .requesterEmail(request.getRequesterEmail())
                    .requesterName(request.getRequesterName())
                    .aiHandlingActive(true)
                    .humanEscalationCount(0)
                    .build();
            ticket = ticketRepository.save(ticket);
            triggerAiAutoReply(ticket);
            return toResponse(ticket, null, null);
        } finally {
            TenantContext.clear();
        }
    }

    // ── Auto Reply (Async) ────────────────────────────────────────────────────

    /**
     * Triggered immediately after ticket creation and after every customer message.
     * Fetches RAG context for the org, then calls the AI reply service.
     * Saves the result as an AI message on the ticket.
     */
    @Async
    @Transactional
    public void triggerAiAutoReply(Ticket ticket) {
        try {
            List<TicketMessage> existingMessages =
                    messageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId());
            List<Map<String, String>> history = existingMessages.stream()
                    .filter(m -> !Boolean.TRUE.equals(m.getIsInternalNote()))
                    .map(m -> Map.of("sender", m.getSenderType(), "message", m.getBody() != null ? m.getBody() : ""))
                    .collect(Collectors.toList());

            // Fetch company-specific RAG context
            String ragContext = null;
            if (ticket.getOrganizationId() != null) {
                ragContext = aiServiceClient.getRagContext(
                        ticket.getOrganizationId().toString(),
                        ticket.getSubject() + " " + ticket.getDescription());
            }

            Map<String, Object> result = aiServiceClient.suggestReply(
                    ticket.getSubject(), ticket.getDescription(), history, ragContext);
            String aiReply = (String) result.getOrDefault("suggested_reply", "");

            if (aiReply == null || aiReply.isBlank()) {
                log.warn("AI returned empty reply for ticket {}", ticket.getId());
                return;
            }

            TicketMessage aiMessage = TicketMessage.builder()
                    .ticketId(ticket.getId())
                    .senderType("AI")
                    .senderName("AI Assistant")
                    .body(aiReply)
                    .isInternalNote(false)
                    .build();
            messageRepository.save(aiMessage);

            // AI reply: change status OPEN → WAITING_ON_CUSTOMER
            Ticket ticketToUpdate = ticketRepository.findById(ticket.getId()).orElse(ticket);
            if ("OPEN".equals(ticketToUpdate.getStatus())) {
                ticketToUpdate.setStatus("WAITING_ON_CUSTOMER");
                ticketRepository.save(ticketToUpdate);
                log.info("Ticket #{} → WAITING_ON_CUSTOMER (AI replied)", ticket.getTicketNumber());
            }

            log.info("AI auto-reply saved for ticket #{}", ticket.getTicketNumber());
        } catch (Exception e) {
            log.error("Failed to generate AI auto-reply for ticket {}: {}", ticket.getId(), e.getMessage());
        }
    }

    // ── Ticket Listing ────────────────────────────────────────────────────────

    public Page<TicketResponse> listTickets(String status, String priority, String category,
                                            UUID assignedAgentId, Pageable pageable) {
        UUID orgId = TenantContext.getTenantId();
        Page<Ticket> tickets;
        if (status != null) {
            tickets = ticketRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(orgId, status, pageable);
        } else if (assignedAgentId != null) {
            tickets = ticketRepository.findByOrganizationIdAndAssignedAgentIdOrderByCreatedAtDesc(orgId, assignedAgentId, pageable);
        } else if (priority != null) {
            tickets = ticketRepository.findByOrganizationIdAndPriorityOrderByCreatedAtDesc(orgId, priority, pageable);
        } else if (category != null) {
            tickets = ticketRepository.findByOrganizationIdAndCategoryOrderByCreatedAtDesc(orgId, category, pageable);
        } else {
            tickets = ticketRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId, pageable);
        }
        return tickets.map(t -> toResponse(t, null, null));
    }

    /** Customer-only: list tickets belonging to this specific requester. */
    public Page<TicketResponse> listMyTickets(UUID requesterId, Pageable pageable) {
        return ticketRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId, pageable)
                .map(t -> toResponse(t, null, null));
    }

    public TicketResponse getTicket(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        List<TicketMessageResponse> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream().map(this::toMessageResponse).collect(Collectors.toList());
        List<TicketActivityResponse> activities = activityRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
                .stream().map(this::toActivityResponse).collect(Collectors.toList());
        return toResponse(ticket, messages, activities);
    }

    // ── Ticket Update ─────────────────────────────────────────────────────────

    @Transactional
    public TicketResponse updateTicket(UUID ticketId, UpdateTicketRequest request, UUID userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        if (request.getStatus() != null && !request.getStatus().equals(ticket.getStatus())) {
            recordActivity(ticketId, userId, "STATUS_CHANGED", ticket.getStatus(), request.getStatus(), null);
            ticket.setStatus(request.getStatus());
            if ("RESOLVED".equals(request.getStatus())) {
                ticket.setResolvedAt(Instant.now());
                ticket.setAiHandlingActive(false);
                sendSystemMessage(ticket, "This ticket has been marked as resolved by a support agent. " +
                        "If you need further assistance, please open a new ticket.");
            } else if ("CLOSED".equals(request.getStatus())) {
                ticket.setClosedAt(Instant.now());
                ticket.setAiHandlingActive(false);
            }
        }
        if (request.getPriority() != null && !request.getPriority().equals(ticket.getPriority())) {
            recordActivity(ticketId, userId, "PRIORITY_CHANGED", ticket.getPriority(), request.getPriority(), null);
            ticket.setPriority(request.getPriority());
        }
        if (request.getCategory() != null && !request.getCategory().equals(ticket.getCategory())) {
            recordActivity(ticketId, userId, "CATEGORY_CHANGED", ticket.getCategory(), request.getCategory(), null);
            ticket.setCategory(request.getCategory());
        }
        if (request.getAssignedAgentId() != null) {
            recordActivity(ticketId, userId, "ASSIGNED",
                    ticket.getAssignedAgentId() != null ? ticket.getAssignedAgentId().toString() : null,
                    request.getAssignedAgentId().toString(), null);
            ticket.setAssignedAgentId(request.getAssignedAgentId());
        }
        if (request.getAssignedTeamId() != null) ticket.setAssignedTeamId(request.getAssignedTeamId());
        if (request.getDepartmentId() != null) ticket.setDepartmentId(request.getDepartmentId());
        if (request.getSubject() != null) ticket.setSubject(request.getSubject());
        if (request.getDescription() != null) ticket.setDescription(request.getDescription());
        if (request.getTags() != null) ticket.setTags(request.getTags());

        ticket = ticketRepository.save(ticket);
        return toResponse(ticket, null, null);
    }

    // ── Message Handling ──────────────────────────────────────────────────────

    @Transactional
    public TicketMessageResponse addMessage(UUID ticketId, CreateMessageRequest request,
                                            UUID senderId, String senderName, String senderEmail, String senderType) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        TicketMessage message = TicketMessage.builder()
                .ticketId(ticketId)
                .senderId(senderId)
                .senderType(senderType)
                .senderName(senderName)
                .senderEmail(senderEmail)
                .body(request.getBody())
                .bodyHtml(request.getBodyHtml())
                .isInternalNote(request.isInternalNote())
                .attachments(request.getAttachments())
                .build();
        message = messageRepository.save(message);

        // Track first agent response time
        if ("AGENT".equals(senderType) || "ADMIN".equals(senderType) || "OWNER".equals(senderType)) {
            if (ticket.getFirstResponseAt() == null) {
                ticket.setFirstResponseAt(Instant.now());
                if (ticket.getFirstResponseDueAt() != null &&
                        Instant.now().isAfter(ticket.getFirstResponseDueAt())) {
                    ticket.setSlaFirstResponseBreached(true);
                }
            }
            // Agent/Admin reply: OPEN → WAITING_ON_CUSTOMER
            if (!request.isInternalNote() && "OPEN".equals(ticket.getStatus())) {
                ticket.setStatus("WAITING_ON_CUSTOMER");
                recordActivity(ticketId, senderId, "STATUS_CHANGED", "OPEN", "WAITING_ON_CUSTOMER",
                        "Awaiting customer reply");
                log.info("Ticket #{} → WAITING_ON_CUSTOMER (agent replied)", ticket.getTicketNumber());
            }
        }

        // Customer message handling
        if ("CUSTOMER".equals(senderType) && !Boolean.TRUE.equals(request.isInternalNote())) {
            // Customer replied: WAITING_ON_CUSTOMER → OPEN
            if ("WAITING_ON_CUSTOMER".equals(ticket.getStatus())) {
                ticket.setStatus("OPEN");
                recordActivity(ticketId, senderId, "STATUS_CHANGED", "WAITING_ON_CUSTOMER", "OPEN",
                        "Customer replied");
                log.info("Ticket #{} → OPEN (customer replied)", ticket.getTicketNumber());
            }
            ticket.setLastCustomerReplyAt(Instant.now());

            // Check satisfaction
            Map<String, Object> satisfactionResult = aiServiceClient.checkSatisfaction(request.getBody());
            boolean satisfied = Boolean.TRUE.equals(satisfactionResult.get("satisfied"));
            if (satisfied) {
                ticket.setStatus("CLOSED");
                ticket.setClosedAt(Instant.now());
                ticket.setAiHandlingActive(false);
                sendSystemMessage(ticket, "Thank you! We're glad your issue has been resolved. " +
                        "This ticket has been closed. Feel free to open a new ticket if you need further help.");
                log.info("Ticket #{} auto-closed — customer satisfied", ticket.getTicketNumber());
            } else if (Boolean.TRUE.equals(ticket.getAiHandlingActive())) {
                // Check for escalation
                String bodyLower = request.getBody().toLowerCase();
                boolean hasEscalationKeyword = ESCALATION_KEYWORDS.stream()
                        .anyMatch(bodyLower::contains);

                if (hasEscalationKeyword) {
                    // Escalate to a human agent immediately on the FIRST request
                    ticket.setStatus("NEEDS_AGENT_REVIEW");
                    ticket.setAiHandlingActive(false);
                    int newCount = (ticket.getHumanEscalationCount() != null ?
                            ticket.getHumanEscalationCount() : 0) + 1;
                    ticket.setHumanEscalationCount(newCount);
                    sendSystemMessage(ticket, "I understand you'd like to speak with a support agent. " +
                            "Your request has been flagged and a member of our team will be with you shortly. " +
                            "Thank you for your patience!");
                    recordActivity(ticketId, null, "ESCALATED", null, "NEEDS_AGENT_REVIEW",
                            "Customer requested human agent");
                    log.info("Ticket #{} escalated to NEEDS_AGENT_REVIEW — customer requested agent",
                            ticket.getTicketNumber());
                }

                // Trigger AI auto-reply if still AI-handled
                if (Boolean.TRUE.equals(ticket.getAiHandlingActive())) {
                    Ticket savedTicket = ticketRepository.save(ticket);
                    triggerAiAutoReply(savedTicket);
                }
            }
        }

        ticketRepository.save(ticket);
        recordActivity(ticketId, senderId, "COMMENTED", null, null,
                request.isInternalNote() ? "Added internal note" : "Added reply");

        return toMessageResponse(message);
    }

    // ── AI Summary / Suggestions ──────────────────────────────────────────────

    public Map<String, Object> getAiSummary(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        List<TicketMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);

        List<Map<String, String>> msgList = new ArrayList<>();
        if (ticket.getDescription() != null && !ticket.getDescription().isBlank()) {
            msgList.add(Map.of("sender", ticket.getRequesterName() != null ? ticket.getRequesterName() : "Customer",
                    "message", ticket.getDescription(),
                    "timestamp", ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : ""));
        }
        messages.stream()
                .map(m -> Map.of("sender", m.getSenderType(),
                        "message", m.getBody() != null ? m.getBody() : "",
                        "timestamp", m.getCreatedAt() != null ? m.getCreatedAt().toString() : ""))
                .forEach(msgList::add);

        return aiServiceClient.summarizeThread(msgList);
    }

    public Map<String, Object> getAiReplySuggestion(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        List<TicketMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        List<Map<String, String>> history = messages.stream()
                .map(m -> Map.of("sender", m.getSenderType(), "message", m.getBody() != null ? m.getBody() : ""))
                .collect(Collectors.toList());
        String ragContext = ticket.getOrganizationId() != null ?
                aiServiceClient.getRagContext(ticket.getOrganizationId().toString(),
                        ticket.getSubject() + " " + ticket.getDescription()) : null;
        return aiServiceClient.suggestReply(ticket.getSubject(), ticket.getDescription(), history, ragContext);
    }

    // ── Dashboard Stats ───────────────────────────────────────────────────────

    public Map<String, Object> getTicketStats() {
        UUID orgId = TenantContext.getTenantId();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("open", ticketRepository.countByOrganizationIdAndStatus(orgId, "OPEN"));
        stats.put("inProgress", ticketRepository.countByOrganizationIdAndStatus(orgId, "IN_PROGRESS"));
        stats.put("waitingOnCustomer", ticketRepository.countByOrganizationIdAndStatus(orgId, "WAITING_ON_CUSTOMER"));
        stats.put("needsAgentReview", ticketRepository.countByOrganizationIdAndStatus(orgId, "NEEDS_AGENT_REVIEW"));
        stats.put("resolved", ticketRepository.countByOrganizationIdAndStatus(orgId, "RESOLVED"));
        stats.put("closed", ticketRepository.countByOrganizationIdAndStatus(orgId, "CLOSED"));
        return stats;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void sendSystemMessage(Ticket ticket, String text) {
        TicketMessage msg = TicketMessage.builder()
                .ticketId(ticket.getId())
                .senderType("AI")
                .senderName("AI Assistant")
                .body(text)
                .isInternalNote(false)
                .build();
        messageRepository.save(msg);
    }

    private void saveAiPrediction(Ticket ticket, String type, String value, double confidence) {
        AiPrediction prediction = AiPrediction.builder()
                .organizationId(ticket.getOrganizationId())
                .ticketId(ticket.getId())
                .predictionType(type)
                .predictedValue(value)
                .confidence(BigDecimal.valueOf(confidence))
                .build();
        aiPredictionRepository.save(prediction);
    }

    private void recordActivity(UUID ticketId, UUID userId, String type,
                                String oldValue, String newValue, String description) {
        TicketActivity activity = TicketActivity.builder()
                .ticketId(ticketId)
                .userId(userId)
                .activityType(type)
                .oldValue(oldValue)
                .newValue(newValue)
                .description(description)
                .build();
        activityRepository.save(activity);
    }

    private TicketResponse toResponse(Ticket t, List<TicketMessageResponse> messages,
                                      List<TicketActivityResponse> activities) {
        return TicketResponse.builder()
                .id(t.getId())
                .organizationId(t.getOrganizationId())
                .ticketNumber(t.getTicketNumber())
                .subject(t.getSubject())
                .description(t.getDescription())
                .status(t.getStatus())
                .priority(t.getPriority())
                .category(t.getCategory())
                .channel(t.getChannel())
                .sentiment(t.getSentiment())
                .sentimentScore(t.getSentimentScore())
                .aiSummary(t.getAiSummary())
                .departmentId(t.getDepartmentId())
                .workspaceId(t.getWorkspaceId())
                .assignedAgentId(t.getAssignedAgentId())
                .assignedTeamId(t.getAssignedTeamId())
                .requesterId(t.getRequesterId())
                .requesterEmail(t.getRequesterEmail())
                .requesterName(t.getRequesterName())
                .firstResponseDueAt(t.getFirstResponseDueAt())
                .resolutionDueAt(t.getResolutionDueAt())
                .firstResponseAt(t.getFirstResponseAt())
                .resolvedAt(t.getResolvedAt())
                .slaFirstResponseBreached(t.getSlaFirstResponseBreached())
                .slaResolutionBreached(t.getSlaResolutionBreached())
                .tags(t.getTags())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .aiHandlingActive(t.getAiHandlingActive())
                .humanEscalationCount(t.getHumanEscalationCount())
                .lastCustomerReplyAt(t.getLastCustomerReplyAt())
                .messages(messages)
                .activities(activities)
                .build();
    }

    private TicketMessageResponse toMessageResponse(TicketMessage m) {
        return TicketMessageResponse.builder()
                .id(m.getId())
                .ticketId(m.getTicketId())
                .senderId(m.getSenderId())
                .senderType(m.getSenderType())
                .senderName(m.getSenderName())
                .senderEmail(m.getSenderEmail())
                .body(m.getBody())
                .bodyHtml(m.getBodyHtml())
                .isInternalNote(m.getIsInternalNote())
                .attachments(m.getAttachments())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private TicketActivityResponse toActivityResponse(TicketActivity a) {
        return TicketActivityResponse.builder()
                .id(a.getId())
                .ticketId(a.getTicketId())
                .userId(a.getUserId())
                .activityType(a.getActivityType())
                .oldValue(a.getOldValue())
                .newValue(a.getNewValue())
                .description(a.getDescription())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
