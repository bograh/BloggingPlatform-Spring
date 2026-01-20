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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aspect for performance monitoring and metrics collection.
 * Tracks execution times, call counts, percentiles, and performance thresholds.
 */
@Slf4j
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final long SLOW_THRESHOLD_MS = 1000; // 1 second
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConcurrentHashMap<String, MethodMetrics> metricsMap = new ConcurrentHashMap<>();

    /**
     * Pointcut for all service layer methods
     */
    @Pointcut("execution(* org.amalitech.bloggingplatformspring.services..*(..))")
    public void serviceMethods() {
    }

    /**
     * Pointcut for repository methods
     */
    @Pointcut("execution(* org.amalitech.bloggingplatformspring.repository..*(..))")
    public void repositoryMethods() {
    }

    /**
     * Around advice for performance monitoring of service methods
     */
    @Around("serviceMethods()")
    public Object monitorServicePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorMethodPerformance(joinPoint, "SERVICE");
    }

    /**
     * Around advice for performance monitoring of repository methods
     */
    @Around("repositoryMethods()")
    public Object monitorRepositoryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorMethodPerformance(joinPoint, "REPOSITORY");
    }

    /**
     * Core performance monitoring logic
     */
    private Object monitorMethodPerformance(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String fullMethodName = layer + "::" + methodName;

        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();

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
            long endMemory = getUsedMemory();
            long memoryUsed = endMemory - startMemory;

            updateMetrics(fullMethodName, executionTime, exception == null);

            logPerformance(fullMethodName, executionTime, memoryUsed, exception);

            if (executionTime > SLOW_THRESHOLD_MS) {
                log.warn("[PERFORMANCE] SLOW {} OPERATION DETECTED: {} took {} ms",
                        layer, methodName, executionTime);
            }
        }
    }

    /**
     * Log detailed performance information
     */
    private void logPerformance(String methodName, long executionTime, long memoryUsed, Throwable exception) {
        String status = exception == null ? "SUCCESS" : "FAILED";
        String timestamp = LocalDateTime.now().format(formatter);

        log.info("[PERFORMANCE] {} | {} | Method: {} | Execution Time: {} ms | Memory: {} KB | Status: {}",
                timestamp,
                getPerformanceLevel(executionTime),
                methodName,
                executionTime,
                memoryUsed / 1024,
                status);
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
     * Get performance level based on execution time
     */
    private String getPerformanceLevel(long executionTime) {
        if (executionTime < 100)
            return "FAST";
        if (executionTime < 500)
            return "NORMAL";
        if (executionTime < 1000)
            return "SLOW";
        return "CRITICAL";
    }

    /**
     * Get current memory usage
     */
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
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
     * Export performance summary
     */
    public void exportPerformanceSummary() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path logFilePath = Paths.get("logs", timestamp + "-export.log");

        try {
            // Create logs directory if it doesn't exist
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
                content.append("  P50 (Median): ").append(metrics.getPercentile(50)).append(" ms\n");
                content.append("  P95: ").append(metrics.getPercentile(95)).append(" ms\n");
                content.append("  P99: ").append(metrics.getPercentile(99)).append(" ms\n");
                content.append("-".repeat(80)).append("\n");
            });

            Files.writeString(logFilePath, content.toString());
            log.info("Performance summary exported to: {}", logFilePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to export performance summary to file", e);
        }
    }

    /**
     * Reset all metrics
     */
    public void resetMetrics() {
        metricsMap.clear();
        log.info("All performance metrics have been reset");
    }

    /**
     * Get metrics summary statistics
     */
    public Map<String, Object> getMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalMethodsMonitored", metricsMap.size());
        summary.put("totalExecutions", metricsMap.values().stream()
                .mapToLong(MethodMetrics::getTotalCalls)
                .sum());
        summary.put("totalFailures", metricsMap.values().stream()
                .mapToLong(MethodMetrics::getFailedCalls)
                .sum());

        OptionalDouble avgExecTime = metricsMap.values().stream()
                .mapToLong(MethodMetrics::getAverageExecutionTime)
                .average();
        summary.put("overallAverageExecutionTime",
                String.format("%.2f ms", avgExecTime.orElse(0.0)));

        return summary;
    }

    /**
     * Inner class to store method performance metrics
     */
    public static class MethodMetrics {
        @Getter
        private final String methodName;
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong successfulCalls = new AtomicLong(0);
        private final AtomicLong failedCalls = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final List<Long> executionTimes = Collections.synchronizedList(new ArrayList<>());
        private final int maxSampleSize = 1000;
        private volatile long minExecutionTime = Long.MAX_VALUE;
        @Getter
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

            // Store execution time for percentile calculation
            executionTimes.add(executionTime);

            // Keep only the last maxSampleSize executions to prevent memory issues
            if (executionTimes.size() > maxSampleSize) {
                executionTimes.removeFirst();
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

        /**
         * Calculate percentile value
         * 
         * @param percentile Percentile to calculate (e.g., 50, 95, 99)
         * @return Execution time at the given percentile
         */
        public long getPercentile(int percentile) {
            if (executionTimes.isEmpty()) {
                return 0;
            }

            List<Long> sortedTimes;
            synchronized (executionTimes) {
                sortedTimes = new ArrayList<>(executionTimes);
            }
            Collections.sort(sortedTimes);

            int index = (int) Math.ceil(percentile / 100.0 * sortedTimes.size()) - 1;
            index = Math.max(0, Math.min(index, sortedTimes.size() - 1));
            return sortedTimes.get(index);
        }

        /**
         * Get failure rate as a percentage
         */
        public double getFailureRate() {
            long total = totalCalls.get();
            return total > 0 ? (failedCalls.get() * 100.0) / total : 0.0;
        }

        /**
         * Get standard deviation of execution times
         */
        public double getStandardDeviation() {
            if (executionTimes.size() < 2) {
                return 0;
            }

            double mean = getAverageExecutionTime();
            double sumSquaredDiff;

            synchronized (executionTimes) {
                sumSquaredDiff = executionTimes.stream()
                        .mapToDouble(time -> Math.pow(time - mean, 2))
                        .sum();
            }

            return Math.sqrt(sumSquaredDiff / executionTimes.size());
        }

    }
}