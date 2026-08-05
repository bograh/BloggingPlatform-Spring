package org.amalitech.bloggingplatformspring.dtos.responses;

import java.time.LocalDateTime;

/**
 * DTO for cache summary response.
 */
public record CacheSummaryDTO(
    int totalCaches,
    long totalHits,
    long totalMisses,
    long totalRequests,
    String overallHitRate,
    long totalPuts,
    long totalEvictions,
    CachePerformanceDTO bestPerformingCache,
    CachePerformanceDTO worstPerformingCache,
    LocalDateTime timestamp) {
}
