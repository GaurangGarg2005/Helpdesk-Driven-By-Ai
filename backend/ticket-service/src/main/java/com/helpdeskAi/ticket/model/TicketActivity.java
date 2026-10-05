package com.helpdeskAi.ticket.model;

import com.helpdeskAi.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "ticket_activities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TicketActivity extends BaseEntity {

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;  // STATUS_CHANGED, ASSIGNED, PRIORITY_CHANGED, CATEGORY_CHANGED, COMMENTED, CREATED

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(columnDefinition = "TEXT")
    private String description;
}
