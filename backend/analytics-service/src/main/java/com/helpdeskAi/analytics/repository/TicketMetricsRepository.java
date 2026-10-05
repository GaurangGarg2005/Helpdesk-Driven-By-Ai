package com.helpdeskAi.analytics.repository;

import com.helpdeskAi.analytics.model.TicketMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketMetricsRepository extends JpaRepository<TicketMetrics, UUID> {

    Optional<TicketMetrics> findByOrganizationIdAndMetricDate(UUID organizationId, LocalDate date);

    List<TicketMetrics> findByOrganizationIdAndMetricDateBetweenOrderByMetricDateAsc(
            UUID organizationId, LocalDate from, LocalDate to);

    @Query("SELECT SUM(m.totalCreated) FROM TicketMetrics m WHERE m.organizationId = :orgId AND m.metricDate BETWEEN :from AND :to")
    Long sumCreated(@Param("orgId") UUID orgId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT SUM(m.totalResolved) FROM TicketMetrics m WHERE m.organizationId = :orgId AND m.metricDate BETWEEN :from AND :to")
    Long sumResolved(@Param("orgId") UUID orgId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT AVG(m.csatAvg) FROM TicketMetrics m WHERE m.organizationId = :orgId AND m.metricDate BETWEEN :from AND :to AND m.csatAvg IS NOT NULL")
    Double avgCsat(@Param("orgId") UUID orgId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT AVG(m.avgResolutionMinutes) FROM TicketMetrics m WHERE m.organizationId = :orgId AND m.metricDate BETWEEN :from AND :to AND m.avgResolutionMinutes IS NOT NULL")
    Double avgResolutionTime(@Param("orgId") UUID orgId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
