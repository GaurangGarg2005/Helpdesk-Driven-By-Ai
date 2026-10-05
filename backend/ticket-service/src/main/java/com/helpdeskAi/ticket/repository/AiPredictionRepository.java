package com.helpdeskAi.ticket.repository;

import com.helpdeskAi.ticket.model.AiPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiPredictionRepository extends JpaRepository<AiPrediction, UUID> {
    List<AiPrediction> findByTicketId(UUID ticketId);
}
