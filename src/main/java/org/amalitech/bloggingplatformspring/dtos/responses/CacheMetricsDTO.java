package org.amalitech.bloggingplatformspring.dtos.responses;

public record CacheMetricsDTO(
        String cacheName,
        Long hits,
        Long misses,
        String hitRate,
        String missRate,
        Long totalRequests,
        Long puts,
        Long evictions,
        Long clears,
        String timestamp
) {
}