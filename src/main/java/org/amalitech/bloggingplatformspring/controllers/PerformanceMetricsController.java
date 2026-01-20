package org.amalitech.bloggingplatformspring.controllers;

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
public class PerformanceMetricsController {

  private final PerformanceMetricsService metricsService;

  /**
   * Get all performance metrics
   * 
   * @return Map of all method metrics
   */
  @GetMapping
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
  public ResponseEntity<Map<String, Object>> getMethodMetrics(
      @PathVariable String layer,
      @PathVariable String methodName) {
    String fullMethodName = layer.toUpperCase() + "::" + methodName;
    return ResponseEntity.ok(metricsService.getMethodMetricsFormatted(fullMethodName));
  }

  /**
   * Get summary of all metrics
   * 
   * @return Summary statistics of all methods
   */
  @GetMapping("/summary")
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
  public ResponseEntity<Map<String, Object>> getSlowMethods(
      @RequestParam(defaultValue = "1000") long thresholdMs) {
    return ResponseEntity.ok(metricsService.getSlowMethods(thresholdMs));
  }

  /**
   * Get top N methods by average execution time
   * 
   * @param limit Number of methods to return (default: 10)
   * @return Top methods by average execution time
   */
  @GetMapping("/top")
  public ResponseEntity<Map<String, Object>> getTopSlowMethods(
      @RequestParam(defaultValue = "10") int limit) {
    return ResponseEntity.ok(metricsService.getTopSlowMethods(limit));
  }

  /**
   * Get methods by layer (SERVICE or REPOSITORY)
   * 
   * @param layer The layer to filter by
   * @return Metrics for all methods in the specified layer
   */
  @GetMapping("/layer/{layer}")
  public ResponseEntity<Map<String, Object>> getMetricsByLayer(@PathVariable String layer) {
    return ResponseEntity.ok(metricsService.getMetricsByLayer(layer.toUpperCase()));
  }

  /**
   * Get failure statistics
   * 
   * @return Methods with failure counts and rates
   */
  @GetMapping("/failures")
  public ResponseEntity<Map<String, Object>> getFailureStatistics() {
    return ResponseEntity.ok(metricsService.getFailureStatistics());
  }

  /**
   * Reset all metrics
   * 
   * @return Confirmation message
   */
  @DeleteMapping("/reset")
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
  public ResponseEntity<Map<String, String>> exportToLog() {
    metricsService.exportPerformanceSummary();
    return ResponseEntity.ok(Map.of(
        "status", "success",
        "message", "Metrics exported to application log"));
  }
}