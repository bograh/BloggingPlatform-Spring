package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect;
import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect.MethodMetrics;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for aggregating and formatting performance metrics.
 * Provides various views and analysis of performance data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceMetricsService {

    private final PerformanceMonitoringAspect performanceAspect;

    /**
     * Get all metrics in a formatted structure
     */
    public Map<String, Object> getAllMetricsFormatted() {
        ConcurrentHashMap<String, MethodMetrics> allMetrics = performanceAspect.getAllMetrics();

        Map<String, Object> result = new HashMap<>();
        result.put("totalMethods", allMetrics.size());
        result.put("timestamp", new Date());

        List<Map<String, Object>> methodsList = new ArrayList<>();
        allMetrics.forEach((methodName, metrics) -> {
            methodsList.add(formatMethodMetrics(methodName, metrics));
        });

        // Sort by average execution time (descending)
        methodsList.sort((m1, m2) -> Long.compare((Long) m2.get("avgExecutionTime"), (Long) m1.get("avgExecutionTime")));

        result.put("methods", methodsList);
        return result;
    }

    /**
     * Get metrics for a specific method
     */
    public Map<String, Object> getMethodMetricsFormatted(String methodName) {
        MethodMetrics metrics = performanceAspect.getMetrics(methodName);

        if (metrics == null) {
            Map<String, Object> notFound = new HashMap<>();
            notFound.put("error", "Method not found");
            notFound.put("methodName", methodName);
            return notFound;
        }

        return formatMethodMetrics(methodName, metrics);
    }

    /**
     * Get summary statistics
     */
    public Map<String, Object> getMetricsSummary() {
        return performanceAspect.getMetricsSummary();
    }

    /**
     * Get slow methods exceeding a threshold
     */
    public Map<String, Object> getSlowMethods(long thresholdMs) {
        ConcurrentHashMap<String, MethodMetrics> allMetrics = performanceAspect.getAllMetrics();

        List<Map<String, Object>> slowMethods = allMetrics.entrySet().stream()
                .filter(entry -> entry.getValue().getAverageExecutionTime() > thresholdMs)
                .map(entry -> formatMethodMetrics(entry.getKey(), entry.getValue()))
                .sorted((m1, m2) -> Long.compare((Long) m2.get("avgExecutionTime"), (Long) m1.get("avgExecutionTime")))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("threshold", thresholdMs + " ms");
        result.put("count", slowMethods.size());
        result.put("methods", slowMethods);
        return result;
    }

    /**
     * Get top N slowest methods
     */
    public Map<String, Object> getTopSlowMethods(int limit) {
        ConcurrentHashMap<String, MethodMetrics> allMetrics = performanceAspect.getAllMetrics();

        List<Map<String, Object>> topMethods = allMetrics.entrySet().stream()
                .map(entry -> formatMethodMetrics(entry.getKey(), entry.getValue()))
                .sorted((m1, m2) -> Long.compare((Long) m2.get("avgExecutionTime"), (Long) m1.get("avgExecutionTime")))
                .limit(limit)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("limit", limit);
        result.put("methods", topMethods);
        return result;
    }

    /**
     * Get metrics filtered by layer
     */
    public Map<String, Object> getMetricsByLayer(String layer) {
        ConcurrentHashMap<String, MethodMetrics> allMetrics = performanceAspect.getAllMetrics();

        List<Map<String, Object>> layerMethods = allMetrics.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(layer + "::"))
                .map(entry -> formatMethodMetrics(entry.getKey(), entry.getValue()))
                .sorted((m1, m2) -> Long.compare((Long) m2.get("avgExecutionTime"), (Long) m1.get("avgExecutionTime")))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("layer", layer);
        result.put("count", layerMethods.size());
        result.put("methods", layerMethods);
        return result;
    }

    /**
     * Get failure statistics
     */
    public Map<String, Object> getFailureStatistics() {
        ConcurrentHashMap<String, MethodMetrics> allMetrics = performanceAspect.getAllMetrics();

        List<Map<String, Object>> methodsWithFailures = allMetrics.entrySet().stream()
                .filter(entry -> entry.getValue().getFailedCalls() > 0)
                .map(entry -> {
                    Map<String, Object> methodData = new HashMap<>();
                    MethodMetrics metrics = entry.getValue();
                    methodData.put("method", entry.getKey());
                    methodData.put("totalCalls", metrics.getTotalCalls());
                    methodData.put("failedCalls", metrics.getFailedCalls());
                    methodData.put("successfulCalls", metrics.getSuccessfulCalls());
                    methodData.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
                    return methodData;
                })
                .sorted((m1, m2) -> Long.compare((Long) m2.get("failedCalls"), (Long) m1.get("failedCalls")))
                .collect(Collectors.toList());

        long totalFailures = allMetrics.values().stream()
                .mapToLong(MethodMetrics::getFailedCalls)
                .sum();

        Map<String, Object> result = new HashMap<>();
        result.put("totalFailures", totalFailures);
        result.put("methodsWithFailures", methodsWithFailures.size());
        result.put("methods", methodsWithFailures);
        return result;
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
     * Format method metrics into a map
     */
    private Map<String, Object> formatMethodMetrics(String methodName, MethodMetrics metrics) {
        Map<String, Object> formatted = new LinkedHashMap<>();
        formatted.put("method", methodName);
        formatted.put("totalCalls", metrics.getTotalCalls());
        formatted.put("successfulCalls", metrics.getSuccessfulCalls());
        formatted.put("failedCalls", metrics.getFailedCalls());
        formatted.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
        formatted.put("avgExecutionTime", metrics.getAverageExecutionTime());
        formatted.put("minExecutionTime", metrics.getMinExecutionTime());
        formatted.put("maxExecutionTime", metrics.getMaxExecutionTime());
        formatted.put("p50", metrics.getPercentile(50));
        formatted.put("p95", metrics.getPercentile(95));
        formatted.put("p99", metrics.getPercentile(99));
        formatted.put("stdDev", String.format("%.2f", metrics.getStandardDeviation()));
        formatted.put("unit", "ms");

        // Performance classification
        long avgTime = metrics.getAverageExecutionTime();
        String performance;
        if (avgTime < 100) {
            performance = "FAST";
        } else if (avgTime < 500) {
            performance = "NORMAL";
        } else if (avgTime < 1000) {
            performance = "SLOW";
        } else {
            performance = "CRITICAL";
        }
        formatted.put("performanceLevel", performance);

        return formatted;
    }
}