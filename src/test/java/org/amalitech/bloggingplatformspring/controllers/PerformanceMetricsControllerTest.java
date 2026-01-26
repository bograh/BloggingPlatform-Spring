package org.amalitech.bloggingplatformspring.controllers;

import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.services.PerformanceMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PerformanceMetricsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PerformanceMetricsService metricsService;

    @InjectMocks
    private PerformanceMetricsController metricsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(metricsController).build();
    }

    @Test
    void getAllMetrics_ShouldReturnAllMetrics() throws Exception {
        Map<String, Object> mockMetrics = new HashMap<>();
        mockMetrics.put("totalMethods", 50);
        mockMetrics.put("totalExecutions", 1000);
        mockMetrics.put("averageExecutionTime", 250.5);

        when(metricsService.getAllMetricsFormatted()).thenReturn(mockMetrics);

        mockMvc.perform(get("/api/metrics/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMethods").value(50))
                .andExpect(jsonPath("$.totalExecutions").value(1000))
                .andExpect(jsonPath("$.averageExecutionTime").value(250.5));

        verify(metricsService).getAllMetricsFormatted();
    }

    @Test
    void getMethodMetrics_WithValidLayerAndMethod_ShouldReturnMethodMetrics() throws Exception {
        String layer = "SERVICE";
        String methodName = "createPost";
        String fullMethodName = "SERVICE::createPost";

        Map<String, Object> mockMethodMetrics = new HashMap<>();
        mockMethodMetrics.put("methodName", fullMethodName);
        mockMethodMetrics.put("executionCount", 100);
        mockMethodMetrics.put("averageTime", 150.0);
        mockMethodMetrics.put("minTime", 50L);
        mockMethodMetrics.put("maxTime", 500L);

        when(metricsService.getMethodMetricsFormatted(fullMethodName)).thenReturn(mockMethodMetrics);

        mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methodName").value(fullMethodName))
                .andExpect(jsonPath("$.executionCount").value(100))
                .andExpect(jsonPath("$.averageTime").value(150.0))
                .andExpect(jsonPath("$.minTime").value(50))
                .andExpect(jsonPath("$.maxTime").value(500));

        verify(metricsService).getMethodMetricsFormatted(fullMethodName);
    }

    @Test
    void getMethodMetrics_WithRepositoryLayer_ShouldConvertToUpperCase() throws Exception {
        String layer = "repository";
        String methodName = "findById";
        String fullMethodName = "REPOSITORY::findById";

        Map<String, Object> mockMethodMetrics = new HashMap<>();
        mockMethodMetrics.put("methodName", fullMethodName);
        mockMethodMetrics.put("executionCount", 50);

        when(metricsService.getMethodMetricsFormatted(fullMethodName)).thenReturn(mockMethodMetrics);

        mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methodName").value(fullMethodName));

        verify(metricsService).getMethodMetricsFormatted(fullMethodName);
    }

    @Test
    void getMetricsSummary_ShouldReturnSummaryData() throws Exception {
        Map<String, Object> mockSummary = new HashMap<>();
        mockSummary.put("totalMethods", 30);
        mockSummary.put("healthyMethods", 25);
        mockSummary.put("slowMethods", 5);
        mockSummary.put("failureRate", 2.5);

        when(metricsService.getMetricsSummary()).thenReturn(mockSummary);

        mockMvc.perform(get("/api/metrics/performance/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMethods").value(30))
                .andExpect(jsonPath("$.healthyMethods").value(25))
                .andExpect(jsonPath("$.slowMethods").value(5))
                .andExpect(jsonPath("$.failureRate").value(2.5));

        verify(metricsService).getMetricsSummary();
    }

    @Test
    void getSlowMethods_WithDefaultThreshold_ShouldReturnSlowMethods() throws Exception {
        Map<String, Object> mockSlowMethods = new HashMap<>();
        mockSlowMethods.put("threshold", 1000L);
        mockSlowMethods.put("count", 5);
        mockSlowMethods.put("methods", Map.of(
                "SERVICE::complexCalculation", 1500L,
                "REPOSITORY::bulkInsert", 2000L
        ));

        when(metricsService.getSlowMethods(1000L)).thenReturn(mockSlowMethods);

        mockMvc.perform(get("/api/metrics/performance/slow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threshold").value(1000))
                .andExpect(jsonPath("$.count").value(5));

        verify(metricsService).getSlowMethods(1000L);
    }

    @Test
    void getSlowMethods_WithCustomThreshold_ShouldReturnSlowMethods() throws Exception {
        long customThreshold = 500L;
        Map<String, Object> mockSlowMethods = new HashMap<>();
        mockSlowMethods.put("threshold", customThreshold);
        mockSlowMethods.put("count", 10);

        when(metricsService.getSlowMethods(customThreshold)).thenReturn(mockSlowMethods);

        mockMvc.perform(get("/api/metrics/performance/slow")
                        .param("thresholdMs", String.valueOf(customThreshold)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threshold").value(customThreshold))
                .andExpect(jsonPath("$.count").value(10));

        verify(metricsService).getSlowMethods(customThreshold);
    }

    @Test
    void getTopSlowMethods_WithDefaultLimit_ShouldReturnTopMethods() throws Exception {
        Map<String, Object> mockTopMethods = new HashMap<>();
        mockTopMethods.put("limit", 10);
        mockTopMethods.put("methods", Map.of(
                "SERVICE::heavyOperation", 3000L,
                "REPOSITORY::complexQuery", 2500L
        ));

        when(metricsService.getTopSlowMethods(10)).thenReturn(mockTopMethods);

        mockMvc.perform(get("/api/metrics/performance/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(10));

        verify(metricsService).getTopSlowMethods(10);
    }

    @Test
    void getTopSlowMethods_WithCustomLimit_ShouldReturnTopMethods() throws Exception {
        int customLimit = 5;
        Map<String, Object> mockTopMethods = new HashMap<>();
        mockTopMethods.put("limit", customLimit);
        mockTopMethods.put("count", 5);

        when(metricsService.getTopSlowMethods(customLimit)).thenReturn(mockTopMethods);

        mockMvc.perform(get("/api/metrics/performance/top")
                        .param("limit", String.valueOf(customLimit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(customLimit))
                .andExpect(jsonPath("$.count").value(5));

        verify(metricsService).getTopSlowMethods(customLimit);
    }

    @Test
    void getMetricsByLayer_WithServiceLayer_ShouldReturnServiceMetrics() throws Exception {
        String layer = "SERVICE";
        Map<String, Object> mockLayerMetrics = new HashMap<>();
        mockLayerMetrics.put("layer", layer);
        mockLayerMetrics.put("methodCount", 20);
        mockLayerMetrics.put("totalExecutions", 500);

        when(metricsService.getMetricsByLayer(layer)).thenReturn(mockLayerMetrics);

        mockMvc.perform(get("/api/metrics/performance/layer/{layer}", layer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layer").value(layer))
                .andExpect(jsonPath("$.methodCount").value(20))
                .andExpect(jsonPath("$.totalExecutions").value(500));

        verify(metricsService).getMetricsByLayer(layer);
    }

    @Test
    void getMetricsByLayer_WithLowercaseLayer_ShouldConvertToUpperCase() throws Exception {
        String layer = "repository";
        Map<String, Object> mockLayerMetrics = new HashMap<>();
        mockLayerMetrics.put("layer", "REPOSITORY");
        mockLayerMetrics.put("methodCount", 15);

        when(metricsService.getMetricsByLayer("REPOSITORY")).thenReturn(mockLayerMetrics);

        mockMvc.perform(get("/api/metrics/performance/layer/{layer}", layer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layer").value("REPOSITORY"));

        verify(metricsService).getMetricsByLayer("REPOSITORY");
    }

    @Test
    void getFailureStatistics_ShouldReturnFailureStats() throws Exception {
        Map<String, Object> mockFailureStats = new HashMap<>();
        mockFailureStats.put("totalFailures", 25);
        mockFailureStats.put("failureRate", 5.0);
        mockFailureStats.put("topFailingMethods", Map.of(
                "SERVICE::validateUser", 10,
                "REPOSITORY::connect", 8
        ));

        when(metricsService.getFailureStatistics()).thenReturn(mockFailureStats);

        mockMvc.perform(get("/api/metrics/performance/failures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFailures").value(25))
                .andExpect(jsonPath("$.failureRate").value(5.0));

        verify(metricsService).getFailureStatistics();
    }

    @Test
    void resetMetrics_ShouldResetAllMetrics() throws Exception {
        doNothing().when(metricsService).resetMetrics();

        mockMvc.perform(delete("/api/metrics/performance/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("All performance metrics have been reset"));

        verify(metricsService).resetMetrics();
    }

    @Test
    void exportToLog_ShouldExportMetrics() throws Exception {
        doNothing().when(metricsService).exportPerformanceSummary();

        mockMvc.perform(post("/api/metrics/performance/export/log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Metrics exported to application log"));

        verify(metricsService).exportPerformanceSummary();
    }

    @Test
    void getMethodMetrics_WithNonExistentMethod_ShouldThrowException() throws Exception {
        String layer = "SERVICE";
        String methodName = "nonExistentMethod";
        String fullMethodName = "SERVICE::nonExistentMethod";

        when(metricsService.getMethodMetricsFormatted(fullMethodName))
                .thenThrow(new ResourceNotFoundException("Method not found"));

        mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName))
                .andExpect(status().is4xxClientError());

        verify(metricsService).getMethodMetricsFormatted(fullMethodName);
    }

    @Test
    void getMetricsByLayer_WithInvalidLayer_ShouldThrowException() throws Exception {
        String layer = "INVALID";

        when(metricsService.getMetricsByLayer(layer))
                .thenThrow(new ResourceNotFoundException("Invalid layer"));

        mockMvc.perform(get("/api/metrics/performance/layer/{layer}", layer))
                .andExpect(status().is4xxClientError());

        verify(metricsService).getMetricsByLayer(layer);
    }

    @Test
    void getSlowMethods_WithZeroThreshold_ShouldReturnAllMethods() throws Exception {
        long threshold = 0L;
        Map<String, Object> mockSlowMethods = new HashMap<>();
        mockSlowMethods.put("threshold", threshold);
        mockSlowMethods.put("count", 50);

        when(metricsService.getSlowMethods(threshold)).thenReturn(mockSlowMethods);

        mockMvc.perform(get("/api/metrics/performance/slow")
                        .param("thresholdMs", String.valueOf(threshold)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threshold").value(threshold))
                .andExpect(jsonPath("$.count").value(50));

        verify(metricsService).getSlowMethods(threshold);
    }

    @Test
    void getTopSlowMethods_WithLimitOne_ShouldReturnOnlyTopMethod() throws Exception {
        int limit = 1;
        Map<String, Object> mockTopMethods = new HashMap<>();
        mockTopMethods.put("limit", limit);
        mockTopMethods.put("count", 1);
        mockTopMethods.put("methods", Map.of("SERVICE::slowestMethod", 5000L));

        when(metricsService.getTopSlowMethods(limit)).thenReturn(mockTopMethods);

        mockMvc.perform(get("/api/metrics/performance/top")
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(limit))
                .andExpect(jsonPath("$.count").value(1));

        verify(metricsService).getTopSlowMethods(limit);
    }
}