package com.helpdeskAi.ticket.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "ticket_feedback")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TicketFeedback extends TenantAwareEntity {

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(nullable = false)
    private Integer rating;  // 1-5

    @Column(columnDefinition = "TEXT")
    private String comment;
}
