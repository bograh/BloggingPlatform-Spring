package org.amalitech.bloggingplatformspring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect;
import org.amalitech.bloggingplatformspring.dtos.responses.CacheMetricsDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.StatusResponse;
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
        public ResponseEntity<Map<String, Object>> getAllMetrics() {
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
        public ResponseEntity<PerformanceMonitoringAspect.MethodMetrics> getMethodMetrics(
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
        public ResponseEntity<Map<String, Object>> getMetricsSummary() {
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
        public ResponseEntity<StatusResponse> resetMetrics() {
                metricsService.resetMetrics();
                return ResponseEntity.ok(StatusResponse.success("All performance metrics have been reset"));
        }

        /**
         * Export metrics to log file
         *
         * @return Confirmation message
         */
        @PostMapping("/export-log")
        @Operation(summary = "Export performance metrics to log", description = "Exports current performance metrics summary to the application log file and metrics folder")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Metrics successfully exported to log")
        })
        public ResponseEntity<StatusResponse> exportToLog() {
                metricsService.exportPerformanceSummary();
                return ResponseEntity.ok(StatusResponse
                                .success("Performance metrics exported to application log and metrics folder"));
        }

        /**
         * Export cache metrics to log file
         *
         * @return Confirmation message
         */
        @PostMapping("/cache/export-log")
        @Operation(summary = "Export cache metrics to log", description = "Exports current cache metrics to the application log file and metrics folder")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Cache metrics successfully exported to log")
        })
        public ResponseEntity<StatusResponse> exportCacheToLog() {
                metricsService.exportCacheMetrics();
                return ResponseEntity.ok(
                                StatusResponse.success("Cache metrics exported to application log and metrics folder"));
        }

        /**
         * Export combined performance and cache metrics to log file
         *
         * @return Confirmation message
         */
        @PostMapping("/export-all")
        @Operation(summary = "Export all metrics to log", description = "Exports both performance and cache metrics to the application log file and metrics folder")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "All metrics successfully exported to log")
        })
        public ResponseEntity<StatusResponse> exportAllMetrics() {
                metricsService.exportAllMetrics();
                return ResponseEntity.ok(StatusResponse.success(
                                "Combined performance and cache metrics exported to application log and metrics folder"));
        }

        /**
         * Get all cache metrics
         *
         * @return Map of all cache statistics
         */
        @GetMapping("/cache")
        @Operation(summary = "Get all cache metrics", description = "Retrieves cache statistics including hits, misses, and hit rates for all caches")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Cache metrics successfully retrieved")
        })
        public ResponseEntity<Map<String, Object>> getAllCacheMetrics() {
                return ResponseEntity.ok(metricsService.getAllCacheMetrics());
        }

        /**
         * Get metrics for a specific cache
         *
         * @param cacheName The name of the cache (e.g., users, posts, comments, tags)
         * @return Metrics for the specified cache
         */
        @GetMapping("/cache/{cacheName}")
        @Operation(summary = "Get metrics for a specific cache", description = "Retrieves detailed cache statistics for a specific cache by name")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Cache metrics successfully retrieved"),
                        @ApiResponse(responseCode = "400", description = "Cache not found")
        })
        public ResponseEntity<CacheMetricsDTO> getCacheMetrics(
                        @Parameter(description = "Cache name", example = "users") @PathVariable String cacheName) {
                return ResponseEntity.ok(metricsService.getCacheMetrics(cacheName));
        }

        /**
         * Get cache summary statistics
         *
         * @return Summary of all cache statistics
         */
        @GetMapping("/cache/summary")
        @Operation(summary = "Get cache summary", description = "Retrieves aggregated cache statistics including overall hit rate, best and worst performing caches")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Cache summary successfully retrieved")
        })
        public ResponseEntity<Map<String, Object>> getCacheSummary() {
                return ResponseEntity.ok(metricsService.getCacheSummary());
        }

        /**
         * Reset cache statistics
         *
         * @return Confirmation message
         */
        @DeleteMapping("/cache/reset")
        @Operation(summary = "Reset cache metrics", description = "Clears all cache statistics data")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Cache metrics successfully reset")
        })
        public ResponseEntity<StatusResponse> resetCacheMetrics() {
                metricsService.resetCacheMetrics();
                return ResponseEntity.ok(StatusResponse.success("All cache metrics have been reset"));
        }
}