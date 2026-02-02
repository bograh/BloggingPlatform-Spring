package org.amalitech.bloggingplatformspring.controllers;

import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect.MethodMetrics;
import org.amalitech.bloggingplatformspring.services.PerformanceMetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PerformanceMetricsController.class)
class PerformanceMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PerformanceMetricsService metricsService;

    @Test
    void getAllMetrics_ShouldReturnOkWithMetrics_WhenMetricsExist() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("totalMethods", 2);
        mockResponse.put("timestamp", new Date());

        ConcurrentHashMap<String, MethodMetrics> metricsMap = new ConcurrentHashMap<>();
        MethodMetrics metrics = new MethodMetrics("UserService.getUser(..)");
        metrics.recordExecution(100, true);
        metricsMap.put("UserService.getUser(..)", metrics);

        mockResponse.put("metrics", metricsMap);

        when(metricsService.getAllMetrics()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/metrics/performance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMethods").value(2))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.metrics").exists());

        verify(metricsService).getAllMetrics();
    }

    @Test
    void getAllMetrics_ShouldReturnOkWithEmptyMetrics_WhenNoMetricsExist() throws Exception {
        Map<String, Object> emptyResponse = new HashMap<>();
        emptyResponse.put("totalMethods", 0);
        emptyResponse.put("timestamp", new Date());
        emptyResponse.put("metrics", new ConcurrentHashMap<>());

        when(metricsService.getAllMetrics()).thenReturn(emptyResponse);

        mockMvc.perform(get("/api/metrics/performance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMethods").value(0))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.metrics").isEmpty());

        verify(metricsService).getAllMetrics();
    }

    @Test
    void getMethodMetrics_ShouldReturnOkWithMetrics_WhenMethodExists() throws Exception {
        String layer = "SERVICE";
        String methodName = "createPost";
        String fullMethodName = "SERVICE::createPost";

        MethodMetrics mockMetrics = new MethodMetrics(fullMethodName);
        mockMetrics.recordExecution(150, true);
        mockMetrics.recordExecution(200, true);

        when(metricsService.getMethodMetrics(fullMethodName)).thenReturn(mockMetrics);

        mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methodName").value(fullMethodName))
                .andExpect(jsonPath("$.totalCalls").value(2))
                .andExpect(jsonPath("$.successfulCalls").value(2))
                .andExpect(jsonPath("$.failedCalls").value(0));

        verify(metricsService).getMethodMetrics(fullMethodName);
    }

    @Test
    void getMethodMetrics_ShouldConvertLayerToUpperCase() throws Exception {
        String layer = "service";
        String methodName = "updateUser";
        String expectedFullMethodName = "SERVICE::updateUser";

        MethodMetrics mockMetrics = new MethodMetrics(expectedFullMethodName);
        mockMetrics.recordExecution(100, true);

        when(metricsService.getMethodMetrics(expectedFullMethodName)).thenReturn(mockMetrics);

        mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(metricsService).getMethodMetrics(expectedFullMethodName);
    }

    @Test
    void getMethodMetrics_ShouldReturnOkWithNull_WhenMethodDoesNotExist() throws Exception {
        String layer = "SERVICE";
        String methodName = "nonExistentMethod";
        String fullMethodName = "SERVICE::nonExistentMethod";

        when(metricsService.getMethodMetrics(fullMethodName)).thenReturn(null);

        mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(metricsService).getMethodMetrics(fullMethodName);
    }

    @Test
    void getMethodMetrics_ShouldHandleRepositoryLayer() throws Exception {
        String layer = "REPOSITORY";
        String methodName = "findById";
        String fullMethodName = "REPOSITORY::findById";

        MethodMetrics mockMetrics = new MethodMetrics(fullMethodName);
        mockMetrics.recordExecution(50, true);

        when(metricsService.getMethodMetrics(fullMethodName)).thenReturn(mockMetrics);

        mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methodName").value(fullMethodName));

        verify(metricsService).getMethodMetrics(fullMethodName);
    }

    @Test
    void getMethodMetrics_ShouldHandleMethodNameWithSpecialCharacters() throws Exception {
        String layer = "SERVICE";
        String methodName = "get-user-by-id";
        String fullMethodName = "SERVICE::get-user-by-id";

        MethodMetrics mockMetrics = new MethodMetrics(fullMethodName);
        mockMetrics.recordExecution(75, true);

        when(metricsService.getMethodMetrics(fullMethodName)).thenReturn(mockMetrics);

        mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(metricsService).getMethodMetrics(fullMethodName);
    }

    @Test
    void getMetricsSummary_ShouldReturnOkWithSummary() throws Exception {
        Map<String, Object> summaryResponse = new HashMap<>();
        summaryResponse.put("totalMethodsMonitored", 5);
        summaryResponse.put("totalExecutions", 100L);
        summaryResponse.put("totalFailures", 10L);
        summaryResponse.put("overallAverageExecutionTime", "150.25 ms");
        summaryResponse.put("timestamp", new Date());

        when(metricsService.getMetricsSummary()).thenReturn(summaryResponse);

        mockMvc.perform(get("/api/metrics/performance/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMethodsMonitored").value(5))
                .andExpect(jsonPath("$.totalExecutions").value(100))
                .andExpect(jsonPath("$.totalFailures").value(10))
                .andExpect(jsonPath("$.overallAverageExecutionTime").value("150.25 ms"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(metricsService).getMetricsSummary();
    }

    @Test
    void getMetricsSummary_ShouldReturnOkWithZeroValues_WhenNoMetrics() throws Exception {
        Map<String, Object> emptySummary = new HashMap<>();
        emptySummary.put("totalMethodsMonitored", 0);
        emptySummary.put("totalExecutions", 0L);
        emptySummary.put("totalFailures", 0L);
        emptySummary.put("overallAverageExecutionTime", "0.00 ms");
        emptySummary.put("timestamp", new Date());

        when(metricsService.getMetricsSummary()).thenReturn(emptySummary);

        mockMvc.perform(get("/api/metrics/performance/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMethodsMonitored").value(0))
                .andExpect(jsonPath("$.totalExecutions").value(0))
                .andExpect(jsonPath("$.totalFailures").value(0))
                .andExpect(jsonPath("$.overallAverageExecutionTime").value("0.00 ms"));

        verify(metricsService).getMetricsSummary();
    }

    @Test
    void resetMetrics_ShouldReturnOkWithSuccessMessage() throws Exception {
        doNothing().when(metricsService).resetMetrics();

        mockMvc.perform(delete("/api/metrics/performance/reset")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("All performance metrics have been reset"));

        verify(metricsService).resetMetrics();
    }

    @Test
    void resetMetrics_ShouldCallServiceOnce() throws Exception {
        doNothing().when(metricsService).resetMetrics();

        mockMvc.perform(delete("/api/metrics/performance/reset")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(metricsService, times(1)).resetMetrics();
        verifyNoMoreInteractions(metricsService);
    }

    @Test
    void exportToLog_ShouldReturnOkWithSuccessMessage() throws Exception {
        doNothing().when(metricsService).exportPerformanceSummary();

        mockMvc.perform(post("/api/metrics/performance/export-log")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Metrics exported to application log"));

        verify(metricsService).exportPerformanceSummary();
    }

    @Test
    void exportToLog_ShouldCallServiceOnce() throws Exception {
        doNothing().when(metricsService).exportPerformanceSummary();

        mockMvc.perform(post("/api/metrics/performance/export-log")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(metricsService, times(1)).exportPerformanceSummary();
        verifyNoMoreInteractions(metricsService);
    }

    @Test
    void getAllMetrics_ShouldReturnCorrectContentType() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("totalMethods", 0);
        mockResponse.put("timestamp", new Date());
        mockResponse.put("metrics", new ConcurrentHashMap<>());

        when(metricsService.getAllMetrics()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/metrics/performance"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void getMetricsSummary_ShouldReturnCorrectContentType() throws Exception {
        Map<String, Object> summaryResponse = new HashMap<>();
        summaryResponse.put("totalMethodsMonitored", 0);
        summaryResponse.put("totalExecutions", 0L);
        summaryResponse.put("totalFailures", 0L);
        summaryResponse.put("overallAverageExecutionTime", "0.00 ms");
        summaryResponse.put("timestamp", new Date());

        when(metricsService.getMetricsSummary()).thenReturn(summaryResponse);

        mockMvc.perform(get("/api/metrics/performance/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void resetMetrics_ShouldReturnCorrectContentType() throws Exception {
        doNothing().when(metricsService).resetMetrics();

        mockMvc.perform(delete("/api/metrics/performance/reset"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void exportToLog_ShouldReturnCorrectContentType() throws Exception {
        doNothing().when(metricsService).exportPerformanceSummary();

        mockMvc.perform(post("/api/metrics/performance/export-log"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void getMethodMetrics_ShouldHandleMixedCaseLayer() throws Exception {
        String layer = "SeRvIcE";
        String methodName = "deleteUser";
        String expectedFullMethodName = "SERVICE::deleteUser";

        MethodMetrics mockMetrics = new MethodMetrics(expectedFullMethodName);
        mockMetrics.recordExecution(80, true);

        when(metricsService.getMethodMetrics(expectedFullMethodName)).thenReturn(mockMetrics);

        mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName))
                .andExpect(status().isOk());

        verify(metricsService).getMethodMetrics(expectedFullMethodName);
    }

    @Test
    void getAllMetrics_ShouldHandleComplexMetricsData() throws Exception {
        Map<String, Object> complexResponse = new HashMap<>();
        complexResponse.put("totalMethods", 3);
        complexResponse.put("timestamp", new Date());

        ConcurrentHashMap<String, MethodMetrics> metricsMap = new ConcurrentHashMap<>();

        MethodMetrics metrics1 = new MethodMetrics("UserService.getUser(..)");
        metrics1.recordExecution(100, true);
        metrics1.recordExecution(150, true);
        metrics1.recordExecution(200, false);

        MethodMetrics metrics2 = new MethodMetrics("PostService.createPost(..)");
        metrics2.recordExecution(250, true);

        metricsMap.put("UserService.getUser(..)", metrics1);
        metricsMap.put("PostService.createPost(..)", metrics2);

        complexResponse.put("metrics", metricsMap);

        when(metricsService.getAllMetrics()).thenReturn(complexResponse);

        mockMvc.perform(get("/api/metrics/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMethods").value(3))
                .andExpect(jsonPath("$.metrics").isNotEmpty());
    }

    @Test
    void getMethodMetrics_ShouldReturnMetricsWithFailedCalls() throws Exception {
        String layer = "SERVICE";
        String methodName = "processPayment";
        String fullMethodName = "SERVICE::processPayment";

        MethodMetrics mockMetrics = new MethodMetrics(fullMethodName);
        mockMetrics.recordExecution(100, true);
        mockMetrics.recordExecution(150, false);
        mockMetrics.recordExecution(200, false);
        mockMetrics.recordExecution(120, true);

        when(metricsService.getMethodMetrics(fullMethodName)).thenReturn(mockMetrics);

        mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalls").value(4))
                .andExpect(jsonPath("$.successfulCalls").value(2))
                .andExpect(jsonPath("$.failedCalls").value(2))
                .andExpect(jsonPath("$.averageExecutionTime").value(142));

        verify(metricsService).getMethodMetrics(fullMethodName);
    }

    @Test
    void resetMetrics_ShouldReturnMapWithTwoKeys() throws Exception {
        doNothing().when(metricsService).resetMetrics();

        mockMvc.perform(delete("/api/metrics/performance/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(2)))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void exportToLog_ShouldReturnMapWithTwoKeys() throws Exception {
        doNothing().when(metricsService).exportPerformanceSummary();

        mockMvc.perform(post("/api/metrics/performance/export-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(2)))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getMethodMetrics_ShouldHandleNumericMethodName() throws Exception {
        String layer = "SERVICE";
        String methodName = "method123";
        String fullMethodName = "SERVICE::method123";

        MethodMetrics mockMetrics = new MethodMetrics(fullMethodName);
        mockMetrics.recordExecution(90, true);

        when(metricsService.getMethodMetrics(fullMethodName)).thenReturn(mockMetrics);

        mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methodName").value(fullMethodName));

        verify(metricsService).getMethodMetrics(fullMethodName);
    }

    @Test
    void getAllMetrics_ShouldBeAccessibleViaGetRequest() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("totalMethods", 0);
        mockResponse.put("timestamp", new Date());
        mockResponse.put("metrics", new ConcurrentHashMap<>());

        when(metricsService.getAllMetrics()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/metrics/performance"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/metrics/performance"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/metrics/performance"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/metrics/performance"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void resetMetrics_ShouldOnlyAcceptDeleteRequest() throws Exception {
        doNothing().when(metricsService).resetMetrics();

        mockMvc.perform(delete("/api/metrics/performance/reset"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/metrics/performance/reset"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(post("/api/metrics/performance/reset"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void exportToLog_ShouldOnlyAcceptPostRequest() throws Exception {
        doNothing().when(metricsService).exportPerformanceSummary();

        mockMvc.perform(post("/api/metrics/performance/export-log"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/metrics/performance/export-log"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/metrics/performance/export-log"))
                .andExpect(status().isMethodNotAllowed());
    }
}