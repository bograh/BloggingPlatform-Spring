package org.amalitech.bloggingplatformspring.dtos.responses;

/**
 * DTO for method performance metrics response.
 */
public record MethodMetricsDTO(
    String methodName,
    long totalCalls,
    long successfulCalls,
    long failedCalls,
    long averageExecutionTime,
    long minExecutionTime,
    long maxExecutionTime,
    double successRate) {
  /**
   * Create MethodMetricsDTO from MethodMetrics object
   */
  public static MethodMetricsDTO from(String name, long totalCalls, long successfulCalls,
      long failedCalls, long avgTime, long minTime, long maxTime) {
    double successRate = totalCalls > 0 ? (double) successfulCalls / totalCalls * 100 : 0.0;
    return new MethodMetricsDTO(name, totalCalls, successfulCalls, failedCalls,
        avgTime, minTime, maxTime, successRate);
  }
}
