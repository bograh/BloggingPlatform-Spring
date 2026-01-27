package org.amalitech.bloggingplatformspring.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect.MethodMetrics;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceMonitoringAspectTest {

    @InjectMocks
    private PerformanceMonitoringAspect performanceMonitoringAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(PerformanceMonitoringAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        performanceMonitoringAspect.resetMetrics();
    }

    @Test
    void monitorPerformance_ShouldReturnResult_WhenMethodExecutesSuccessfully() throws Throwable {
        String methodName = "UserService.createUser(..)";
        String expectedResult = "User created";

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenReturn(expectedResult);

        Object result = performanceMonitoringAspect.monitorPerformance(joinPoint);

        assertThat(result).isEqualTo(expectedResult);
        verify(joinPoint).proceed();
    }

    @Test
    void monitorPerformance_ShouldLogPerformanceInfo_WhenMethodExecutesSuccessfully() throws Throwable {
        String methodName = "UserService.getUser(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenReturn("user");

        performanceMonitoringAspect.monitorPerformance(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSizeGreaterThanOrEqualTo(1);

        ILoggingEvent performanceLog = logsList.stream()
                .filter(log -> log.getFormattedMessage().contains("[PERFORMANCE]"))
                .findFirst()
                .orElse(null);

        assertThat(performanceLog).isNotNull();
        assertThat(performanceLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(performanceLog.getFormattedMessage())
                .contains("[PERFORMANCE]")
                .contains(methodName)
                .contains("SUCCESS");
    }

    @Test
    void monitorPerformance_ShouldLogWarning_WhenExecutionTimeExceedsThreshold() throws Throwable {
        String methodName = "UserService.slowMethod(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(1100);
            return "result";
        });

        performanceMonitoringAspect.monitorPerformance(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        ILoggingEvent slowOperationLog = logsList.stream()
                .filter(log -> log.getFormattedMessage().contains("SLOW OPERATION"))
                .findFirst()
                .orElse(null);

        assertThat(slowOperationLog).isNotNull();
        assertThat(slowOperationLog.getLevel()).isEqualTo(Level.WARN);
        assertThat(slowOperationLog.getFormattedMessage())
                .contains("SLOW OPERATION")
                .contains(methodName);
    }

    @Test
    void monitorPerformance_ShouldPropagateException_WhenMethodThrowsException() throws Throwable {
        String methodName = "UserService.deleteUser(..)";
        RuntimeException expectedException = new RuntimeException("User not found");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenThrow(expectedException);

        assertThatThrownBy(() -> performanceMonitoringAspect.monitorPerformance(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    @Test
    void monitorPerformance_ShouldLogFailedStatus_WhenMethodThrowsException() throws Throwable {
        String methodName = "PostService.createPost(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("Invalid data"));

        try {
            performanceMonitoringAspect.monitorPerformance(joinPoint);
        } catch (IllegalArgumentException e) {
        }

        List<ILoggingEvent> logsList = listAppender.list;
        ILoggingEvent performanceLog = logsList.stream()
                .filter(log -> log.getFormattedMessage().contains("[PERFORMANCE]"))
                .filter(log -> log.getFormattedMessage().contains("FAILED"))
                .findFirst()
                .orElse(null);

        assertThat(performanceLog).isNotNull();
        assertThat(performanceLog.getFormattedMessage())
                .contains(methodName)
                .contains("FAILED");
    }

    @Test
    void monitorPerformance_ShouldUpdateMetrics_WhenMethodExecutes() throws Throwable {
        String methodName = "CommentService.addComment(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenReturn("comment");

        performanceMonitoringAspect.monitorPerformance(joinPoint);

        MethodMetrics metrics = performanceMonitoringAspect.getMetrics(methodName);
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalCalls()).isEqualTo(1);
        assertThat(metrics.getSuccessfulCalls()).isEqualTo(1);
        assertThat(metrics.getFailedCalls()).isEqualTo(0);
    }

    @Test
    void getMetrics_ShouldReturnNull_WhenMethodHasNotBeenMonitored() {
        MethodMetrics metrics = performanceMonitoringAspect.getMetrics("NonExistentMethod");

        assertThat(metrics).isNull();
    }

    @Test
    void getAllMetrics_ShouldReturnCopyOfMetricsMap() throws Throwable {
        String methodName = "UserService.getUser(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenReturn("user");

        performanceMonitoringAspect.monitorPerformance(joinPoint);

        ConcurrentHashMap<String, MethodMetrics> allMetrics = performanceMonitoringAspect.getAllMetrics();

        assertThat(allMetrics).hasSize(1);
        assertThat(allMetrics).containsKey(methodName);

        allMetrics.clear();
        assertThat(performanceMonitoringAspect.getAllMetrics()).hasSize(1);
    }

    @Test
    void resetMetrics_ShouldClearAllMetrics() throws Throwable {
        String methodName = "UserService.updateUser(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenReturn("updated");

        performanceMonitoringAspect.monitorPerformance(joinPoint);
        assertThat(performanceMonitoringAspect.getAllMetrics()).hasSize(1);

        performanceMonitoringAspect.resetMetrics();

        assertThat(performanceMonitoringAspect.getAllMetrics()).isEmpty();

        List<ILoggingEvent> logsList = listAppender.list;
        ILoggingEvent resetLog = logsList.stream()
                .filter(log -> log.getFormattedMessage().contains("reset"))
                .findFirst()
                .orElse(null);

        assertThat(resetLog).isNotNull();
        assertThat(resetLog.getFormattedMessage()).contains("All performance metrics have been reset");
    }

    @Test
    void exportPerformanceSummary_ShouldLogMetricsSummary() throws Throwable {
        String methodName = "PostService.getAllPosts(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenReturn("posts");

        performanceMonitoringAspect.monitorPerformance(joinPoint);

        performanceMonitoringAspect.exportPerformanceSummary();

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.stream()
                .anyMatch(log -> log.getFormattedMessage().contains("PERFORMANCE METRICS SUMMARY")))
                .isTrue();
        assertThat(logsList.stream()
                .anyMatch(log -> log.getFormattedMessage().contains(methodName)))
                .isTrue();
    }

    @Test
    void exportPerformanceSummary_ShouldCreateLogFile() throws Throwable {
        String methodName = "UserService.searchUsers(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenReturn("users");

        performanceMonitoringAspect.monitorPerformance(joinPoint);

        performanceMonitoringAspect.exportPerformanceSummary();

        Path metricsDir = Path.of("metrics");
        if (Files.exists(metricsDir)) {
            try (Stream<Path> files = Files.list(metricsDir)) {
                assertThat(files.anyMatch(f -> f.getFileName().toString().endsWith("-performance-summary.log")))
                        .isTrue();
            }
        }
    }

    @Test
    void methodMetrics_ShouldInitializeCorrectly() {
        MethodMetrics metrics = new MethodMetrics("TestMethod");

        assertThat(metrics.getMethodName()).isEqualTo("TestMethod");
        assertThat(metrics.getTotalCalls()).isEqualTo(0);
        assertThat(metrics.getSuccessfulCalls()).isEqualTo(0);
        assertThat(metrics.getFailedCalls()).isEqualTo(0);
        assertThat(metrics.getAverageExecutionTime()).isEqualTo(0);
        assertThat(metrics.getMinExecutionTime()).isEqualTo(0);
        assertThat(metrics.getMaxExecutionTime()).isEqualTo(0);
    }

    @Test
    void methodMetrics_ShouldRecordSuccessfulExecution() {
        MethodMetrics metrics = new MethodMetrics("TestMethod");

        metrics.recordExecution(100, true);

        assertThat(metrics.getTotalCalls()).isEqualTo(1);
        assertThat(metrics.getSuccessfulCalls()).isEqualTo(1);
        assertThat(metrics.getFailedCalls()).isEqualTo(0);
        assertThat(metrics.getAverageExecutionTime()).isEqualTo(100);
        assertThat(metrics.getMinExecutionTime()).isEqualTo(100);
        assertThat(metrics.getMaxExecutionTime()).isEqualTo(100);
    }

    @Test
    void methodMetrics_ShouldRecordFailedExecution() {
        MethodMetrics metrics = new MethodMetrics("TestMethod");

        metrics.recordExecution(150, false);

        assertThat(metrics.getTotalCalls()).isEqualTo(1);
        assertThat(metrics.getSuccessfulCalls()).isEqualTo(0);
        assertThat(metrics.getFailedCalls()).isEqualTo(1);
        assertThat(metrics.getAverageExecutionTime()).isEqualTo(150);
    }

    @Test
    void methodMetrics_ShouldCalculateCorrectAverageExecutionTime() {
        MethodMetrics metrics = new MethodMetrics("TestMethod");

        metrics.recordExecution(100, true);
        metrics.recordExecution(200, true);
        metrics.recordExecution(300, true);

        assertThat(metrics.getTotalCalls()).isEqualTo(3);
    }

    @Test
    void methodMetrics_ShouldTrackMinAndMaxExecutionTime() {
        MethodMetrics metrics = new MethodMetrics("TestMethod");

        metrics.recordExecution(200, true);
        metrics.recordExecution(50, true);
        metrics.recordExecution(500, true);
        metrics.recordExecution(150, true);

        assertThat(metrics.getMinExecutionTime()).isEqualTo(50);
        assertThat(metrics.getMaxExecutionTime()).isEqualTo(500);
    }

    @Test
    void methodMetrics_ShouldHandleMultipleSuccessAndFailedCalls() {
        MethodMetrics metrics = new MethodMetrics("TestMethod");

        metrics.recordExecution(100, true);
        metrics.recordExecution(200, false);
        metrics.recordExecution(150, true);
        metrics.recordExecution(250, false);
        metrics.recordExecution(180, true);

        assertThat(metrics.getTotalCalls()).isEqualTo(5);
        assertThat(metrics.getSuccessfulCalls()).isEqualTo(3);
        assertThat(metrics.getFailedCalls()).isEqualTo(2);
        assertThat(metrics.getMinExecutionTime()).isEqualTo(100);
        assertThat(metrics.getMaxExecutionTime()).isEqualTo(250);
    }

    @Test
    void methodMetrics_ShouldReturnZeroAverage_WhenNoCallsRecorded() {
        MethodMetrics metrics = new MethodMetrics("TestMethod");

        assertThat(metrics.getAverageExecutionTime()).isEqualTo(0);
    }

    @Test
    void monitorPerformance_ShouldHandleMultipleCallsToSameMethod() throws Throwable {
        String methodName = "UserService.findUser(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);
        when(joinPoint.proceed()).thenReturn("user");

        performanceMonitoringAspect.monitorPerformance(joinPoint);
        performanceMonitoringAspect.monitorPerformance(joinPoint);
        performanceMonitoringAspect.monitorPerformance(joinPoint);

        MethodMetrics metrics = performanceMonitoringAspect.getMetrics(methodName);
        assertThat(metrics.getTotalCalls()).isEqualTo(3);
        assertThat(metrics.getSuccessfulCalls()).isEqualTo(3);
    }

    @Test
    void monitorPerformance_ShouldTrackDifferentMethods() throws Throwable {
        String method1 = "UserService.getUser(..)";
        String method2 = "PostService.getPost(..)";

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("result");

        when(signature.toShortString()).thenReturn(method1);
        performanceMonitoringAspect.monitorPerformance(joinPoint);

        when(signature.toShortString()).thenReturn(method2);
        performanceMonitoringAspect.monitorPerformance(joinPoint);

        assertThat(performanceMonitoringAspect.getAllMetrics()).hasSize(2);
        assertThat(performanceMonitoringAspect.getMetrics(method1)).isNotNull();
        assertThat(performanceMonitoringAspect.getMetrics(method2)).isNotNull();
    }
}