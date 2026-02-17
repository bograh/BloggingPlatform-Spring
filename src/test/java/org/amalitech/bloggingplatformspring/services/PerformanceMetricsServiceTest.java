package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect;
import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect.MethodMetrics;
import org.amalitech.bloggingplatformspring.dtos.responses.AllMetricsDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.MethodMetricsDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.MetricsSummaryDTO;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        AllMetricsDTO result = performanceMetricsService.getAllMetrics();

        assertThat(result).isNotNull();
        assertThat(result.totalMethods()).isEqualTo(2);
        assertThat(result.timestamp()).isInstanceOf(LocalDateTime.class);
        assertThat(result.metrics()).isNotNull();
        assertThat(result.metrics()).hasSize(2);

        verify(performanceAspect).getAllMetrics();
    }

    @Test
    void getAllMetrics_ShouldReturnEmptyMetrics_WhenNoMetricsExist() {
        when(performanceAspect.getAllMetrics()).thenReturn(new ConcurrentHashMap<>());

        AllMetricsDTO result = performanceMetricsService.getAllMetrics();

        assertThat(result).isNotNull();
        assertThat(result.totalMethods()).isEqualTo(0);
        assertThat(result.timestamp()).isInstanceOf(LocalDateTime.class);
        assertThat(result.metrics()).isNotNull();
        assertThat(result.metrics()).isEmpty();
    }

    @Test
    void getMethodMetrics_ShouldReturnMethodMetrics_WhenMethodExists() {
        String methodName = "UserService.getUser(..)";
        when(performanceAspect.getMetrics(methodName)).thenReturn(mockMetrics1);

        MethodMetricsDTO result = performanceMetricsService.getMethodMetrics(methodName);

        assertThat(result).isNotNull();
        assertThat(result.methodName()).isEqualTo(methodName);
        assertThat(result.totalCalls()).isEqualTo(3);
        assertThat(result.successfulCalls()).isEqualTo(3);
        assertThat(result.failedCalls()).isEqualTo(0);

        verify(performanceAspect).getMetrics(methodName);
    }

    @Test
    void getMethodMetrics_ShouldThrowException_WhenMethodDoesNotExist() {
        String methodName = "NonExistentService.method(..)";
        when(performanceAspect.getMetrics(methodName)).thenReturn(null);

        assertThatThrownBy(() -> performanceMetricsService.getMethodMetrics(methodName))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Method metrics not found");

        verify(performanceAspect).getMetrics(methodName);
    }

    @Test
    void getMetricsSummary_ShouldCalculateCorrectSummary_WhenMetricsExist() {
        mockMetricsMap.put("UserService.getUser(..)", mockMetrics1);
        mockMetricsMap.put("PostService.createPost(..)", mockMetrics2);
        mockMetricsMap.put("CommentService.deleteComment(..)", mockMetrics3);

        when(performanceAspect.getAllMetrics()).thenReturn(mockMetricsMap);

        MetricsSummaryDTO summary = performanceMetricsService.getMetricsSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.totalMethodsMonitored()).isEqualTo(3);
        assertThat(summary.totalExecutions()).isEqualTo(9L);
        assertThat(summary.totalFailures()).isEqualTo(2L);

        String avgTime = summary.overallAverageExecutionTime();
        assertThat(avgTime).matches("\\d+\\.\\d{2} ms");

        assertThat(summary.timestamp()).isInstanceOf(LocalDateTime.class);

        verify(performanceAspect).getAllMetrics();
    }

    @Test
    void getMetricsSummary_ShouldReturnZeroValues_WhenNoMetricsExist() {
        when(performanceAspect.getAllMetrics()).thenReturn(new ConcurrentHashMap<>());

        MetricsSummaryDTO summary = performanceMetricsService.getMetricsSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.totalMethodsMonitored()).isEqualTo(0);
        assertThat(summary.totalExecutions()).isEqualTo(0L);
        assertThat(summary.totalFailures()).isEqualTo(0L);
        assertThat(summary.overallAverageExecutionTime()).isEqualTo("0.00 ms");
        assertThat(summary.timestamp()).isInstanceOf(LocalDateTime.class);
    }

    @Test
    void getMetricsSummary_ShouldHandleSingleMethod() {
        mockMetricsMap.put("UserService.getUser(..)", mockMetrics1);
        when(performanceAspect.getAllMetrics()).thenReturn(mockMetricsMap);

        MetricsSummaryDTO summary = performanceMetricsService.getMetricsSummary();

        assertThat(summary.totalMethodsMonitored()).isEqualTo(1);
        assertThat(summary.totalExecutions()).isEqualTo(3L);
        assertThat(summary.totalFailures()).isEqualTo(0L);

        String avgTime = summary.overallAverageExecutionTime();
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

        MetricsSummaryDTO summary = performanceMetricsService.getMetricsSummary();

        String avgTime = summary.overallAverageExecutionTime();
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

        MetricsSummaryDTO summary = performanceMetricsService.getMetricsSummary();

        assertThat(summary.totalExecutions()).isEqualTo(3L);
        assertThat(summary.totalFailures()).isEqualTo(3L);
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
        LocalDateTime beforeCall = LocalDateTime.now();

        AllMetricsDTO result = performanceMetricsService.getAllMetrics();

        LocalDateTime afterCall = LocalDateTime.now();
        LocalDateTime timestamp = result.timestamp();

        assertThat(timestamp).isNotNull();
        assertThat(timestamp).isBetween(beforeCall, afterCall);
    }

    @Test
    void getMetricsSummary_ShouldContainTimestampWithinReasonableTimeRange() {
        when(performanceAspect.getAllMetrics()).thenReturn(new ConcurrentHashMap<>());
        LocalDateTime beforeCall = LocalDateTime.now();

        MetricsSummaryDTO summary = performanceMetricsService.getMetricsSummary();

        LocalDateTime afterCall = LocalDateTime.now();
        LocalDateTime timestamp = summary.timestamp();

        assertThat(timestamp).isNotNull();
        assertThat(timestamp).isBetween(beforeCall, afterCall);
    }

    @Test
    void getMetricsSummary_ShouldFormatAverageExecutionTimeWithTwoDecimals() {
        MethodMetrics metrics = new MethodMetrics("TestMethod");
        metrics.recordExecution(333, true);

        mockMetricsMap.put("TestMethod", metrics);
        when(performanceAspect.getAllMetrics()).thenReturn(mockMetricsMap);

        MetricsSummaryDTO summary = performanceMetricsService.getMetricsSummary();

        String avgTime = summary.overallAverageExecutionTime();
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

        AllMetricsDTO result = performanceMetricsService.getAllMetrics();

        assertThat(result.totalMethods()).isEqualTo(100);
        assertThat(result.metrics()).hasSize(100);
    }

    @Test
    void getMetricsSummary_ShouldWorkWithLargeNumberOfExecutions() {
        MethodMetrics heavilyUsedMethod = new MethodMetrics("PopularMethod");
        for (int i = 0; i < 1000; i++) {
            heavilyUsedMethod.recordExecution(50 + (i % 100), i % 10 != 0);
        }

        mockMetricsMap.put("PopularMethod", heavilyUsedMethod);
        when(performanceAspect.getAllMetrics()).thenReturn(mockMetricsMap);

        MetricsSummaryDTO summary = performanceMetricsService.getMetricsSummary();

        assertThat(summary.totalExecutions()).isEqualTo(1000L);
        assertThat(summary.totalFailures()).isEqualTo(100L);
        assertThat(summary.overallAverageExecutionTime()).isNotNull();
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