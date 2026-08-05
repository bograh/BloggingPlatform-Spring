package org.amalitech.bloggingplatformspring.dtos.responses;

public record RuntimeEndpointMetricsDTO(
    String endpoint,
    long totalRequests,
    long errorRequests,
    double errorRatePercent,
    double averageLatencyMs,
    long minLatencyMs,
    long maxLatencyMs,
    double throughputReqPerSec) {
}
