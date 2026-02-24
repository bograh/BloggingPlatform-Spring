package org.amalitech.bloggingplatformspring.dtos.responses;

import java.time.LocalDateTime;
import java.util.List;

public record RuntimeMetricsSnapshotDTO(
    LocalDateTime timestamp,
    long uptimeSeconds,
    long totalRequests,
    long totalErrors,
    double errorRatePercent,
    double averageLatencyMs,
    long minLatencyMs,
    long maxLatencyMs,
    double throughputReqPerSec,
    double throughputLast60SecondsReqPerSec,
    long usedMemoryMb,
    long committedMemoryMb,
    long maxMemoryMb,
    List<RuntimeEndpointMetricsDTO> endpoints) {
}
