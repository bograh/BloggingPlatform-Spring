package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect;
import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect.MethodMetrics;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PerformanceMetricsService {

    private final PerformanceMonitoringAspect performanceAspect;

    /**
     * Get all metrics
     */
    public Map<String, Object> getAllMetrics() {
        ConcurrentHashMap<String, MethodMetrics> allMetrics = performanceAspect.getAllMetrics();

        Map<String, Object> result = new HashMap<>();
        result.put("totalMethods", allMetrics.size());
        result.put("timestamp", new Date());
        result.put("metrics", allMetrics);

        return result;
    }

    /**
     * Get metrics for a specific method
     */
    public MethodMetrics getMethodMetrics(String methodName) {
        return performanceAspect.getMetrics(methodName);
    }

    /**
     * Get metrics summary
     */
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
        summary.put("timestamp", new Date());

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
}