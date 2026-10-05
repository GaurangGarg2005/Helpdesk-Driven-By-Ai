package com.helpdeskAi.chat.repository;

import com.helpdeskAi.chat.model.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Page<ChatSession> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);
    List<ChatSession> findByOrganizationIdAndStatusOrderByCreatedAtAsc(UUID organizationId, String status);
    long countByOrganizationIdAndStatus(UUID organizationId, String status);
}
