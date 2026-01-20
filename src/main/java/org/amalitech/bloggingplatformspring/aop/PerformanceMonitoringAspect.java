package org.amalitech.bloggingplatformspring.aop;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aspect for performance monitoring and metrics collection.
 * Tracks execution times, call counts, and performance thresholds.
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
        if (executionTime < 100) return "FAST";
        if (executionTime < 500) return "NORMAL";
        if (executionTime < 1000) return "SLOW";
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
     * Print performance summary
     */
    public void printPerformanceSummary() {
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
     * Inner class to store method performance metrics
     */
    public static class MethodMetrics {
        @Getter
        private final String methodName;
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong successfulCalls = new AtomicLong(0);
        private final AtomicLong failedCalls = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
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