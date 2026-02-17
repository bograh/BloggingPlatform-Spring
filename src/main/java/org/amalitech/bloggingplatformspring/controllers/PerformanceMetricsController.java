package org.amalitech.bloggingplatformspring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.responses.*;
import org.amalitech.bloggingplatformspring.entity.CacheMetricsSnapshot;
import org.amalitech.bloggingplatformspring.entity.PerformanceMetricsSnapshot;
import org.amalitech.bloggingplatformspring.services.CachePerformanceSimulationService;
import org.amalitech.bloggingplatformspring.services.PerformanceMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for exposing performance metrics.
 * Provides endpoints to view method execution statistics and performance data.
 */
@RestController
@RequestMapping("/api/metrics/performance")
@RequiredArgsConstructor
@Tag(name = "5. Performance Metrics", description = "APIs for monitoring application performance and method execution statistics")
public class PerformanceMetricsController {

        private final PerformanceMetricsService metricsService;
        private final CachePerformanceSimulationService simulationService;

        /**
         * Get all performance metrics
         */
        @GetMapping
        @Operation(summary = "Get all performance metrics", description = "Retrieves performance statistics for all monitored methods")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Metrics successfully retrieved")
        })
        public ResponseEntity<AllMetricsDTO> getAllMetrics() {
                return ResponseEntity.ok(metricsService.getAllMetrics());
        }

        /**
         * Get metrics for a specific method by layer and method name
         */
        @GetMapping("/{layer}/{methodName}")
        @Operation(summary = "Get metrics for a specific method by layer and method name", description = "Retrieves detailed performance metrics for a specific method identified by its layer and name")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Method metrics successfully retrieved")
        })
        public ResponseEntity<MethodMetricsDTO> getMethodMetricsByLayerAndName(
                        @Parameter(description = "Layer name (e.g., SERVICE, REPOSITORY, CONTROLLER)", example = "SERVICE") @PathVariable String layer,
                        @Parameter(description = "Method name", example = "createPost") @PathVariable String methodName) {
                String fullMethodName = layer.toUpperCase() + "::" + methodName;
                return ResponseEntity.ok(metricsService.getMethodMetrics(fullMethodName));
        }

        /**
         * Get metrics for a specific method
         */
        @GetMapping("/method/{methodName}")
        @Operation(summary = "Get metrics for a specific method", description = "Retrieves detailed performance metrics for a specific method")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Method metrics successfully retrieved")
        })
        public ResponseEntity<MethodMetricsDTO> getMethodMetrics(
                        @Parameter(description = "Method name", example = "PostService.getAllPosts(..)") @PathVariable String methodName) {
                return ResponseEntity.ok(metricsService.getMethodMetrics(methodName));
        }

        /**
         * Get metrics summary
         */
        @GetMapping("/summary")
        @Operation(summary = "Get performance metrics summary", description = "Retrieves aggregated statistics")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Summary successfully retrieved")
        })
        public ResponseEntity<MetricsSummaryDTO> getMetricsSummary() {
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
         */
        @GetMapping("/cache")
        @Operation(summary = "Get all cache metrics", description = "Retrieves cache statistics including hits, misses, and hit rates for all caches")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Cache metrics successfully retrieved")
        })
        public ResponseEntity<AllCacheMetricsDTO> getAllCacheMetrics() {
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
         * Get cache summary
         */
        @GetMapping("/cache/summary")
        @Operation(summary = "Get cache summary", description = "Retrieves aggregated cache statistics")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Cache summary successfully retrieved")
        })
        public ResponseEntity<CacheSummaryDTO> getCacheSummary() {
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

        /**
         * Get list of available pre-cache metrics files
         */
        @GetMapping("/comparison/pre-cache-files")
        @Operation(summary = "Get pre-cache files", description = "Lists all available pre-cache metrics files that can be used for comparison")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "File list retrieved successfully")
        })
        public ResponseEntity<List<String>> getPreCacheFiles() {
                return ResponseEntity.ok(metricsService.getPreCacheFiles());
        }

        /**
         * Compare current metrics with a specific pre-cache file
         */
        @GetMapping("/comparison/{fileName}")
        @Operation(summary = "Compare with pre-cache file", description = "Compares current post-cache metrics with a specific pre-cache metrics file")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Comparison completed successfully"),
                        @ApiResponse(responseCode = "400", description = "Pre-cache file not found")
        })
        public ResponseEntity<PerformanceComparisonDTO> compareWithPreCache(
                        @Parameter(description = "Pre-cache metrics file name") @PathVariable String fileName) {
                return ResponseEntity.ok(metricsService.compareWithPreCache(fileName));
        }

        /**
         * Compare current metrics with the latest pre-cache file
         */
        @GetMapping("/comparison")
        @Operation(summary = "Compare with latest pre-cache", description = "Compares current post-cache metrics with the most recent pre-cache metrics file")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Comparison completed successfully"),
                        @ApiResponse(responseCode = "400", description = "No pre-cache files found")
        })
        public ResponseEntity<PerformanceComparisonDTO> compareWithLatestPreCache() {
                return ResponseEntity.ok(metricsService.compareWithLatestPreCache());
        }

        /**
         * Save current performance metrics to database
         */
        @PostMapping("/save")
        @Operation(summary = "Save performance metrics to database", description = "Saves current performance metrics snapshot to the database")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Metrics saved successfully")
        })
        public ResponseEntity<PerformanceMetricsSnapshot> savePerformanceMetrics(
                        @Parameter(description = "Snapshot type (MANUAL, SCHEDULED, etc.)", example = "MANUAL") @RequestParam(defaultValue = "MANUAL") String snapshotType) {
                return ResponseEntity.ok(metricsService.savePerformanceMetricsSnapshot(snapshotType));
        }

        /**
         * Save current cache metrics to database
         */
        @PostMapping("/cache/save")
        @Operation(summary = "Save cache metrics to database", description = "Saves current cache metrics snapshot to the database")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Cache metrics saved successfully")
        })
        public ResponseEntity<CacheMetricsSnapshot> saveCacheMetrics(
                        @Parameter(description = "Snapshot type (MANUAL, SCHEDULED, etc.)", example = "MANUAL") @RequestParam(defaultValue = "MANUAL") String snapshotType) {
                return ResponseEntity.ok(metricsService.saveCacheMetricsSnapshot(snapshotType));
        }

        /**
         * Save all metrics (performance + cache) to database
         */
        @PostMapping("/save-all")
        @Operation(summary = "Save all metrics to database", description = "Saves both performance and cache metrics snapshots to the database")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "All metrics saved successfully")
        })
        public ResponseEntity<StatusResponse> saveAllMetrics(
                        @Parameter(description = "Snapshot type (MANUAL, SCHEDULED, etc.)", example = "MANUAL") @RequestParam(defaultValue = "MANUAL") String snapshotType) {
                metricsService.saveAllMetricsSnapshot(snapshotType);
                return ResponseEntity.ok(StatusResponse.success("All metrics saved to database"));
        }

        /**
         * Get performance metrics history from database
         */
        @GetMapping("/history")
        @Operation(summary = "Get performance metrics history", description = "Retrieves historical performance metrics snapshots from the database")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "History retrieved successfully")
        })
        public ResponseEntity<List<PerformanceMetricsSnapshot>> getPerformanceMetricsHistory(
                        @Parameter(description = "Number of records to retrieve", example = "10") @RequestParam(defaultValue = "10") int limit) {
                return ResponseEntity.ok(metricsService.getPerformanceMetricsHistory(limit));
        }

        /**
         * Get cache metrics history from database
         */
        @GetMapping("/cache/history")
        @Operation(summary = "Get cache metrics history", description = "Retrieves historical cache metrics snapshots from the database")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "History retrieved successfully")
        })
        public ResponseEntity<List<CacheMetricsSnapshot>> getCacheMetricsHistory(
                        @Parameter(description = "Number of records to retrieve", example = "10") @RequestParam(defaultValue = "10") int limit) {
                return ResponseEntity.ok(metricsService.getCacheMetricsHistory(limit));
        }

        /**
         * Save current metrics as PRE_CACHE baseline (resets metrics after saving)
         */
        @PostMapping("/baseline")
        @Operation(summary = "Save PRE_CACHE baseline", description = "Saves current metrics as PRE_CACHE baseline and resets metrics for fresh measurement. Use this BEFORE enabling caching or when measuring without cache.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "PRE_CACHE baseline saved and metrics reset")
        })
        public ResponseEntity<PerformanceMetricsSnapshot> savePreCacheBaseline() {
                return ResponseEntity.ok(metricsService.savePreCacheBaseline());
        }

        /**
         * Save current metrics as POST_CACHE
         */
        @PostMapping("/postcache")
        @Operation(summary = "Save POST_CACHE metrics", description = "Saves current metrics as POST_CACHE snapshot. Use this AFTER enabling caching to capture cached performance.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "POST_CACHE metrics saved")
        })
        public ResponseEntity<PerformanceMetricsSnapshot> savePostCacheMetrics() {
                return ResponseEntity.ok(metricsService.savePostCacheMetrics());
        }

        /**
         * Get latest PRE_CACHE snapshot
         */
        @GetMapping("/baseline/latest")
        @Operation(summary = "Get latest PRE_CACHE baseline", description = "Retrieves the most recent PRE_CACHE baseline snapshot")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "PRE_CACHE baseline retrieved"),
                        @ApiResponse(responseCode = "400", description = "No PRE_CACHE baseline found")
        })
        public ResponseEntity<PerformanceMetricsSnapshot> getLatestPreCacheSnapshot() {
                return ResponseEntity.ok(metricsService.getLatestPreCacheSnapshot());
        }

        /**
         * Get latest POST_CACHE snapshot
         */
        @GetMapping("/postcache/latest")
        @Operation(summary = "Get latest POST_CACHE metrics", description = "Retrieves the most recent POST_CACHE snapshot")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "POST_CACHE metrics retrieved"),
                        @ApiResponse(responseCode = "400", description = "No POST_CACHE snapshot found")
        })
        public ResponseEntity<PerformanceMetricsSnapshot> getLatestPostCacheSnapshot() {
                return ResponseEntity.ok(metricsService.getLatestPostCacheSnapshot());
        }

        /**
         * Get all PRE_CACHE snapshots
         */
        @GetMapping("/baseline/history")
        @Operation(summary = "Get PRE_CACHE baseline history", description = "Retrieves all PRE_CACHE baseline snapshots")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "PRE_CACHE history retrieved")
        })
        public ResponseEntity<List<PerformanceMetricsSnapshot>> getPreCacheSnapshots(
                        @Parameter(description = "Number of records to retrieve", example = "10") @RequestParam(defaultValue = "10") int limit) {
                return ResponseEntity.ok(metricsService.getPreCacheSnapshots(limit));
        }

        /**
         * Get all POST_CACHE snapshots
         */
        @GetMapping("/postcache/history")
        @Operation(summary = "Get POST_CACHE metrics history", description = "Retrieves all POST_CACHE snapshots")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "POST_CACHE history retrieved")
        })
        public ResponseEntity<List<PerformanceMetricsSnapshot>> getPostCacheSnapshots(
                        @Parameter(description = "Number of records to retrieve", example = "10") @RequestParam(defaultValue = "10") int limit) {
                return ResponseEntity.ok(metricsService.getPostCacheSnapshots(limit));
        }

        /**
         * Compare latest PRE_CACHE vs POST_CACHE from database
         */
        @GetMapping("/comparison/database")
        @Operation(summary = "Compare PRE_CACHE vs POST_CACHE from database", description = "Compares the latest PRE_CACHE and POST_CACHE snapshots stored in the database")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Comparison completed"),
                        @ApiResponse(responseCode = "400", description = "Required snapshots not found")
        })
        public ResponseEntity<PerformanceComparisonDTO> compareFromDatabase() {
                return ResponseEntity.ok(metricsService.compareFromDatabase());
        }

        /**
         * Compare specific PRE_CACHE vs POST_CACHE by IDs
         */
        @GetMapping("/comparison/database/{preCacheId}/{postCacheId}")
        @Operation(summary = "Compare specific snapshots by ID", description = "Compares specific PRE_CACHE and POST_CACHE snapshots by their database IDs")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Comparison completed"),
                        @ApiResponse(responseCode = "400", description = "Snapshot not found")
        })
        public ResponseEntity<PerformanceComparisonDTO> compareFromDatabaseByIds(
                        @Parameter(description = "PRE_CACHE snapshot ID") @PathVariable String preCacheId,
                        @Parameter(description = "POST_CACHE snapshot ID") @PathVariable String postCacheId) {
                return ResponseEntity.ok(metricsService.compareFromDatabase(preCacheId, postCacheId));
        }

        /**
         * Run full PRE_CACHE vs POST_CACHE performance simulation
         */
        @PostMapping("/simulation/run")
        @Operation(summary = "Run cache performance simulation", description = "Simulates PRE_CACHE vs POST_CACHE performance for getAllPosts, getPostById, getComments, getPopularTags. "
                        +
                        "Clears cache before each PRE_CACHE call (simulating no cache), then measures cache hits for POST_CACHE.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Simulation completed successfully")
        })
        public ResponseEntity<java.util.Map<String, Object>> runCacheSimulation() {
                return ResponseEntity.ok(simulationService.runFullSimulation());
        }

        /**
         * Simulate a specific method
         */
        @PostMapping("/simulation/method/{methodType}")
        @Operation(summary = "Simulate specific method", description = "Runs PRE_CACHE vs POST_CACHE simulation for a specific method. "
                        +
                        "Valid methodTypes: getAllPosts, getPostById, getCommentsByPostId, getPopularTags, getAllComments")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Method simulation completed"),
                        @ApiResponse(responseCode = "400", description = "Invalid method type or missing resourceId")
        })
        public ResponseEntity<java.util.Map<String, Object>> simulateMethod(
                        @Parameter(description = "Method type to simulate", example = "getAllPosts") @PathVariable String methodType,
                        @Parameter(description = "Resource ID (required for getPostById, getCommentsByPostId)") @RequestParam(required = false) Long resourceId) {
                return ResponseEntity.ok(simulationService.simulateMethod(methodType, resourceId));
        }

        /**
         * Simulate getAllPosts
         */
        @PostMapping("/simulation/getAllPosts")
        @Operation(summary = "Simulate getAllPosts", description = "Runs PRE_CACHE vs POST_CACHE simulation specifically for PostService.getAllPosts()")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Simulation completed")
        })
        public ResponseEntity<java.util.Map<String, Object>> simulateGetAllPosts() {
                return ResponseEntity.ok(simulationService.simulateGetAllPosts());
        }

        /**
         * Simulate getPostById
         */
        @PostMapping("/simulation/getPostById/{postId}")
        @Operation(summary = "Simulate getPostById", description = "Runs PRE_CACHE vs POST_CACHE simulation specifically for PostService.getPostById()")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Simulation completed")
        })
        public ResponseEntity<java.util.Map<String, Object>> simulateGetPostById(
                        @Parameter(description = "Post ID to simulate") @PathVariable Long postId) {
                return ResponseEntity.ok(simulationService.simulateGetPostById(postId));
        }

        /**
         * Simulate getCommentsByPostId
         */
        @PostMapping("/simulation/getCommentsByPostId/{postId}")
        @Operation(summary = "Simulate getCommentsByPostId", description = "Runs PRE_CACHE vs POST_CACHE simulation specifically for CommentService.getAllCommentsByPostId()")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Simulation completed")
        })
        public ResponseEntity<java.util.Map<String, Object>> simulateGetCommentsByPostId(
                        @Parameter(description = "Post ID to get comments for") @PathVariable Long postId) {
                return ResponseEntity.ok(simulationService.simulateGetAllCommentsByPostId(postId));
        }

        /**
         * Simulate getPopularTags
         */
        @PostMapping("/simulation/getPopularTags")
        @Operation(summary = "Simulate getPopularTags", description = "Runs PRE_CACHE vs POST_CACHE simulation specifically for TagService.getPopularTags()")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Simulation completed")
        })
        public ResponseEntity<java.util.Map<String, Object>> simulateGetPopularTags() {
                return ResponseEntity.ok(simulationService.simulateGetPopularTags());
        }
}