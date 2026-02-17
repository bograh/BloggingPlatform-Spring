package org.amalitech.bloggingplatformspring.dtos.responses;

/**
 * DTO for comparing a method's performance pre and post cache.
 */
public record MethodComparisonDTO(
    String methodName,
    PrePostMetricsDTO preCache,
    PrePostMetricsDTO postCache,
    ImprovementDTO improvement) {
}
