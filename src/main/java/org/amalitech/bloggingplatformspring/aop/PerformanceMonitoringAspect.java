package org.amalitech.bloggingplatformspring.aop;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final long SLOW_THRESHOLD_MS = 1000; // 1 second

    private final ConcurrentHashMap<String, MethodMetrics> metricsMap = new ConcurrentHashMap<>();

    /**
     * Pointcut for all service layer methods
     */
    @Pointcut("execution(* org.amalitech.bloggingplatformspring.services..*(..))")
    public void serviceMethods() {
    }

    /**
     * Around advice for performance monitoring of service methods
     */
    @Around("serviceMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        Object result;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            updateMetrics(methodName, executionTime, exception == null);

            log.info("[PERFORMANCE] Method: {} | Execution Time: {} ms | Status: {}",
                    methodName,
                    executionTime,
                    exception == null ? "SUCCESS" : "FAILED");

            if (executionTime > SLOW_THRESHOLD_MS) {
                log.warn("[PERFORMANCE] SLOW OPERATION: {} took {} ms", methodName, executionTime);
            }
        }
    }

    /**
     * Update method execution metrics
     */
    private void updateMetrics(String methodName, long executionTime, boolean success) {
        metricsMap.compute(methodName, (key, metrics) -> {
            if (metrics == null) {
                metrics = new MethodMetrics(methodName);
            }
            metrics.recordExecution(executionTime, success);
            return metrics;
        });
    }

    /**
     * Get metrics for a specific method
     */
    public MethodMetrics getMetrics(String methodName) {
        return metricsMap.get(methodName);
    }

    /**
     * Get all collected metrics
     */
    public ConcurrentHashMap<String, MethodMetrics> getAllMetrics() {
        return new ConcurrentHashMap<>(metricsMap);
    }

    /**
     * Export performance summary to logs
     */
    public void exportPerformanceSummary() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path logFilePath = Paths.get("metrics", timestamp + "-performance-summary.log");
        logPerformanceMetrics();

        try {
            Files.createDirectories(logFilePath.getParent());

            StringBuilder content = new StringBuilder();
            content.append("=".repeat(80)).append("\n");
            content.append("PERFORMANCE METRICS SUMMARY").append("\n");
            content.append("=".repeat(80)).append("\n");

            metricsMap.forEach((methodName, metrics) -> {
                content.append("Method: ").append(methodName).append("\n");
                content.append("  Total Calls: ").append(metrics.getTotalCalls()).append("\n");
                content.append("  Successful: ").append(metrics.getSuccessfulCalls()).append("\n");
                content.append("  Failed: ").append(metrics.getFailedCalls()).append("\n");
                content.append("  Avg Execution Time: ").append(metrics.getAverageExecutionTime()).append(" ms\n");
                content.append("  Min Execution Time: ").append(metrics.getMinExecutionTime()).append(" ms\n");
                content.append("  Max Execution Time: ").append(metrics.getMaxExecutionTime()).append(" ms\n");
                content.append("-".repeat(80)).append("\n");
            });

            Files.writeString(logFilePath, content.toString());
            log.info("Performance summary exported to: {}", logFilePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to export performance summary to file", e);
        }
    }

    /**
     * Export cache metrics to file and logs
     */
    public void exportCacheMetricsToFile(Map<String, Object> cacheMetrics, Map<String, Object> cacheSummary) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path logFilePath = Paths.get("metrics", timestamp + "-cache-metrics.log");

        logCacheMetrics(cacheMetrics, cacheSummary);

        try {
            Files.createDirectories(logFilePath.getParent());
            StringBuilder content = buildCacheMetricsContent(cacheMetrics, cacheSummary);
            Files.writeString(logFilePath, content.toString());
            log.info("Cache metrics exported to: {}", logFilePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to export cache metrics to file", e);
        }
    }

    /**
     * Export combined performance and cache metrics to file and logs
     */
    public void exportCombinedMetrics(Map<String, Object> cacheMetrics, Map<String, Object> cacheSummary) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path logFilePath = Paths.get("metrics", timestamp + "-combined-metrics.log");

        // Log both to console
        logPerformanceMetrics();
        logCacheMetrics(cacheMetrics, cacheSummary);

        try {
            Files.createDirectories(logFilePath.getParent());

            StringBuilder content = new StringBuilder();

            // Performance metrics section
            content.append("=".repeat(80)).append("\n");
            content.append("PERFORMANCE METRICS SUMMARY").append("\n");
            content.append("=".repeat(80)).append("\n");

            metricsMap.forEach((methodName, metrics) -> {
                content.append("Method: ").append(methodName).append("\n");
                content.append("  Total Calls: ").append(metrics.getTotalCalls()).append("\n");
                content.append("  Successful: ").append(metrics.getSuccessfulCalls()).append("\n");
                content.append("  Failed: ").append(metrics.getFailedCalls()).append("\n");
                content.append("  Avg Execution Time: ").append(metrics.getAverageExecutionTime()).append(" ms\n");
                content.append("  Min Execution Time: ").append(metrics.getMinExecutionTime()).append(" ms\n");
                content.append("  Max Execution Time: ").append(metrics.getMaxExecutionTime()).append(" ms\n");
                content.append("-".repeat(80)).append("\n");
            });

            content.append("\n");

            // Cache metrics section
            content.append(buildCacheMetricsContent(cacheMetrics, cacheSummary));

            Files.writeString(logFilePath, content.toString());
            log.info("Combined metrics exported to: {}", logFilePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to export combined metrics to file", e);
        }
    }

    private StringBuilder buildCacheMetricsContent(Map<String, Object> cacheMetrics, Map<String, Object> cacheSummary) {
        StringBuilder content = new StringBuilder();
        content.append("=".repeat(80)).append("\n");
        content.append("CACHE METRICS SUMMARY").append("\n");
        content.append("=".repeat(80)).append("\n");

        // Cache summary
        content.append("\nOverall Cache Statistics:\n");
        content.append("  Total Caches: ").append(cacheSummary.get("totalCaches")).append("\n");
        content.append("  Total Hits: ").append(cacheSummary.get("totalHits")).append("\n");
        content.append("  Total Misses: ").append(cacheSummary.get("totalMisses")).append("\n");
        content.append("  Total Requests: ").append(cacheSummary.get("totalRequests")).append("\n");
        content.append("  Overall Hit Rate: ").append(cacheSummary.get("overallHitRate")).append("\n");
        content.append("  Total Puts: ").append(cacheSummary.get("totalPuts")).append("\n");
        content.append("  Total Evictions: ").append(cacheSummary.get("totalEvictions")).append("\n");

        if (cacheSummary.containsKey("bestPerformingCache")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> best = (Map<String, Object>) cacheSummary.get("bestPerformingCache");
            content.append("  Best Performing Cache: ").append(best.get("name"))
                    .append(" (").append(best.get("hitRate")).append(")\n");
        }

        if (cacheSummary.containsKey("worstPerformingCache")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> worst = (Map<String, Object>) cacheSummary.get("worstPerformingCache");
            content.append("  Worst Performing Cache: ").append(worst.get("name"))
                    .append(" (").append(worst.get("hitRate")).append(")\n");
        }

        content.append("\n").append("-".repeat(80)).append("\n");
        content.append("Individual Cache Details:\n");
        content.append("-".repeat(80)).append("\n");

        // Individual cache details
        @SuppressWarnings("unchecked")
        Map<String, Object> caches = (Map<String, Object>) cacheMetrics.get("caches");
        caches.forEach((cacheName, metricsObj) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> metrics = (Map<String, Object>) metricsObj;
            content.append("\nCache: ").append(cacheName).append("\n");
            content.append("  Hits: ").append(metrics.get("hits")).append("\n");
            content.append("  Misses: ").append(metrics.get("misses")).append("\n");
            content.append("  Hit Rate: ").append(metrics.get("hitRate")).append("\n");
            content.append("  Miss Rate: ").append(metrics.get("missRate")).append("\n");
            content.append("  Total Requests: ").append(metrics.get("totalRequests")).append("\n");
            content.append("  Puts: ").append(metrics.get("puts")).append("\n");
            content.append("  Evictions: ").append(metrics.get("evictions")).append("\n");
            content.append("  Clears: ").append(metrics.get("clears")).append("\n");
        });

        return content;
    }

    private void logCacheMetrics(Map<String, Object> cacheMetrics, Map<String, Object> cacheSummary) {
        log.info("=".repeat(80));
        log.info("CACHE METRICS SUMMARY");
        log.info("=".repeat(80));

        // Cache summary
        log.info("Overall Cache Statistics:");
        log.info("  Total Caches: {}", cacheSummary.get("totalCaches"));
        log.info("  Total Hits: {}", cacheSummary.get("totalHits"));
        log.info("  Total Misses: {}", cacheSummary.get("totalMisses"));
        log.info("  Total Requests: {}", cacheSummary.get("totalRequests"));
        log.info("  Overall Hit Rate: {}", cacheSummary.get("overallHitRate"));
        log.info("  Total Puts: {}", cacheSummary.get("totalPuts"));
        log.info("  Total Evictions: {}", cacheSummary.get("totalEvictions"));

        if (cacheSummary.containsKey("bestPerformingCache")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> best = (Map<String, Object>) cacheSummary.get("bestPerformingCache");
            log.info("  Best Performing Cache: {} ({})", best.get("name"), best.get("hitRate"));
        }

        if (cacheSummary.containsKey("worstPerformingCache")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> worst = (Map<String, Object>) cacheSummary.get("worstPerformingCache");
            log.info("  Worst Performing Cache: {} ({})", worst.get("name"), worst.get("hitRate"));
        }

        log.info("-".repeat(80));
        log.info("Individual Cache Details:");
        log.info("-".repeat(80));

        // Individual cache details
        @SuppressWarnings("unchecked")
        Map<String, Object> caches = (Map<String, Object>) cacheMetrics.get("caches");
        caches.forEach((cacheName, metricsObj) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> metrics = (Map<String, Object>) metricsObj;
            log.info("Cache: {}", cacheName);
            log.info("  Hits: {}", metrics.get("hits"));
            log.info("  Misses: {}", metrics.get("misses"));
            log.info("  Hit Rate: {}", metrics.get("hitRate"));
            log.info("  Miss Rate: {}", metrics.get("missRate"));
            log.info("  Total Requests: {}", metrics.get("totalRequests"));
            log.info("  Puts: {}", metrics.get("puts"));
            log.info("  Evictions: {}", metrics.get("evictions"));
            log.info("  Clears: {}", metrics.get("clears"));
        });
    }

    private void logPerformanceMetrics() {
        log.info("=".repeat(80));
        log.info("PERFORMANCE METRICS SUMMARY");
        log.info("=".repeat(80));

        metricsMap.forEach((methodName, metrics) -> {
            log.info("Method: {}", methodName);
            log.info("  Total Calls: {}", metrics.getTotalCalls());
            log.info("  Successful: {}", metrics.getSuccessfulCalls());
            log.info("  Failed: {}", metrics.getFailedCalls());
            log.info("  Avg Execution Time: {} ms", metrics.getAverageExecutionTime());
            log.info("  Min Execution Time: {} ms", metrics.getMinExecutionTime());
            log.info("  Max Execution Time: {} ms", metrics.getMaxExecutionTime());
            log.info("-".repeat(80));
        });
    }

    /**
     * Reset all metrics
     */
    public void resetMetrics() {
        metricsMap.clear();
        log.info("All performance metrics have been reset");
    }

    /**
     * Inner class to store method performance metrics
     */
    @Getter
    public static class MethodMetrics {
        private final String methodName;
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong successfulCalls = new AtomicLong(0);
        private final AtomicLong failedCalls = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private volatile long minExecutionTime = Long.MAX_VALUE;
        private volatile long maxExecutionTime = 0;

        public MethodMetrics(String methodName) {
            this.methodName = methodName;
        }

        public synchronized void recordExecution(long executionTime, boolean success) {
            totalCalls.incrementAndGet();
            if (success) {
                successfulCalls.incrementAndGet();
            } else {
                failedCalls.incrementAndGet();
            }
            totalExecutionTime.addAndGet(executionTime);

            if (executionTime < minExecutionTime) {
                minExecutionTime = executionTime;
            }
            if (executionTime > maxExecutionTime) {
                maxExecutionTime = executionTime;
            }
        }

        public long getTotalCalls() {
            return totalCalls.get();
        }

        public long getSuccessfulCalls() {
            return successfulCalls.get();
        }

        public long getFailedCalls() {
            return failedCalls.get();
        }

        public long getAverageExecutionTime() {
            long calls = totalCalls.get();
            return calls > 0 ? totalExecutionTime.get() / calls : 0;
        }

        public long getMinExecutionTime() {
            return minExecutionTime == Long.MAX_VALUE ? 0 : minExecutionTime;
        }

    }
}