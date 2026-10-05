package com.helpdeskAi.ticket.scheduler;

import com.helpdeskAi.ticket.model.Ticket;
import com.helpdeskAi.ticket.model.TicketMessage;
import com.helpdeskAi.ticket.repository.TicketMessageRepository;
import com.helpdeskAi.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job that auto-closes tickets where the customer has not replied
 * for 2 days after the last message in the thread.
 *
 * Runs every 30 minutes. Only targets tickets where:
 *   - aiHandlingActive is true (not already escalated or taken over by an agent)
 *   - status is not CLOSED / RESOLVED / NEEDS_AGENT_REVIEW
 *   - lastCustomerReplyAt (or createdAt) is older than 2 days
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketAutoCloseScheduler {

    private static final int INACTIVITY_DAYS = 2;

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;

    @Scheduled(fixedDelay = 30 * 60 * 1000) // every 30 minutes
    @Transactional
    public void autoCloseInactiveTickets() {
        Instant cutoff = Instant.now().minus(INACTIVITY_DAYS, ChronoUnit.DAYS);
        List<Ticket> staleTickets = ticketRepository.findTicketsToAutoClose(cutoff);

        if (staleTickets.isEmpty()) {
            log.debug("Auto-close scheduler: no stale tickets found");
            return;
        }

        log.info("Auto-close scheduler: closing {} stale ticket(s)", staleTickets.size());

        for (Ticket ticket : staleTickets) {
            try {
                // Send farewell AI message
                TicketMessage farewell = TicketMessage.builder()
                        .ticketId(ticket.getId())
                        .senderType("AI")
                        .senderName("AI Assistant")
                        .body("We haven't heard back from you in " + INACTIVITY_DAYS + " days, " +
                              "so we're going to close this ticket. " +
                              "If your issue hasn't been resolved, please don't hesitate to open a new ticket " +
                              "and we'll be happy to help. Thank you for reaching out!")
                        .isInternalNote(false)
                        .build();
                messageRepository.save(farewell);

                ticket.setStatus("CLOSED");
                ticket.setClosedAt(Instant.now());
                ticket.setAiHandlingActive(false);
                ticketRepository.save(ticket);

                log.info("Auto-closed ticket #{} (org: {}) — {} days of inactivity",
                        ticket.getTicketNumber(), ticket.getOrganizationId(), INACTIVITY_DAYS);
            } catch (Exception e) {
                log.error("Failed to auto-close ticket {}: {}", ticket.getId(), e.getMessage());
            }
        }
    }
}
