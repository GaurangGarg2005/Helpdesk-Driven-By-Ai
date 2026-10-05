package com.helpdeskAi.analytics.model;

import com.helpdeskAi.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "ticket_metrics", indexes = {
    @Index(columnList = "organization_id, metric_date", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TicketMetrics extends TenantAwareEntity {

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Builder.Default private Long totalCreated    = 0L;
    @Builder.Default private Long totalResolved   = 0L;
    @Builder.Default private Long totalOpen       = 0L;
    @Builder.Default private Long totalInProgress = 0L;

    @Builder.Default private Long priorityLow    = 0L;
    @Builder.Default private Long priorityMedium = 0L;
    @Builder.Default private Long priorityHigh   = 0L;
    @Builder.Default private Long priorityUrgent = 0L;

    @Builder.Default private Long aiAutoResolved = 0L;
    @Builder.Default private Long aiClassified   = 0L;

    @Column(name = "avg_resolution_minutes")
    private Double avgResolutionMinutes;

    @Column(name = "avg_first_response_minutes")
    private Double avgFirstResponseMinutes;

    @Column(name = "sla_breached_count")
    @Builder.Default private Long slaBreachedCount = 0L;

    // CSAT (Customer Satisfaction): 1–5 scale
    @Column(name = "csat_avg")
    private Double csatAvg;

    @Column(name = "csat_responses")
    @Builder.Default private Long csatResponses = 0L;
}
