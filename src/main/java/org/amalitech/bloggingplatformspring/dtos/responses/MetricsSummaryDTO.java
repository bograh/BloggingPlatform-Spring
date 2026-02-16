package org.amalitech.bloggingplatformspring.dtos.responses;

import java.time.LocalDateTime;

/**
 * DTO for performance metrics summary response.
 */
public record MetricsSummaryDTO(
    int totalMethodsMonitored,
    long totalExecutions,
    long totalFailures,
    String overallAverageExecutionTime,
    double overallSuccessRate,
    LocalDateTime timestamp) {
}
