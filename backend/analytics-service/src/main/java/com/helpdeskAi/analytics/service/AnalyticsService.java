package com.helpdeskAi.analytics.service;

import com.helpdeskAi.analytics.model.TicketMetrics;
import com.helpdeskAi.analytics.repository.TicketMetricsRepository;
import com.helpdeskAi.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TicketMetricsRepository metricsRepo;

    /**
     * Overview KPIs for the current tenant over the last N days.
     */
    public Map<String, Object> getOverview(int days) {
        UUID orgId = TenantContext.getTenantId();
        LocalDate to   = LocalDate.now();
        LocalDate from = to.minusDays(days - 1);

        Long created  = nullsafe(metricsRepo.sumCreated(orgId, from, to));
        Long resolved = nullsafe(metricsRepo.sumResolved(orgId, from, to));
        Double csat   = metricsRepo.avgCsat(orgId, from, to);
        Double resTime = metricsRepo.avgResolutionTime(orgId, from, to);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("periodDays",          days);
        result.put("totalCreated",        created);
        result.put("totalResolved",       resolved);
        result.put("resolutionRate",      created > 0 ? Math.round((resolved * 100.0 / created) * 10) / 10.0 : 0.0);
        result.put("avgCsatScore",        csat != null ? Math.round(csat * 100) / 100.0 : null);
        result.put("avgResolutionMinutes", resTime != null ? Math.round(resTime) : null);
        return result;
    }

    /**
     * Daily volume timeseries for charts.
     */
    public List<Map<String, Object>> getVolumeTimeseries(int days) {
        UUID orgId = TenantContext.getTenantId();
        LocalDate to   = LocalDate.now();
        LocalDate from = to.minusDays(days - 1);

        List<TicketMetrics> metrics =
                metricsRepo.findByOrganizationIdAndMetricDateBetweenOrderByMetricDateAsc(orgId, from, to);

        // Fill missing days with zeros
        Map<LocalDate, TicketMetrics> byDate = new LinkedHashMap<>();
        for (TicketMetrics m : metrics) byDate.put(m.getMetricDate(), m);

        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            TicketMetrics m = byDate.get(d);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date",       d.toString());
            row.put("created",    m != null ? m.getTotalCreated()    : 0L);
            row.put("resolved",   m != null ? m.getTotalResolved()   : 0L);
            row.put("aiResolved", m != null ? m.getAiAutoResolved()  : 0L);
            result.add(row);
        }
        return result;
    }

    /**
     * Priority breakdown for pie/bar chart.
     */
    public Map<String, Object> getPriorityBreakdown(int days) {
        UUID orgId = TenantContext.getTenantId();
        LocalDate to   = LocalDate.now();
        LocalDate from = to.minusDays(days - 1);

        List<TicketMetrics> metrics =
                metricsRepo.findByOrganizationIdAndMetricDateBetweenOrderByMetricDateAsc(orgId, from, to);

        long low = 0, medium = 0, high = 0, urgent = 0;
        for (TicketMetrics m : metrics) {
            low    += m.getPriorityLow();
            medium += m.getPriorityMedium();
            high   += m.getPriorityHigh();
            urgent += m.getPriorityUrgent();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("LOW",    low);
        result.put("MEDIUM", medium);
        result.put("HIGH",   high);
        result.put("URGENT", urgent);
        return result;
    }

    /**
     * CSAT trend per day.
     */
    public List<Map<String, Object>> getCsatTrend(int days) {
        UUID orgId = TenantContext.getTenantId();
        LocalDate to   = LocalDate.now();
        LocalDate from = to.minusDays(days - 1);

        List<TicketMetrics> metrics =
                metricsRepo.findByOrganizationIdAndMetricDateBetweenOrderByMetricDateAsc(orgId, from, to);

        List<Map<String, Object>> result = new ArrayList<>();
        for (TicketMetrics m : metrics) {
            if (m.getCsatAvg() != null) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("date",      m.getMetricDate().toString());
                row.put("csat",      m.getCsatAvg());
                row.put("responses", m.getCsatResponses());
                result.add(row);
            }
        }
        return result;
    }

    /**
     * Upsert today's snapshot (called by scheduled job or explicitly).
     * In production this would query ticket-service. Here we seed demo data.
     */
    public TicketMetrics upsertTodaySnapshot(UUID orgId,
                                              long created, long resolved, long open, long inProgress,
                                              long low, long medium, long high, long urgent,
                                              long aiResolved, Double csatAvg, long csatResponses,
                                              Double avgResMin, Double avgFrMin, long slaBreached) {
        LocalDate today = LocalDate.now();
        TicketMetrics m = metricsRepo.findByOrganizationIdAndMetricDate(orgId, today)
                .orElseGet(() -> TicketMetrics.builder()
                        .organizationId(orgId)
                        .metricDate(today)
                        .build());

        m.setTotalCreated(created);
        m.setTotalResolved(resolved);
        m.setTotalOpen(open);
        m.setTotalInProgress(inProgress);
        m.setPriorityLow(low);
        m.setPriorityMedium(medium);
        m.setPriorityHigh(high);
        m.setPriorityUrgent(urgent);
        m.setAiAutoResolved(aiResolved);
        m.setCsatAvg(csatAvg);
        m.setCsatResponses(csatResponses);
        m.setAvgResolutionMinutes(avgResMin);
        m.setAvgFirstResponseMinutes(avgFrMin);
        m.setSlaBreachedCount(slaBreached);

        return metricsRepo.save(m);
    }

    private long nullsafe(Long v) { return v != null ? v : 0L; }
}
