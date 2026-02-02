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