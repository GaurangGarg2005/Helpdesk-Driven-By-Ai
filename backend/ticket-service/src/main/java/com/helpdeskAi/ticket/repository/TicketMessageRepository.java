package com.helpdeskAi.ticket.repository;

import com.helpdeskAi.ticket.model.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {
    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
    List<TicketMessage> findByTicketIdAndIsInternalNoteFalseOrderByCreatedAtAsc(UUID ticketId);
}
