package org.amalitech.bloggingplatformspring.dtos.responses;

/**
 * DTO for performance improvement metrics.
 */
public record ImprovementDTO(
        long avgTimeReduction,
        String avgTimeReductionPercent,
        long minTimeReduction,
        long maxTimeReduction,
        boolean improved
) {}
