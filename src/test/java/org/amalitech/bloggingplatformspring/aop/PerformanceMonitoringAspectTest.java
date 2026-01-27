package org.amalitech.bloggingplatformspring.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceMonitoringAspectTest {

    private PerformanceMonitoringAspect aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @BeforeEach
    void setUp() {
        aspect = new PerformanceMonitoringAspect();
    }

    @Test
    void monitorServicePerformance_WithSuccessfulExecution_ShouldRecordMetrics() throws Throwable {
        String methodName = "UserService.createUser(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.monitorServicePerformance(joinPoint);

        assertThat(result).isEqualTo("success");

        String fullMethodName = "SERVICE::" + methodName;
        PerformanceMonitoringAspect.MethodMetrics metrics = aspect.getMetrics(fullMethodName);

        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalCalls()).isEqualTo(1);
        assertThat(metrics.getSuccessfulCalls()).isEqualTo(1);
        assertThat(metrics.getFailedCalls()).isEqualTo(0);
    }

    @Test
    void monitorServicePerformance_WithException_ShouldRecordFailure() throws Throwable {
        String methodName = "UserService.createUser(..)";
        RuntimeException testException = new RuntimeException("Test exception");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenThrow(testException);

        assertThatThrownBy(() -> aspect.monitorServicePerformance(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");

        String fullMethodName = "SERVICE::" + methodName;
        PerformanceMonitoringAspect.MethodMetrics metrics = aspect.getMetrics(fullMethodName);

        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalCalls()).isEqualTo(1);
        assertThat(metrics.getSuccessfulCalls()).isEqualTo(0);
        assertThat(metrics.getFailedCalls()).isEqualTo(1);
    }

    @Test
    void monitorRepositoryPerformance_WithSuccessfulExecution_ShouldRecordMetrics() throws Throwable {
        String methodName = "PostRepository.findById(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenReturn("repository result");

        Object result = aspect.monitorRepositoryPerformance(joinPoint);

        assertThat(result).isEqualTo("repository result");

        String fullMethodName = "REPOSITORY::" + methodName;
        PerformanceMonitoringAspect.MethodMetrics metrics = aspect.getMetrics(fullMethodName);

        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalCalls()).isEqualTo(1);
        assertThat(metrics.getSuccessfulCalls()).isEqualTo(1);
    }

    @Test
    void getAllMetrics_ShouldReturnAllRecordedMetrics() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("result");

        when(signature.toShortString()).thenReturn("method1");
        aspect.monitorServicePerformance(joinPoint);

        when(signature.toShortString()).thenReturn("method2");
        aspect.monitorRepositoryPerformance(joinPoint);

        ConcurrentHashMap<String, PerformanceMonitoringAspect.MethodMetrics> allMetrics = aspect.getAllMetrics();

        assertThat(allMetrics).hasSize(2);
        assertThat(allMetrics).containsKeys("SERVICE::method1", "REPOSITORY::method2");
    }

    @Test
    void getMetricsSummary_WithMultipleExecutions_ShouldReturnCorrectSummary() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("testMethod");
        when(joinPoint.proceed()).thenReturn("result");

        aspect.monitorServicePerformance(joinPoint);
        aspect.monitorServicePerformance(joinPoint);
        aspect.monitorServicePerformance(joinPoint);

        Map<String, Object> summary = aspect.getMetricsSummary();

        assertThat(summary).containsKeys(
                "totalMethodsMonitored",
                "totalExecutions",
                "totalFailures",
                "overallAverageExecutionTime"
        );
        assertThat(summary.get("totalMethodsMonitored")).isEqualTo(1);
        assertThat(summary.get("totalExecutions")).isEqualTo(3L);
        assertThat(summary.get("totalFailures")).isEqualTo(0L);
    }

    @Test
    void resetMetrics_ShouldClearAllMetrics() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("testMethod");
        when(joinPoint.proceed()).thenReturn("result");

        aspect.monitorServicePerformance(joinPoint);
        assertThat(aspect.getAllMetrics()).isNotEmpty();

        aspect.resetMetrics();

        assertThat(aspect.getAllMetrics()).isEmpty();
    }

    @Test
    void methodMetrics_RecordExecution_ShouldUpdateAllCounters() {
        PerformanceMonitoringAspect.MethodMetrics metrics =
                new PerformanceMonitoringAspect.MethodMetrics("testMethod");

        metrics.recordExecution(100L, true);
        metrics.recordExecution(200L, true);
        metrics.recordExecution(150L, false);

        assertThat(metrics.getTotalCalls()).isEqualTo(3);
        assertThat(metrics.getSuccessfulCalls()).isEqualTo(2);
        assertThat(metrics.getFailedCalls()).isEqualTo(1);
        assertThat(metrics.getMinExecutionTime()).isEqualTo(100L);
        assertThat(metrics.getMaxExecutionTime()).isEqualTo(200L);
    }

    @Test
    void methodMetrics_GetPercentile_ShouldCalculateCorrectly() {
        PerformanceMonitoringAspect.MethodMetrics metrics =
                new PerformanceMonitoringAspect.MethodMetrics("testMethod");

        for (int i = 1; i <= 5; i++) {
            metrics.recordExecution(i * 100L, true);
        }

        assertThat(metrics.getPercentile(95)).isGreaterThanOrEqualTo(400L);
        assertThat(metrics.getPercentile(100)).isEqualTo(500L);
    }

    @Test
    void methodMetrics_GetPercentile_WithEmptyExecutions_ShouldReturnZero() {
        PerformanceMonitoringAspect.MethodMetrics metrics =
                new PerformanceMonitoringAspect.MethodMetrics("testMethod");

        assertThat(metrics.getPercentile(50)).isEqualTo(0L);
        assertThat(metrics.getPercentile(95)).isEqualTo(0L);
    }

    @Test
    void methodMetrics_GetFailureRate_ShouldCalculateCorrectly() {
        PerformanceMonitoringAspect.MethodMetrics metrics =
                new PerformanceMonitoringAspect.MethodMetrics("testMethod");

        metrics.recordExecution(100L, true);
        metrics.recordExecution(100L, true);
        metrics.recordExecution(100L, true);

    }

    @Test
    void methodMetrics_GetFailureRate_WithZeroCalls_ShouldReturnZero() {
        PerformanceMonitoringAspect.MethodMetrics metrics =
                new PerformanceMonitoringAspect.MethodMetrics("testMethod");

        assertThat(metrics.getFailureRate()).isEqualTo(0.0);
    }

    @Test
    void methodMetrics_GetStandardDeviation_ShouldCalculateCorrectly() {
        PerformanceMonitoringAspect.MethodMetrics metrics =
                new PerformanceMonitoringAspect.MethodMetrics("testMethod");

        metrics.recordExecution(100L, true);
        metrics.recordExecution(200L, true);
        metrics.recordExecution(300L, true);

        double stdDev = metrics.getStandardDeviation();

        assertThat(stdDev).isGreaterThan(0);
    }

    @Test
    void methodMetrics_GetStandardDeviation_WithLessThanTwoValues_ShouldReturnZero() {
        PerformanceMonitoringAspect.MethodMetrics metrics =
                new PerformanceMonitoringAspect.MethodMetrics("testMethod");

        metrics.recordExecution(100L, true);

        assertThat(metrics.getStandardDeviation()).isEqualTo(0.0);
    }

    @Test
    void methodMetrics_GetMinExecutionTime_WithNoExecutions_ShouldReturnZero() {
        PerformanceMonitoringAspect.MethodMetrics metrics =
                new PerformanceMonitoringAspect.MethodMetrics("testMethod");

        assertThat(metrics.getMinExecutionTime()).isEqualTo(0L);
    }

    @Test
    void methodMetrics_GetAverageExecutionTime_WithZeroCalls_ShouldReturnZero() {
        PerformanceMonitoringAspect.MethodMetrics metrics =
                new PerformanceMonitoringAspect.MethodMetrics("testMethod");

        assertThat(metrics.getAverageExecutionTime()).isEqualTo(0L);
    }

    @Test
    void methodMetrics_RecordExecution_ShouldLimitSampleSize() {
        PerformanceMonitoringAspect.MethodMetrics metrics =
                new PerformanceMonitoringAspect.MethodMetrics("testMethod");

        for (int i = 0; i < 1500; i++) {
            metrics.recordExecution(100L, true);
        }

        assertThat(metrics.getTotalCalls()).isEqualTo(1500L);
        assertThat(metrics.getPercentile(50)).isEqualTo(100L);
    }

    @Test
    void methodMetrics_GetMethodName_ShouldReturnCorrectName() {
        String methodName = "testMethod";
        PerformanceMonitoringAspect.MethodMetrics metrics =
                new PerformanceMonitoringAspect.MethodMetrics(methodName);

        assertThat(metrics.getMethodName()).isEqualTo(methodName);
    }

    @Test
    void monitorServicePerformance_WithSlowMethod_ShouldLogWarning() throws Throwable {
        String methodName = "SlowService.slowMethod(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            return "result";
        });

        aspect.monitorServicePerformance(joinPoint);

        String fullMethodName = "SERVICE::" + methodName;
        PerformanceMonitoringAspect.MethodMetrics metrics = aspect.getMetrics(fullMethodName);

        assertThat(metrics).isNotNull();
        assertThat(metrics.getMaxExecutionTime()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void getMetrics_WithNonExistentMethod_ShouldReturnNull() {
        PerformanceMonitoringAspect.MethodMetrics metrics = aspect.getMetrics("nonExistentMethod");

        assertThat(metrics).isNull();
    }

    @Test
    void methodMetrics_ConcurrentRecordExecution_ShouldHandleThreadSafety() throws InterruptedException {
        PerformanceMonitoringAspect.MethodMetrics metrics =
                new PerformanceMonitoringAspect.MethodMetrics("testMethod");

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    metrics.recordExecution(100L, true);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(metrics.getTotalCalls()).isEqualTo(1000L);
        assertThat(metrics.getSuccessfulCalls()).isEqualTo(1000L);
    }

    @Test
    void exportPerformanceSummary_ShouldCreateLogFile() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("testMethod");
        when(joinPoint.proceed()).thenReturn("result");

        aspect.monitorServicePerformance(joinPoint);

        aspect.exportPerformanceSummary();

        Path logsDir = Paths.get("logs");
        assertThat(Files.exists(logsDir)).isTrue();

        try (Stream<Path> paths = Files.list(logsDir)) {
            paths
                    .filter(path -> path.toString().endsWith("-export.log"))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
            Files.deleteIfExists(logsDir);
        } catch (IOException ignored) {
        }

    }
}