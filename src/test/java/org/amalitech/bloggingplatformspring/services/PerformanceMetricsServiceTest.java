package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceMetricsServiceTest {

    @Mock
    private PerformanceMonitoringAspect performanceAspect;

    @Mock
    private PerformanceMonitoringAspect.MethodMetrics fastMetrics;

    @Mock
    private PerformanceMonitoringAspect.MethodMetrics slowMetrics;

    @InjectMocks
    private PerformanceMetricsService service;

    private ConcurrentHashMap<String, PerformanceMonitoringAspect.MethodMetrics> metricsMap;

    @Test
    void getAllMetricsFormatted_sortsByAverageExecutionTime() {
        metricsMap = new ConcurrentHashMap<>();

        when(fastMetrics.getTotalCalls()).thenReturn(10L);
        when(fastMetrics.getSuccessfulCalls()).thenReturn(10L);
        when(fastMetrics.getFailedCalls()).thenReturn(0L);
        when(fastMetrics.getFailureRate()).thenReturn(0.0);
        when(fastMetrics.getAverageExecutionTime()).thenReturn(50L);
        when(fastMetrics.getMinExecutionTime()).thenReturn(20L);
        when(fastMetrics.getMaxExecutionTime()).thenReturn(80L);
        when(fastMetrics.getPercentile(anyInt())).thenReturn(50L);
        when(fastMetrics.getStandardDeviation()).thenReturn(5.0);

        when(slowMetrics.getTotalCalls()).thenReturn(5L);
        when(slowMetrics.getSuccessfulCalls()).thenReturn(3L);
        when(slowMetrics.getFailedCalls()).thenReturn(2L);
        when(slowMetrics.getFailureRate()).thenReturn(40.0);
        when(slowMetrics.getAverageExecutionTime()).thenReturn(1200L);
        when(slowMetrics.getMinExecutionTime()).thenReturn(800L);
        when(slowMetrics.getMaxExecutionTime()).thenReturn(1500L);
        when(slowMetrics.getPercentile(anyInt())).thenReturn(1200L);
        when(slowMetrics.getStandardDeviation()).thenReturn(100.0);

        metricsMap.put("service::fastMethod", fastMetrics);
        metricsMap.put("controller::slowMethod", slowMetrics);

        when(performanceAspect.getAllMetrics()).thenReturn(metricsMap);

        Map<String, Object> result = service.getAllMetricsFormatted();

        List<?> methods = (List<?>) result.get("methods");
        Map<?, ?> first = (Map<?, ?>) methods.getFirst();

        assertEquals("controller::slowMethod", first.get("method"));
        assertEquals("CRITICAL", first.get("performanceLevel"));
    }

    @Test
    void getMethodMetricsFormatted_methodExists() {
        when(fastMetrics.getTotalCalls()).thenReturn(1L);
        when(fastMetrics.getSuccessfulCalls()).thenReturn(1L);
        when(fastMetrics.getFailedCalls()).thenReturn(0L);
        when(fastMetrics.getFailureRate()).thenReturn(0.0);
        when(fastMetrics.getAverageExecutionTime()).thenReturn(30L);
        when(fastMetrics.getMinExecutionTime()).thenReturn(30L);
        when(fastMetrics.getMaxExecutionTime()).thenReturn(30L);
        when(fastMetrics.getPercentile(anyInt())).thenReturn(30L);
        when(fastMetrics.getStandardDeviation()).thenReturn(0.0);

        when(performanceAspect.getMetrics("service::fastMethod"))
                .thenReturn(fastMetrics);

        Map<String, Object> result =
                service.getMethodMetricsFormatted("service::fastMethod");

        assertEquals("FAST", result.get("performanceLevel"));
    }

    @Test
    void getMethodMetricsFormatted_methodNotFound() {
        when(performanceAspect.getMetrics("unknown")).thenReturn(null);

        Map<String, Object> result =
                service.getMethodMetricsFormatted("unknown");

        assertEquals("Method not found", result.get("error"));
    }

    @Test
    void resetMetrics_delegatesToAspect() {
        service.resetMetrics();
        verify(performanceAspect).resetMetrics();
    }

    @Test
    void exportPerformanceSummary_delegatesToAspect() {
        service.exportPerformanceSummary();
        verify(performanceAspect).exportPerformanceSummary();
    }
}