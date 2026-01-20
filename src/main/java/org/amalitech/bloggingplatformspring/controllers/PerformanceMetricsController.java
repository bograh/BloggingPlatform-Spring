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
@Tag(name = "Performance Metrics", description = "APIs for monitoring application performance and method execution statistics")
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
  public ResponseEntity<Map<String, Object>> getAllMetrics() {
    return ResponseEntity.ok(metricsService.getAllMetricsFormatted());
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
  public ResponseEntity<Map<String, Object>> getMethodMetrics(
      @Parameter(description = "Layer name (SERVICE or REPOSITORY)", example = "SERVICE") @PathVariable String layer,
      @Parameter(description = "Method name", example = "createPost") @PathVariable String methodName) {
    String fullMethodName = layer.toUpperCase() + "::" + methodName;
    return ResponseEntity.ok(metricsService.getMethodMetricsFormatted(fullMethodName));
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
  public ResponseEntity<Map<String, Object>> getMetricsSummary() {
    return ResponseEntity.ok(metricsService.getMetricsSummary());
  }

  /**
   * Get slow methods (execution time > threshold)
   *
   * @param thresholdMs Threshold in milliseconds (default: 1000ms)
   * @return List of methods exceeding the threshold
   */
  @GetMapping("/slow")
  @Operation(summary = "Get slow methods", description = "Retrieves methods with average execution time exceeding the specified threshold in milliseconds")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Slow methods successfully retrieved")
  })
  public ResponseEntity<Map<String, Object>> getSlowMethods(
      @Parameter(description = "Threshold in milliseconds", example = "1000") @RequestParam(defaultValue = "1000") long thresholdMs) {
    return ResponseEntity.ok(metricsService.getSlowMethods(thresholdMs));
  }

  /**
   * Get top N methods by average execution time
   *
   * @param limit Number of methods to return (default: 10)
   * @return Top methods by average execution time
   */
  @GetMapping("/top")
  @Operation(summary = "Get top slowest methods", description = "Retrieves the top N methods with the highest average execution times")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Top methods successfully retrieved")
  })
  public ResponseEntity<Map<String, Object>> getTopSlowMethods(
      @Parameter(description = "Number of methods to return", example = "10") @RequestParam(defaultValue = "10") int limit) {
    return ResponseEntity.ok(metricsService.getTopSlowMethods(limit));
  }

  /**
   * Get methods by layer (SERVICE or REPOSITORY)
   *
   * @param layer The layer to filter by
   * @return Metrics for all methods in the specified layer
   */
  @GetMapping("/layer/{layer}")
  @Operation(summary = "Get metrics by layer", description = "Retrieves performance metrics for all methods in a specific layer (SERVICE or REPOSITORY)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Layer metrics successfully retrieved")
  })
  public ResponseEntity<Map<String, Object>> getMetricsByLayer(
      @Parameter(description = "Layer name (SERVICE or REPOSITORY)", example = "SERVICE") @PathVariable String layer) {
    return ResponseEntity.ok(metricsService.getMetricsByLayer(layer.toUpperCase()));
  }

  /**
   * Get failure statistics
   *
   * @return Methods with failure counts and rates
   */
  @GetMapping("/failures")
  @Operation(summary = "Get failure statistics", description = "Retrieves methods with their failure counts and failure rates")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Failure statistics successfully retrieved")
  })
  public ResponseEntity<Map<String, Object>> getFailureStatistics() {
    return ResponseEntity.ok(metricsService.getFailureStatistics());
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
   * Export metrics to console log
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