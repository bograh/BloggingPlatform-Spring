package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect;
import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect.MethodMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceMetricsServiceTest {

    @Mock
    private PerformanceMonitoringAspect performanceAspect;

    @InjectMocks
    private PerformanceMetricsService service;

    private ConcurrentHashMap<String, MethodMetrics> testMetrics;

    @BeforeEach
    void setUp() {
        testMetrics = new ConcurrentHashMap<>();
    }

    @Test
    void getAllMetricsFormatted_shouldReturnFormattedMetrics() {
        MethodMetrics metrics1 = createMethodMetrics(10L, 100L, 500L, 250L, 5, 0);
        MethodMetrics metrics2 = createMethodMetrics(5L, 50L, 200L, 100L, 3, 0);
        testMetrics.put("Service::method1", metrics1);
        testMetrics.put("Controller::method2", metrics2);

        when(performanceAspect.getAllMetrics()).thenReturn(testMetrics);

        Map<String, Object> result = service.getAllMetricsFormatted();

        assertThat(result).containsKey("totalMethods");
        assertThat(result.get("totalMethods")).isEqualTo(2);
        assertThat(result).containsKey("timestamp");
        assertThat(result.get("timestamp")).isInstanceOf(Date.class);
        assertThat(result).containsKey("methods");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> methods = (List<Map<String, Object>>) result.get("methods");
        assertThat(methods).hasSize(2);

        assertThat(methods.get(0).get("method")).isEqualTo("Service::method1");
        assertThat(methods.get(1).get("method")).isEqualTo("Controller::method2");
    }

    @Test
    void getAllMetricsFormatted_withEmptyMetrics_shouldReturnEmptyList() {
        when(performanceAspect.getAllMetrics()).thenReturn(testMetrics);

        Map<String, Object> result = service.getAllMetricsFormatted();

        assertThat(result.get("totalMethods")).isEqualTo(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> methods = (List<Map<String, Object>>) result.get("methods");
        assertThat(methods).isEmpty();
    }

    @Test
    void getMethodMetricsFormatted_withExistingMethod_shouldReturnFormattedMetrics() {
        String methodName = "Service::testMethod";
        MethodMetrics metrics = createMethodMetrics(10L, 100L, 500L, 250L, 5, 0);
        when(performanceAspect.getMetrics(methodName)).thenReturn(metrics);

        Map<String, Object> result = service.getMethodMetricsFormatted(methodName);

        assertThat(result).containsEntry("method", methodName);
        assertThat(result).containsEntry("totalCalls", 5L);
        assertThat(result).containsEntry("avgExecutionTime", 250L);
        assertThat(result).containsEntry("minExecutionTime", 10L);
        assertThat(result).containsEntry("maxExecutionTime", 100L);
        assertThat(result).containsEntry("performanceLevel", "NORMAL");
        assertThat(result).containsEntry("unit", "ms");
    }

    @Test
    void getMethodMetricsFormatted_withNonExistingMethod_shouldReturnError() {
        String methodName = "NonExistent::method";
        when(performanceAspect.getMetrics(methodName)).thenReturn(null);

        Map<String, Object> result = service.getMethodMetricsFormatted(methodName);

        assertThat(result).containsEntry("error", "Method not found");
        assertThat(result).containsEntry("methodName", methodName);
    }

    @Test
    void getMetricsSummary_shouldReturnSummaryFromAspect() {
        Map<String, Object> expectedSummary = Map.of("key", "value");
        when(performanceAspect.getMetricsSummary()).thenReturn(expectedSummary);

        Map<String, Object> result = service.getMetricsSummary();

        assertThat(result).isEqualTo(expectedSummary);
        verify(performanceAspect).getMetricsSummary();
    }

    @Test
    void getSlowMethods_shouldReturnMethodsAboveThreshold() {
        MethodMetrics fastMetrics = createMethodMetrics(10L, 50L, 100L, 80L, 5, 0);
        MethodMetrics slowMetrics = createMethodMetrics(500L, 800L, 1200L, 900L, 3, 0);
        testMetrics.put("Fast::method", fastMetrics);
        testMetrics.put("Slow::method", slowMetrics);

        when(performanceAspect.getAllMetrics()).thenReturn(testMetrics);

        Map<String, Object> result = service.getSlowMethods(500L);

        assertThat(result).containsEntry("threshold", "500 ms");
        assertThat(result).containsEntry("count", 1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> methods = (List<Map<String, Object>>) result.get("methods");
        assertThat(methods).hasSize(1);
        assertThat(methods.get(0).get("method")).isEqualTo("Slow::method");
    }

    @Test
    void getSlowMethods_withNoSlowMethods_shouldReturnEmptyList() {
        MethodMetrics fastMetrics = createMethodMetrics(10L, 50L, 100L, 80L, 5, 0);
        testMetrics.put("Fast::method", fastMetrics);

        when(performanceAspect.getAllMetrics()).thenReturn(testMetrics);

        Map<String, Object> result = service.getSlowMethods(1000L);

        assertThat(result).containsEntry("count", 0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> methods = (List<Map<String, Object>>) result.get("methods");
        assertThat(methods).isEmpty();
    }

    @Test
    void getTopSlowMethods_shouldReturnLimitedSortedMethods() {
        MethodMetrics metrics1 = createMethodMetrics(100L, 200L, 300L, 200L, 5, 0);
        MethodMetrics metrics2 = createMethodMetrics(300L, 400L, 500L, 400L, 5, 0);
        MethodMetrics metrics3 = createMethodMetrics(500L, 600L, 700L, 600L, 5, 0);
        testMetrics.put("Method1", metrics1);
        testMetrics.put("Method2", metrics2);
        testMetrics.put("Method3", metrics3);

        when(performanceAspect.getAllMetrics()).thenReturn(testMetrics);

        Map<String, Object> result = service.getTopSlowMethods(2);

        assertThat(result).containsEntry("limit", 2);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> methods = (List<Map<String, Object>>) result.get("methods");
        assertThat(methods).hasSize(2);
        assertThat(methods.get(0).get("method")).isEqualTo("Method3");
        assertThat(methods.get(1).get("method")).isEqualTo("Method2");
    }

    @Test
    void getMetricsByLayer_shouldFilterByLayerPrefix() {
        MethodMetrics serviceMetrics = createMethodMetrics(100L, 200L, 300L, 200L, 5, 0);
        MethodMetrics controllerMetrics = createMethodMetrics(50L, 100L, 150L, 100L, 5, 0);
        testMetrics.put("Service::method1", serviceMetrics);
        testMetrics.put("Service::method2", serviceMetrics);
        testMetrics.put("Controller::method1", controllerMetrics);

        when(performanceAspect.getAllMetrics()).thenReturn(testMetrics);

        Map<String, Object> result = service.getMetricsByLayer("Service");

        assertThat(result).containsEntry("layer", "Service");
        assertThat(result).containsEntry("count", 2);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> methods = (List<Map<String, Object>>) result.get("methods");
        assertThat(methods).hasSize(2);
        assertThat(methods).allMatch(m -> ((String) m.get("method")).startsWith("Service::"));
    }

    @Test
    void getFailureStatistics_shouldReturnMethodsWithFailures() {
        MethodMetrics noFailures = createMethodMetrics(100L, 200L, 300L, 200L, 10, 0);
        MethodMetrics withFailures = createMethodMetrics(100L, 200L, 300L, 200L, 8, 2);
        testMetrics.put("Stable::method", noFailures);
        testMetrics.put("Failing::method", withFailures);

        when(performanceAspect.getAllMetrics()).thenReturn(testMetrics);

        Map<String, Object> result = service.getFailureStatistics();

        assertThat(result).containsEntry("totalFailures", 2L);
        assertThat(result).containsEntry("methodsWithFailures", 1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> methods = (List<Map<String, Object>>) result.get("methods");
        assertThat(methods).hasSize(1);
        assertThat(methods.get(0).get("method")).isEqualTo("Failing::method");
        assertThat(methods.get(0).get("failedCalls")).isEqualTo(2L);
    }

    @Test
    void resetMetrics_shouldDelegateToAspect() {
        service.resetMetrics();

        verify(performanceAspect).resetMetrics();
    }

    @Test
    void exportPerformanceSummary_shouldDelegateToAspect() {
        service.exportPerformanceSummary();

        verify(performanceAspect).exportPerformanceSummary();
    }

    @Test
    void formatMethodMetrics_shouldClassifyPerformanceCorrectly() {
        MethodMetrics fastMetrics = createMethodMetrics(10L, 50L, 90L, 50L, 5, 0);
        MethodMetrics normalMetrics = createMethodMetrics(100L, 200L, 400L, 250L, 5, 0);
        MethodMetrics slowMetrics = createMethodMetrics(500L, 700L, 900L, 700L, 5, 0);
        MethodMetrics criticalMetrics = createMethodMetrics(1000L, 1500L, 2000L, 1500L, 5, 0);

        testMetrics.put("Fast::method", fastMetrics);
        testMetrics.put("Normal::method", normalMetrics);
        testMetrics.put("Slow::method", slowMetrics);
        testMetrics.put("Critical::method", criticalMetrics);

        when(performanceAspect.getAllMetrics()).thenReturn(testMetrics);

        Map<String, Object> result = service.getAllMetricsFormatted();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> methods = (List<Map<String, Object>>) result.get("methods");

        Map<String, Object> fastMethod = methods.stream()
                .filter(m -> m.get("method").equals("Fast::method"))
                .findFirst().orElseThrow();
        assertThat(fastMethod.get("performanceLevel")).isEqualTo("FAST");

        Map<String, Object> normalMethod = methods.stream()
                .filter(m -> m.get("method").equals("Normal::method"))
                .findFirst().orElseThrow();
        assertThat(normalMethod.get("performanceLevel")).isEqualTo("NORMAL");

        Map<String, Object> slowMethod = methods.stream()
                .filter(m -> m.get("method").equals("Slow::method"))
                .findFirst().orElseThrow();
        assertThat(slowMethod.get("performanceLevel")).isEqualTo("SLOW");

        Map<String, Object> criticalMethod = methods.stream()
                .filter(m -> m.get("method").equals("Critical::method"))
                .findFirst().orElseThrow();
        assertThat(criticalMethod.get("performanceLevel")).isEqualTo("CRITICAL");
    }

    private MethodMetrics createMethodMetrics(long min, long max, long p99, long avg,
                                              long successfulCalls, long failedCalls) {
        MethodMetrics metrics = mock(MethodMetrics.class);
        lenient().when(metrics.getMinExecutionTime()).thenReturn(min);
        lenient().when(metrics.getMaxExecutionTime()).thenReturn(max);
        lenient().when(metrics.getAverageExecutionTime()).thenReturn(avg);
        lenient().when(metrics.getTotalCalls()).thenReturn(successfulCalls + failedCalls);
        lenient().when(metrics.getSuccessfulCalls()).thenReturn(successfulCalls);
        lenient().when(metrics.getFailedCalls()).thenReturn(failedCalls);
        lenient().when(metrics.getPercentile(50)).thenReturn(avg);
        lenient().when(metrics.getPercentile(95)).thenReturn((max + avg) / 2);
        lenient().when(metrics.getPercentile(99)).thenReturn(p99);
        lenient().when(metrics.getStandardDeviation()).thenReturn(50.0);

        double failureRate = failedCalls > 0
                ? (failedCalls * 100.0) / (successfulCalls + failedCalls)
                : 0.0;
        lenient().when(metrics.getFailureRate()).thenReturn(failureRate);

        return metrics;
    }
}