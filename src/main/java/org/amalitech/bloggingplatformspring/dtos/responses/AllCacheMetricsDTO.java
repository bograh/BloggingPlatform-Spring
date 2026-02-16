package org.amalitech.bloggingplatformspring.dtos.responses;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for all cache metrics response.
 */
public record AllCacheMetricsDTO(
    int totalCaches,
    LocalDateTime timestamp,
    List<CacheMetricsDTO> caches) {
}
