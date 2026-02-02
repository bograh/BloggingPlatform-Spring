package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect;
import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect.MethodMetrics;
import org.amalitech.bloggingplatformspring.config.CacheConfig;
import org.amalitech.bloggingplatformspring.config.CacheConfig.CacheStatistics;
import org.amalitech.bloggingplatformspring.dtos.responses.CacheMetricsDTO;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class PerformanceMetricsService {

    private static final String STRING_FORMAT = "%.2f%%";
    private static final String TIMESTAMP_LABEL = "timestamp";
    private final PerformanceMonitoringAspect performanceAspect;

    /**
     * Get all metrics
     */
    public Map<String, Object> getAllMetrics() {
        ConcurrentHashMap<String, MethodMetrics> allMetrics = performanceAspect.getAllMetrics();

        Map<String, Object> result = new HashMap<>();
        result.put("totalMethods", allMetrics.size());
        result.put(TIMESTAMP_LABEL, new Date());
        result.put("metrics", allMetrics);

        return result;
    }

    /**
     * Get metrics for a specific method
     */
    public MethodMetrics getMethodMetrics(String methodName) {
        return performanceAspect.getMetrics(methodName);
    }

    public Map<String, Object> getMetricsSummary() {
        ConcurrentHashMap<String, MethodMetrics> allMetrics = performanceAspect.getAllMetrics();

        long totalExecutions = allMetrics.values().stream()
                .mapToLong(MethodMetrics::getTotalCalls)
                .sum();

        long totalFailures = allMetrics.values().stream()
                .mapToLong(MethodMetrics::getFailedCalls)
                .sum();

        double overallAvgTime = allMetrics.values().stream()
                .mapToLong(MethodMetrics::getAverageExecutionTime)
                .average()
                .orElse(0.0);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalMethodsMonitored", allMetrics.size());
        summary.put("totalExecutions", totalExecutions);
        summary.put("totalFailures", totalFailures);
        summary.put("overallAverageExecutionTime", String.format("%.2f ms", overallAvgTime));
        summary.put(TIMESTAMP_LABEL, new Date());

        return summary;
    }

    /**
     * Reset all metrics
     */
    public void resetMetrics() {
        performanceAspect.resetMetrics();
    }

    /**
     * Export performance summary to logs
     */
    public void exportPerformanceSummary() {
        performanceAspect.exportPerformanceSummary();
    }

    /**
     * Export combined performance and cache metrics summary to logs and file
     */
    public void exportAllMetrics() {
        performanceAspect.exportCombinedMetrics(getAllCacheMetrics(), getCacheSummary());
    }

    /**
     * Export only cache metrics to logs and file
     */
    public void exportCacheMetrics() {
        performanceAspect.exportCacheMetricsToFile(getAllCacheMetrics(), getCacheSummary());
    }

    /**
     * Get all cache statistics
     */
    public Map<String, Object> getAllCacheMetrics() {
        ConcurrentMap<String, CacheStatistics> allStats = CacheConfig.getAllCacheStatistics();

        Map<String, CacheMetricsDTO> cacheMetrics = new HashMap<>();

        for (Map.Entry<String, CacheStatistics> entry : allStats.entrySet()) {
            CacheStatistics stats = entry.getValue();
            CacheMetricsDTO metrics = setCacheMetrics(stats);
            cacheMetrics.put(entry.getKey(), metrics);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCaches", cacheMetrics.size());
        result.put(TIMESTAMP_LABEL, new Date());
        result.put("caches", cacheMetrics);

        return result;
    }

    /**
     * Get cache statistics for a specific cache
     */
    public CacheMetricsDTO getCacheMetrics(String cacheName) {
        ConcurrentHashMap<String, CacheStatistics> allStats = CacheConfig.getAllCacheStatistics();
        CacheStatistics stats = allStats.get(cacheName);

        if (stats == null) {
            throw new BadRequestException("Cache not found: " + cacheName);
        }

        return setCacheMetrics(stats);
    }

    /**
     * Get cache summary statistics
     */
    public Map<String, Object> getCacheSummary() {
        ConcurrentHashMap<String, CacheStatistics> allStats = CacheConfig.getAllCacheStatistics();

        long totalHits = allStats.values().stream()
                .mapToLong(CacheStatistics::getHits)
                .sum();

        long totalMisses = allStats.values().stream()
                .mapToLong(CacheStatistics::getMisses)
                .sum();

        long totalPuts = allStats.values().stream()
                .mapToLong(CacheStatistics::getPuts)
                .sum();

        long totalEvictions = allStats.values().stream()
                .mapToLong(CacheStatistics::getEvictions)
                .sum();

        long totalRequests = totalHits + totalMisses;
        double overallHitRate = totalRequests == 0 ? 0.0 : (double) totalHits / totalRequests * 100;

        Optional<CacheStatistics> bestCache = allStats.values().stream()
                .filter(s -> s.getTotalRequests() > 0)
                .max(Comparator.comparingDouble(CacheStatistics::getHitRate));

        Optional<CacheStatistics> worstCache = allStats.values().stream()
                .filter(s -> s.getTotalRequests() > 0)
                .min(Comparator.comparingDouble(CacheStatistics::getHitRate));

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCaches", allStats.size());
        summary.put("totalHits", totalHits);
        summary.put("totalMisses", totalMisses);
        summary.put("totalRequests", totalRequests);
        summary.put("overallHitRate", String.format(STRING_FORMAT, overallHitRate));
        summary.put("totalPuts", totalPuts);
        summary.put("totalEvictions", totalEvictions);

        if (bestCache.isPresent()) {
            Map<String, Object> best = new HashMap<>();
            best.put("name", bestCache.get().getCacheName());
            best.put("hitRate", String.format(STRING_FORMAT, bestCache.get().getHitRate()));
            summary.put("bestPerformingCache", best);
        }

        if (worstCache.isPresent()) {
            Map<String, Object> worst = new HashMap<>();
            worst.put("name", worstCache.get().getCacheName());
            worst.put("hitRate", String.format(STRING_FORMAT, worstCache.get().getHitRate()));
            summary.put("worstPerformingCache", worst);
        }

        summary.put(TIMESTAMP_LABEL, new Date());

        return summary;
    }

    /**
     * Reset cache statistics
     */
    public void resetCacheMetrics() {
        CacheConfig.resetAllStatistics();
    }

    private CacheMetricsDTO setCacheMetrics(CacheStatistics stats) {
        return new CacheMetricsDTO(
                stats.getCacheName(),
                stats.getHits(),
                stats.getMisses(),
                String.format(STRING_FORMAT, stats.getHitRate()),
                String.format(STRING_FORMAT, stats.getMissRate()),
                stats.getTotalRequests(),
                stats.getPuts(),
                stats.getEvictions(),
                stats.getClears(),
                String.valueOf(LocalDateTime.now()));
    }
}