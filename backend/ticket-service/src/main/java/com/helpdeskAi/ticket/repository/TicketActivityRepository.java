package com.helpdeskAi.ticket.repository;

import com.helpdeskAi.ticket.model.TicketActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketActivityRepository extends JpaRepository<TicketActivity, UUID> {
    List<TicketActivity> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);
}
