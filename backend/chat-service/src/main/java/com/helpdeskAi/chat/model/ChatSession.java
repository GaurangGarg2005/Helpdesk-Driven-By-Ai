package com.helpdeskAi.chat.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "chat_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ChatSession extends TenantAwareEntity {

    @Column(name = "visitor_id")
    private String visitorId;     // Anonymous or userId for logged-in customers

    @Column(name = "visitor_name")
    private String visitorName;

    @Column(name = "visitor_email")
    private String visitorEmail;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Column(name = "agent_name")
    private String agentName;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "OPEN";   // OPEN | ACTIVE | CLOSED | QUEUED

    @Column(name = "ticket_id")   // Optional: linked ticket
    private UUID ticketId;

    private String channel;  // WEB | MOBILE
}
