package org.amalitech.bloggingplatformspring.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity for storing cache metrics snapshots in MongoDB.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cache_metrics")
public class CacheMetricsSnapshot {

  @Id
  private String id;

  @Indexed
  private LocalDateTime timestamp;

  private String snapshotType;

  private int totalCaches;
  private long totalHits;
  private long totalMisses;
  private long totalRequests;
  private double overallHitRate;
  private long totalPuts;
  private long totalEvictions;

  private String bestPerformingCacheName;
  private double bestPerformingCacheHitRate;
  private String worstPerformingCacheName;
  private double worstPerformingCacheHitRate;

  private List<CacheMetricsData> cacheMetrics;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CacheMetricsData {
    private String cacheName;
    private long hits;
    private long misses;
    private double hitRate;
    private double missRate;
    private long totalRequests;
    private long puts;
    private long evictions;
    private long clears;
  }
}
