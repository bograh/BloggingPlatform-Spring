package org.amalitech.bloggingplatformspring.controllers;

import org.amalitech.bloggingplatformspring.dtos.responses.AllMetricsDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.MethodMetricsDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.MetricsSummaryDTO;
import org.amalitech.bloggingplatformspring.services.PerformanceMetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PerformanceMetricsController.class)
@AutoConfigureMockMvc(addFilters = false)
class PerformanceMetricsControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private PerformanceMetricsService metricsService;

        @MockitoBean
        private org.amalitech.bloggingplatformspring.services.CachePerformanceSimulationService simulationService;

        // Mock security components to prevent ApplicationContext loading errors
        @MockitoBean
        private org.amalitech.bloggingplatformspring.security.JwtTokenProvider jwtTokenProvider;

        @MockitoBean
        private org.amalitech.bloggingplatformspring.services.CustomUserDetailsService customUserDetailsService;

        @MockitoBean
        private org.amalitech.bloggingplatformspring.security.TokenSessionService tokenSessionService;

        @MockitoBean
        private org.amalitech.bloggingplatformspring.services.SecurityAuditService securityAuditService;

        @Test
        void getAllMetrics_ShouldReturnOkWithMetrics_WhenMetricsExist() throws Exception {
                List<MethodMetricsDTO> metricsList = new ArrayList<>();
                MethodMetricsDTO methodMetric = new MethodMetricsDTO(
                                "UserService.getUser(..)", 2, 2, 0, 100, 50, 150, 100.0);
                metricsList.add(methodMetric);

                AllMetricsDTO mockResponse = new AllMetricsDTO(
                                2,
                                LocalDateTime.now(),
                                metricsList);

                when(metricsService.getAllMetrics()).thenReturn(mockResponse);

                mockMvc.perform(get("/api/metrics/performance")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalMethods").value(2))
                                .andExpect(jsonPath("$.timestamp").exists())
                                .andExpect(jsonPath("$.metrics").isArray());

                verify(metricsService).getAllMetrics();
        }

        @Test
        void getAllMetrics_ShouldReturnOkWithEmptyMetrics_WhenNoMetricsExist() throws Exception {
                AllMetricsDTO emptyResponse = new AllMetricsDTO(
                                0,
                                LocalDateTime.now(),
                                new ArrayList<>());

                when(metricsService.getAllMetrics()).thenReturn(emptyResponse);

                mockMvc.perform(get("/api/metrics/performance")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalMethods").value(0))
                                .andExpect(jsonPath("$.timestamp").exists())
                                .andExpect(jsonPath("$.metrics", hasSize(0)));

                verify(metricsService).getAllMetrics();
        }

        @Test
        void getMethodMetrics_ShouldReturnOkWithMetrics_WhenMethodExists() throws Exception {
                String layer = "SERVICE";
                String methodName = "createPost";
                String fullMethodName = "SERVICE::createPost";

                MethodMetricsDTO mockMetrics = new MethodMetricsDTO(
                                fullMethodName, 2, 2, 0, 175, 150, 200, 100.0);

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

                MethodMetricsDTO mockMetrics = new MethodMetricsDTO(
                                expectedFullMethodName, 1, 1, 0, 100, 100, 100, 100.0);

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

                MethodMetricsDTO mockMetrics = new MethodMetricsDTO(
                                fullMethodName, 1, 1, 0, 50, 50, 50, 100.0);

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

                MethodMetricsDTO mockMetrics = new MethodMetricsDTO(
                                fullMethodName, 1, 1, 0, 75, 75, 75, 100.0);

                when(metricsService.getMethodMetrics(fullMethodName)).thenReturn(mockMetrics);

                mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                verify(metricsService).getMethodMetrics(fullMethodName);
        }

        @Test
        void getMetricsSummary_ShouldReturnOkWithSummary() throws Exception {
                MetricsSummaryDTO summaryResponse = new MetricsSummaryDTO(
                                5,
                                100L,
                                10L,
                                "150.25 ms",
                                90.0,
                                LocalDateTime.now());

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
                MetricsSummaryDTO emptySummary = new MetricsSummaryDTO(
                                0,
                                0L,
                                0L,
                                "0.00 ms",
                                0.0,
                                LocalDateTime.now());

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
                                .andExpect(jsonPath("$.message").value(
                                                "Performance metrics exported to application log and metrics folder"));

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
                AllMetricsDTO mockResponse = new AllMetricsDTO(
                                0,
                                LocalDateTime.now(),
                                new ArrayList<>());

                when(metricsService.getAllMetrics()).thenReturn(mockResponse);

                mockMvc.perform(get("/api/metrics/performance"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        void getMetricsSummary_ShouldReturnCorrectContentType() throws Exception {
                MetricsSummaryDTO summaryResponse = new MetricsSummaryDTO(
                                0,
                                0L,
                                0L,
                                "0.00 ms",
                                0.0,
                                LocalDateTime.now());

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

                MethodMetricsDTO mockMetrics = new MethodMetricsDTO(
                                expectedFullMethodName, 1, 1, 0, 80, 80, 80, 100.0);

                when(metricsService.getMethodMetrics(expectedFullMethodName)).thenReturn(mockMetrics);

                mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName))
                                .andExpect(status().isOk());

                verify(metricsService).getMethodMetrics(expectedFullMethodName);
        }

        @Test
        void getAllMetrics_ShouldHandleComplexMetricsData() throws Exception {
                List<MethodMetricsDTO> metricsList = new ArrayList<>();
                metricsList.add(new MethodMetricsDTO(
                                "UserService.getUser(..)", 3, 2, 1, 150, 100, 200, 66.67));
                metricsList.add(new MethodMetricsDTO(
                                "PostService.createPost(..)", 1, 1, 0, 250, 250, 250, 100.0));

                AllMetricsDTO complexResponse = new AllMetricsDTO(
                                3,
                                LocalDateTime.now(),
                                metricsList);

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

                MethodMetricsDTO mockMetrics = new MethodMetricsDTO(
                                fullMethodName, 4, 2, 2, 142, 100, 200, 50.0);

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
                                .andExpect(jsonPath("$.status").exists())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void exportToLog_ShouldReturnMapWithTwoKeys() throws Exception {
                doNothing().when(metricsService).exportPerformanceSummary();

                mockMvc.perform(post("/api/metrics/performance/export-log"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").exists())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void getMethodMetrics_ShouldHandleNumericMethodName() throws Exception {
                String layer = "SERVICE";
                String methodName = "method123";
                String fullMethodName = "SERVICE::method123";

                MethodMetricsDTO mockMetrics = new MethodMetricsDTO(
                                fullMethodName, 1, 1, 0, 90, 90, 90, 100.0);

                when(metricsService.getMethodMetrics(fullMethodName)).thenReturn(mockMetrics);

                mockMvc.perform(get("/api/metrics/performance/{layer}/{methodName}", layer, methodName))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.methodName").value(fullMethodName));

                verify(metricsService).getMethodMetrics(fullMethodName);
        }

        @Test
        void getAllMetrics_ShouldBeAccessibleViaGetRequest() throws Exception {
                AllMetricsDTO mockResponse = new AllMetricsDTO(
                                0,
                                LocalDateTime.now(),
                                new ArrayList<>());

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