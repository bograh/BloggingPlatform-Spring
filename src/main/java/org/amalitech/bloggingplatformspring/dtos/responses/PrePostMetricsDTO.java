package org.amalitech.bloggingplatformspring.dtos.responses;

/**
 * DTO for pre or post cache metrics for comparison.
 */
public record PrePostMetricsDTO(
    long totalCalls,
    long avgExecutionTime,
    long minExecutionTime,
    long maxExecutionTime) {
}
