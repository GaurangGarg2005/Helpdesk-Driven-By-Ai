package com.helpdeskAi.ticket.repository;

import com.helpdeskAi.ticket.model.TicketFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketFeedbackRepository extends JpaRepository<TicketFeedback, UUID> {
    Optional<TicketFeedback> findByTicketId(UUID ticketId);
}
