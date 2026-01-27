package org.amalitech.bloggingplatformspring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.services.PerformanceMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for exposing performance metrics.
 * Provides endpoints to view method execution statistics and performance data.
 */
@RestController
@RequestMapping("/api/metrics/performance")
@RequiredArgsConstructor
@Tag(name = "4. Performance Metrics", description = "APIs for monitoring application performance and method execution statistics")
public class PerformanceMetricsController {

    private final PerformanceMetricsService metricsService;

    /**
     * Get all performance metrics
     *
     * @return Map of all method metrics
     */
    @GetMapping
    @Operation(summary = "Get all performance metrics", description = "Retrieves performance statistics for all monitored methods including execution times, call counts, and failure rates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics successfully retrieved")
    })
    public ResponseEntity<?> getAllMetrics() {
        return ResponseEntity.ok(metricsService.getAllMetrics());
    }

    /**
     * Get metrics for a specific method
     *
     * @param methodName The name of the method (format: LAYER::methodName)
     * @return Metrics for the specified method
     */
    @GetMapping("/{layer}/{methodName}")
    @Operation(summary = "Get metrics for a specific method", description = "Retrieves detailed performance metrics for a specific method identified by layer and method name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Method metrics successfully retrieved")
    })
    public ResponseEntity<?> getMethodMetrics(
            @Parameter(description = "Layer name (SERVICE or REPOSITORY)", example = "SERVICE") @PathVariable String layer,
            @Parameter(description = "Method name", example = "createPost") @PathVariable String methodName) {
        String fullMethodName = layer.toUpperCase() + "::" + methodName;
        return ResponseEntity.ok(metricsService.getMethodMetrics(fullMethodName));
    }

    /**
     * Get summary of all metrics
     *
     * @return Summary statistics of all methods
     */
    @GetMapping("/summary")
    @Operation(summary = "Get performance metrics summary", description = "Retrieves aggregated statistics including total methods monitored, total calls, average execution time, and overall failure rate")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Summary successfully retrieved")
    })
    public ResponseEntity<?> getMetricsSummary() {
        return ResponseEntity.ok(metricsService.getMetricsSummary());
    }

    /**
     * Reset all metrics
     *
     * @return Confirmation message
     */
    @DeleteMapping("/reset")
    @Operation(summary = "Reset all metrics", description = "Clears all performance metrics data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics successfully reset")
    })
    public ResponseEntity<Map<String, String>> resetMetrics() {
        metricsService.resetMetrics();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "All performance metrics have been reset"));
    }

    /**
     * Export metrics to log file
     *
     * @return Confirmation message
     */
    @PostMapping("/export/log")
    @Operation(summary = "Export metrics to log", description = "Exports current performance metrics summary to the application log file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics successfully exported to log")
    })
    public ResponseEntity<Map<String, String>> exportToLog() {
        metricsService.exportPerformanceSummary();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Metrics exported to application log"));
    }
}