package org.amalitech.bloggingplatformspring.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity for storing performance metrics snapshots in MongoDB.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "performance_metrics")
public class PerformanceMetricsSnapshot {

  @Id
  private String id;

  @Indexed
  private LocalDateTime timestamp;

  private String snapshotType; // MANUAL, SCHEDULED, PRE_CACHE, POST_CACHE

  private int totalMethodsMonitored;
  private long totalExecutions;
  private long totalFailures;
  private double overallAverageExecutionTime;
  private double overallSuccessRate;

  private List<MethodMetricsData> methodMetrics;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MethodMetricsData {
    private String methodName;
    private long totalCalls;
    private long successfulCalls;
    private long failedCalls;
    private long averageExecutionTime;
    private long minExecutionTime;
    private long maxExecutionTime;
  }

  /**
   * Snapshot types
   */
  public enum SnapshotType {
    MANUAL,
    SCHEDULED,
    PRE_CACHE,
    POST_CACHE,
    COMPARISON
  }
}
