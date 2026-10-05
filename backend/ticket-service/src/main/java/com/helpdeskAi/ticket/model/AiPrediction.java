package com.helpdeskAi.ticket.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ai_predictions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AiPrediction extends TenantAwareEntity {

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "prediction_type", nullable = false, length = 50)
    private String predictionType;  // CATEGORY, PRIORITY, SENTIMENT

    @Column(name = "predicted_value", nullable = false, length = 100)
    private String predictedValue;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "agent_corrected_value", length = 100)
    private String agentCorrectedValue;

    @Column(name = "was_accepted")
    private Boolean wasAccepted;
}
