package com.helpdeskAi.chat.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(columnList = "session_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ChatMessage extends TenantAwareEntity {

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "sender_id")
    private String senderId;

    @Column(name = "sender_name")
    private String senderName;

    @Builder.Default
    @Column(name = "sender_type", length = 20)
    private String senderType = "CUSTOMER";  // CUSTOMER | AGENT | BOT

    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false;
}
