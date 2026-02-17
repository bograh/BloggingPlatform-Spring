package org.amalitech.bloggingplatformspring.dtos.responses;

/**
 * DTO for cache performance (best/worst performing cache).
 */
public record CachePerformanceDTO(
        String name,
        String hitRate
) {}
