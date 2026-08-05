package org.amalitech.bloggingplatformspring.services;

import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.responses.RuntimeEndpointMetricsDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.RuntimeMetricsSnapshotDTO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RuntimeMetricsService {

  private static final double ZERO_DOUBLE = 0.0;
  private static final long ZERO_LONG = 0L;
  private static final String RUNTIME_METRICS_DIR = "metrics/runtime";

  private final long startedAtMillis;
  private final LongAdder totalRequests = new LongAdder();
  private final LongAdder totalErrors = new LongAdder();
  private final LongAdder totalLatencyMs = new LongAdder();
  private final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);
  private final AtomicLong maxLatencyMs = new AtomicLong(Long.MIN_VALUE);
  private final ConcurrentLinkedQueue<Long> recentRequestTimestamps = new ConcurrentLinkedQueue<>();
  private final ConcurrentHashMap<String, EndpointMetrics> endpointMetricsByKey = new ConcurrentHashMap<>();

  public RuntimeMetricsService() {
    this.startedAtMillis = System.currentTimeMillis();
  }

  public void recordRequest(String method, String path, int statusCode, long latencyMs) {
    String endpoint = method + " " + path;
    boolean error = statusCode >= 400;

    totalRequests.increment();
    totalLatencyMs.add(latencyMs);
    if (error) {
      totalErrors.increment();
    }

    updateMin(minLatencyMs, latencyMs);
    updateMax(maxLatencyMs, latencyMs);

    endpointMetricsByKey.computeIfAbsent(endpoint, EndpointMetrics::new).record(latencyMs, error);

    long now = System.currentTimeMillis();
    recentRequestTimestamps.add(now);
    pruneOldRecentRequests(now);
  }

  public RuntimeMetricsSnapshotDTO getRuntimeSnapshot(int endpointLimit) {
    long now = System.currentTimeMillis();
    pruneOldRecentRequests(now);

    long requests = totalRequests.sum();
    long errors = totalErrors.sum();
    long uptimeSeconds = Math.max(1, (now - startedAtMillis) / 1000);

    double avgLatency = requests > 0 ? (double) totalLatencyMs.sum() / requests : ZERO_DOUBLE;
    double errorRate = requests > 0 ? ((double) errors / requests) * 100 : ZERO_DOUBLE;
    double throughput = (double) requests / uptimeSeconds;
    double throughputLast60 = (double) recentRequestTimestamps.size() / 60;

    Runtime runtime = Runtime.getRuntime();
    long committedMemoryMb = bytesToMb(runtime.totalMemory());
    long usedMemoryMb = bytesToMb(runtime.totalMemory() - runtime.freeMemory());
    long maxMemoryMb = bytesToMb(runtime.maxMemory());

    List<RuntimeEndpointMetricsDTO> endpoints = endpointMetricsByKey.values().stream()
        .map(metrics -> metrics.toDto(uptimeSeconds))
        .sorted(Comparator.comparingLong(RuntimeEndpointMetricsDTO::totalRequests).reversed())
        .limit(Math.max(1, endpointLimit))
        .collect(Collectors.toList());

    return new RuntimeMetricsSnapshotDTO(
        LocalDateTime.now(),
        uptimeSeconds,
        requests,
        errors,
        errorRate,
        avgLatency,
        normalizeMinLatency(minLatencyMs.get()),
        normalizeMaxLatency(maxLatencyMs.get()),
        throughput,
        throughputLast60,
        usedMemoryMb,
        committedMemoryMb,
        maxMemoryMb,
        endpoints);
  }

  public void reset() {
    totalRequests.reset();
    totalErrors.reset();
    totalLatencyMs.reset();
    minLatencyMs.set(Long.MAX_VALUE);
    maxLatencyMs.set(Long.MIN_VALUE);
    recentRequestTimestamps.clear();
    endpointMetricsByKey.clear();
  }

  public String exportRuntimeMetrics(int endpointLimit) {
    RuntimeMetricsSnapshotDTO snapshot = getRuntimeSnapshot(endpointLimit);
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    Path exportPath = Paths.get(RUNTIME_METRICS_DIR, timestamp + "-runtime-metrics.csv");

    try {
      Files.createDirectories(exportPath.getParent());
      Files.writeString(exportPath, buildCsv(snapshot));
      log.info("Runtime metrics exported to {}", exportPath.toAbsolutePath());
      return exportPath.toString();
    } catch (IOException exception) {
      log.error("Failed to export runtime metrics", exception);
      throw new IllegalStateException("Failed to export runtime metrics", exception);
    }
  }

  private String buildCsv(RuntimeMetricsSnapshotDTO snapshot) {
    StringBuilder csvBuilder = new StringBuilder();
    csvBuilder.append(
        "timestamp,uptimeSeconds,totalRequests,totalErrors,errorRatePercent,averageLatencyMs,minLatencyMs,maxLatencyMs,throughputReqPerSec,throughputLast60ReqPerSec,usedMemoryMb,committedMemoryMb,maxMemoryMb\n");
    csvBuilder.append(snapshot.timestamp()).append(",")
        .append(snapshot.uptimeSeconds()).append(",")
        .append(snapshot.totalRequests()).append(",")
        .append(snapshot.totalErrors()).append(",")
        .append(snapshot.errorRatePercent()).append(",")
        .append(snapshot.averageLatencyMs()).append(",")
        .append(snapshot.minLatencyMs()).append(",")
        .append(snapshot.maxLatencyMs()).append(",")
        .append(snapshot.throughputReqPerSec()).append(",")
        .append(snapshot.throughputLast60SecondsReqPerSec()).append(",")
        .append(snapshot.usedMemoryMb()).append(",")
        .append(snapshot.committedMemoryMb()).append(",")
        .append(snapshot.maxMemoryMb()).append("\n\n");

    csvBuilder.append(
        "endpoint,totalRequests,errorRequests,errorRatePercent,averageLatencyMs,minLatencyMs,maxLatencyMs,throughputReqPerSec\n");
    for (RuntimeEndpointMetricsDTO endpoint : snapshot.endpoints()) {
      csvBuilder.append(escape(endpoint.endpoint())).append(",")
          .append(endpoint.totalRequests()).append(",")
          .append(endpoint.errorRequests()).append(",")
          .append(endpoint.errorRatePercent()).append(",")
          .append(endpoint.averageLatencyMs()).append(",")
          .append(endpoint.minLatencyMs()).append(",")
          .append(endpoint.maxLatencyMs()).append(",")
          .append(endpoint.throughputReqPerSec()).append("\n");
    }

    return csvBuilder.toString();
  }

  private String escape(String value) {
    if (value.contains(",") || value.contains("\"")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  private long bytesToMb(long bytes) {
    return bytes / (1024 * 1024);
  }

  private void pruneOldRecentRequests(long nowMillis) {
    long threshold = nowMillis - 60_000;
    Long oldest = recentRequestTimestamps.peek();
    while (oldest != null && oldest < threshold) {
      recentRequestTimestamps.poll();
      oldest = recentRequestTimestamps.peek();
    }
  }

  private void updateMin(AtomicLong targetMin, long candidate) {
    targetMin.accumulateAndGet(candidate, Math::min);
  }

  private void updateMax(AtomicLong targetMax, long candidate) {
    targetMax.accumulateAndGet(candidate, Math::max);
  }

  private long normalizeMinLatency(long value) {
    return value == Long.MAX_VALUE ? ZERO_LONG : value;
  }

  private long normalizeMaxLatency(long value) {
    return value == Long.MIN_VALUE ? ZERO_LONG : value;
  }

  private static class EndpointMetrics {
    private final String endpoint;
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder errorRequests = new LongAdder();
    private final LongAdder totalLatencyMs = new LongAdder();
    private final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatencyMs = new AtomicLong(Long.MIN_VALUE);

    private EndpointMetrics(String endpoint) {
      this.endpoint = endpoint;
    }

    private void record(long latencyMs, boolean error) {
      totalRequests.increment();
      totalLatencyMs.add(latencyMs);
      if (error) {
        errorRequests.increment();
      }
      minLatencyMs.accumulateAndGet(latencyMs, Math::min);
      maxLatencyMs.accumulateAndGet(latencyMs, Math::max);
    }

    private RuntimeEndpointMetricsDTO toDto(long uptimeSeconds) {
      long requests = totalRequests.sum();
      long errors = errorRequests.sum();
      double avgLatency = requests > 0 ? (double) totalLatencyMs.sum() / requests : ZERO_DOUBLE;
      double errorRate = requests > 0 ? ((double) errors / requests) * 100 : ZERO_DOUBLE;
      double throughput = (double) requests / Math.max(1, uptimeSeconds);

      return new RuntimeEndpointMetricsDTO(
          endpoint,
          requests,
          errors,
          errorRate,
          avgLatency,
          minLatencyMs.get() == Long.MAX_VALUE ? ZERO_LONG : minLatencyMs.get(),
          maxLatencyMs.get() == Long.MIN_VALUE ? ZERO_LONG : maxLatencyMs.get(),
          throughput);
    }
  }
}
