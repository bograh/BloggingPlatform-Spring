package org.amalitech.bloggingplatformspring.dtos.responses;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for performance comparison between pre-cache and post-cache metrics.
 */
public record PerformanceComparisonDTO(
    String preCacheFile,
    String postCacheTimestamp,
    List<MethodComparisonDTO> methodComparisons,
    ComparisonSummaryDTO summary,
    LocalDateTime timestamp) {
}
