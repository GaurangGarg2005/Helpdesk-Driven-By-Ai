package com.helpdeskAi.ticket.repository;

import com.helpdeskAi.ticket.model.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @Query(value = "SELECT t FROM Ticket t WHERE t.organizationId = :orgId ORDER BY t.createdAt DESC",
           countQuery = "SELECT COUNT(t) FROM Ticket t WHERE t.organizationId = :orgId")
    Page<Ticket> findByOrganizationIdOrderByCreatedAtDesc(@Param("orgId") UUID orgId, Pageable pageable);

    Page<Ticket> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
            UUID organizationId, String status, Pageable pageable);

    Page<Ticket> findByOrganizationIdAndAssignedAgentIdOrderByCreatedAtDesc(
            UUID organizationId, UUID agentId, Pageable pageable);

    Page<Ticket> findByOrganizationIdAndPriorityOrderByCreatedAtDesc(
            UUID organizationId, String priority, Pageable pageable);

    Page<Ticket> findByOrganizationIdAndCategoryOrderByCreatedAtDesc(
            UUID organizationId, String category, Pageable pageable);

    Optional<Ticket> findByOrganizationIdAndTicketNumber(UUID organizationId, Long ticketNumber);

    @Query("SELECT COALESCE(MAX(t.ticketNumber), 0) + 1 FROM Ticket t WHERE t.organizationId = :orgId")
    Long getNextTicketNumber(@Param("orgId") UUID organizationId);

    long countByOrganizationIdAndStatus(UUID organizationId, String status);

    /** Customer portal: list tickets belonging to a specific requester. */
    Page<Ticket> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId, Pageable pageable);

    /** For public portal tracking by email. */
    Page<Ticket> findByRequesterEmailOrderByCreatedAtDesc(String email, Pageable pageable);

    /**
     * Auto-close scheduler: find AI-handled tickets where the customer hasn't replied
     * in the past 2 days (or tickets created >2 days ago with no customer reply yet).
     */
    @Query("""
        SELECT t FROM Ticket t WHERE
            t.aiHandlingActive = true AND
            t.status NOT IN ('CLOSED', 'RESOLVED', 'NEEDS_AGENT_REVIEW') AND
            (
                (t.lastCustomerReplyAt IS NOT NULL AND t.lastCustomerReplyAt < :cutoff) OR
                (t.lastCustomerReplyAt IS NULL AND t.createdAt < :cutoff)
            )
        """)
    List<Ticket> findTicketsToAutoClose(@Param("cutoff") Instant cutoff);
}
