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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceMetricsServiceTest {

    @Mock
    private PerformanceMonitoringAspect performanceAspect;

    @InjectMocks
    private PerformanceMetricsService performanceMetricsService;

    private ConcurrentHashMap<String, MethodMetrics> mockMetricsMap;
    private MethodMetrics mockMetrics1;
    private MethodMetrics mockMetrics2;
    private MethodMetrics mockMetrics3;

    @BeforeEach
    void setUp() {
        mockMetricsMap = new ConcurrentHashMap<>();

        mockMetrics1 = new MethodMetrics("UserService.getUser(..)");
        mockMetrics1.recordExecution(100, true);
        mockMetrics1.recordExecution(150, true);
        mockMetrics1.recordExecution(200, true);

        mockMetrics2 = new MethodMetrics("PostService.createPost(..)");
        mockMetrics2.recordExecution(250, true);
        mockMetrics2.recordExecution(300, false);

        mockMetrics3 = new MethodMetrics("CommentService.deleteComment(..)");
        mockMetrics3.recordExecution(50, true);
        mockMetrics3.recordExecution(75, true);
        mockMetrics3.recordExecution(100, false);
        mockMetrics3.recordExecution(125, true);
    }

    @Test
    void getAllMetrics_ShouldReturnAllMetricsWithMetadata_WhenMetricsExist() {
        mockMetricsMap.put("UserService.getUser(..)", mockMetrics1);
        mockMetricsMap.put("PostService.createPost(..)", mockMetrics2);

        when(performanceAspect.getAllMetrics()).thenReturn(mockMetricsMap);

        Map<String, Object> result = performanceMetricsService.getAllMetrics();

        assertThat(result).isNotNull();
        assertThat(result).containsKeys("totalMethods", "timestamp", "metrics");
        assertThat(result.get("totalMethods")).isEqualTo(2);
        assertThat(result.get("timestamp")).isInstanceOf(Date.class);
        assertThat(result.get("metrics")).isEqualTo(mockMetricsMap);

        verify(performanceAspect).getAllMetrics();
    }

    @Test
    void getAllMetrics_ShouldReturnEmptyMetrics_WhenNoMetricsExist() {
        when(performanceAspect.getAllMetrics()).thenReturn(new ConcurrentHashMap<>());

        Map<String, Object> result = performanceMetricsService.getAllMetrics();

        assertThat(result).isNotNull();
        assertThat(result.get("totalMethods")).isEqualTo(0);
        assertThat(result.get("timestamp")).isInstanceOf(Date.class);
        assertThat(result.get("metrics")).isInstanceOf(ConcurrentHashMap.class);

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, MethodMetrics> metrics =
                (ConcurrentHashMap<String, MethodMetrics>) result.get("metrics");
        assertThat(metrics).isEmpty();
    }

    @Test
    void getMethodMetrics_ShouldReturnMethodMetrics_WhenMethodExists() {
        String methodName = "UserService.getUser(..)";
        when(performanceAspect.getMetrics(methodName)).thenReturn(mockMetrics1);

        MethodMetrics result = performanceMetricsService.getMethodMetrics(methodName);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockMetrics1);
        assertThat(result.getMethodName()).isEqualTo(methodName);
        assertThat(result.getTotalCalls()).isEqualTo(3);

        verify(performanceAspect).getMetrics(methodName);
    }

    @Test
    void getMethodMetrics_ShouldReturnNull_WhenMethodDoesNotExist() {
        String methodName = "NonExistentService.method(..)";
        when(performanceAspect.getMetrics(methodName)).thenReturn(null);

        MethodMetrics result = performanceMetricsService.getMethodMetrics(methodName);

        assertThat(result).isNull();
        verify(performanceAspect).getMetrics(methodName);
    }

    @Test
    void getMetricsSummary_ShouldCalculateCorrectSummary_WhenMetricsExist() {
        mockMetricsMap.put("UserService.getUser(..)", mockMetrics1);
        mockMetricsMap.put("PostService.createPost(..)", mockMetrics2);
        mockMetricsMap.put("CommentService.deleteComment(..)", mockMetrics3);

        when(performanceAspect.getAllMetrics()).thenReturn(mockMetricsMap);

        Map<String, Object> summary = performanceMetricsService.getMetricsSummary();

        assertThat(summary).isNotNull();
        assertThat(summary).containsKeys(
                "totalMethodsMonitored",
                "totalExecutions",
                "totalFailures",
                "overallAverageExecutionTime",
                "timestamp"
        );

        assertThat(summary.get("totalMethodsMonitored")).isEqualTo(3);

        assertThat(summary.get("totalExecutions")).isEqualTo(9L);

        assertThat(summary.get("totalFailures")).isEqualTo(2L);

        String avgTime = (String) summary.get("overallAverageExecutionTime");
        assertThat(avgTime).matches("\\d+\\.\\d{2} ms");

        assertThat(summary.get("timestamp")).isInstanceOf(Date.class);

        verify(performanceAspect).getAllMetrics();
    }

    @Test
    void getMetricsSummary_ShouldReturnZeroValues_WhenNoMetricsExist() {
        when(performanceAspect.getAllMetrics()).thenReturn(new ConcurrentHashMap<>());

        Map<String, Object> summary = performanceMetricsService.getMetricsSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.get("totalMethodsMonitored")).isEqualTo(0);
        assertThat(summary.get("totalExecutions")).isEqualTo(0L);
        assertThat(summary.get("totalFailures")).isEqualTo(0L);
        assertThat(summary.get("overallAverageExecutionTime")).isEqualTo("0.00 ms");
        assertThat(summary.get("timestamp")).isInstanceOf(Date.class);
    }

    @Test
    void getMetricsSummary_ShouldHandleSingleMethod() {
        mockMetricsMap.put("UserService.getUser(..)", mockMetrics1);
        when(performanceAspect.getAllMetrics()).thenReturn(mockMetricsMap);

        Map<String, Object> summary = performanceMetricsService.getMetricsSummary();

        assertThat(summary.get("totalMethodsMonitored")).isEqualTo(1);
        assertThat(summary.get("totalExecutions")).isEqualTo(3L);
        assertThat(summary.get("totalFailures")).isEqualTo(0L);

        String avgTime = (String) summary.get("overallAverageExecutionTime");
        assertThat(avgTime).isEqualTo("150.00 ms");
    }

    @Test
    void getMetricsSummary_ShouldCalculateCorrectAverageAcrossMultipleMethods() {
        MethodMetrics metrics1 = new MethodMetrics("Method1");
        metrics1.recordExecution(100, true);
        metrics1.recordExecution(200, true);

        MethodMetrics metrics2 = new MethodMetrics("Method2");
        metrics2.recordExecution(300, true);
        metrics2.recordExecution(500, true);

        mockMetricsMap.put("Method1", metrics1);
        mockMetricsMap.put("Method2", metrics2);

        when(performanceAspect.getAllMetrics()).thenReturn(mockMetricsMap);

        Map<String, Object> summary = performanceMetricsService.getMetricsSummary();

        String avgTime = (String) summary.get("overallAverageExecutionTime");
        assertThat(avgTime).isEqualTo("275.00 ms");
    }

    @Test
    void getMetricsSummary_ShouldCountAllFailuresCorrectly() {
        MethodMetrics failingMetrics = new MethodMetrics("FailingMethod");
        failingMetrics.recordExecution(100, false);
        failingMetrics.recordExecution(200, false);
        failingMetrics.recordExecution(150, false);

        mockMetricsMap.put("FailingMethod", failingMetrics);
        when(performanceAspect.getAllMetrics()).thenReturn(mockMetricsMap);

        Map<String, Object> summary = performanceMetricsService.getMetricsSummary();

        assertThat(summary.get("totalExecutions")).isEqualTo(3L);
        assertThat(summary.get("totalFailures")).isEqualTo(3L);
    }

    @Test
    void resetMetrics_ShouldDelegateToPerformanceAspect() {
        performanceMetricsService.resetMetrics();

        verify(performanceAspect).resetMetrics();
    }

    @Test
    void resetMetrics_ShouldBeCalledOnce() {
        performanceMetricsService.resetMetrics();

        verify(performanceAspect, times(1)).resetMetrics();
        verifyNoMoreInteractions(performanceAspect);
    }

    @Test
    void exportPerformanceSummary_ShouldDelegateToPerformanceAspect() {
        performanceMetricsService.exportPerformanceSummary();

        verify(performanceAspect).exportPerformanceSummary();
    }

    @Test
    void exportPerformanceSummary_ShouldBeCalledOnce() {
        performanceMetricsService.exportPerformanceSummary();

        verify(performanceAspect, times(1)).exportPerformanceSummary();
        verifyNoMoreInteractions(performanceAspect);
    }

    @Test
    void getAllMetrics_ShouldContainTimestampWithinReasonableTimeRange() {
        when(performanceAspect.getAllMetrics()).thenReturn(new ConcurrentHashMap<>());
        Date beforeCall = new Date();

        Map<String, Object> result = performanceMetricsService.getAllMetrics();

        Date afterCall = new Date();
        Date timestamp = (Date) result.get("timestamp");

        assertThat(timestamp).isNotNull();
        assertThat(timestamp.getTime()).isBetween(beforeCall.getTime(), afterCall.getTime());
    }

    @Test
    void getMetricsSummary_ShouldContainTimestampWithinReasonableTimeRange() {
        when(performanceAspect.getAllMetrics()).thenReturn(new ConcurrentHashMap<>());
        Date beforeCall = new Date();

        Map<String, Object> summary = performanceMetricsService.getMetricsSummary();

        Date afterCall = new Date();
        Date timestamp = (Date) summary.get("timestamp");

        assertThat(timestamp).isNotNull();
        assertThat(timestamp.getTime()).isBetween(beforeCall.getTime(), afterCall.getTime());
    }

    @Test
    void getMetricsSummary_ShouldFormatAverageExecutionTimeWithTwoDecimals() {
        MethodMetrics metrics = new MethodMetrics("TestMethod");
        metrics.recordExecution(333, true);

        mockMetricsMap.put("TestMethod", metrics);
        when(performanceAspect.getAllMetrics()).thenReturn(mockMetricsMap);

        Map<String, Object> summary = performanceMetricsService.getMetricsSummary();

        String avgTime = (String) summary.get("overallAverageExecutionTime");
        assertThat(avgTime).matches("\\d+\\.\\d{2} ms");
        assertThat(avgTime).isEqualTo("333.00 ms");
    }

    @Test
    void getAllMetrics_ShouldWorkWithLargeNumberOfMethods() {
        ConcurrentHashMap<String, MethodMetrics> largeMetricsMap = new ConcurrentHashMap<>();
        for (int i = 0; i < 100; i++) {
            MethodMetrics metrics = new MethodMetrics("Method" + i);
            metrics.recordExecution(100 + i, true);
            largeMetricsMap.put("Method" + i, metrics);
        }

        when(performanceAspect.getAllMetrics()).thenReturn(largeMetricsMap);

        Map<String, Object> result = performanceMetricsService.getAllMetrics();

        assertThat(result.get("totalMethods")).isEqualTo(100);

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, MethodMetrics> metrics =
                (ConcurrentHashMap<String, MethodMetrics>) result.get("metrics");
        assertThat(metrics).hasSize(100);
    }

    @Test
    void getMetricsSummary_ShouldWorkWithLargeNumberOfExecutions() {
        MethodMetrics heavilyUsedMethod = new MethodMetrics("PopularMethod");
        for (int i = 0; i < 1000; i++) {
            heavilyUsedMethod.recordExecution(50 + (i % 100), i % 10 != 0);
        }

        mockMetricsMap.put("PopularMethod", heavilyUsedMethod);
        when(performanceAspect.getAllMetrics()).thenReturn(mockMetricsMap);

        Map<String, Object> summary = performanceMetricsService.getMetricsSummary();

        assertThat(summary.get("totalExecutions")).isEqualTo(1000L);
        assertThat(summary.get("totalFailures")).isEqualTo(100L);
        assertThat(summary.get("overallAverageExecutionTime")).isNotNull();
    }

    @Test
    void getMethodMetrics_ShouldVerifyCorrectMethodNamePassedToAspect() {
        String expectedMethodName = "SpecificService.specificMethod(..)";
        when(performanceAspect.getMetrics(expectedMethodName)).thenReturn(mockMetrics1);

        performanceMetricsService.getMethodMetrics(expectedMethodName);

        verify(performanceAspect).getMetrics(expectedMethodName);
        verify(performanceAspect, never()).getMetrics(argThat(arg -> !arg.equals(expectedMethodName)));
    }
}