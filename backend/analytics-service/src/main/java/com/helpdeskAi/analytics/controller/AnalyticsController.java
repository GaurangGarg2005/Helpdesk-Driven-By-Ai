package com.helpdeskAi.analytics.controller;

import com.helpdeskAi.analytics.service.AnalyticsService;
import com.helpdeskAi.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview(
            @RequestParam(defaultValue = "30") int days) {
        return ok("Overview metrics", analyticsService.getOverview(days));
    }

    @GetMapping("/volume")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getVolumeTimeseries(
            @RequestParam(defaultValue = "30") int days) {
        return ok("Volume timeseries", analyticsService.getVolumeTimeseries(days));
    }

    @GetMapping("/priority")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPriorityBreakdown(
            @RequestParam(defaultValue = "30") int days) {
        return ok("Priority breakdown", analyticsService.getPriorityBreakdown(days));
    }

    @GetMapping("/csat")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCsatTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ok("CSAT trend", analyticsService.getCsatTrend(days));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(String msg, T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, msg, data, Instant.now()));
    }
}
