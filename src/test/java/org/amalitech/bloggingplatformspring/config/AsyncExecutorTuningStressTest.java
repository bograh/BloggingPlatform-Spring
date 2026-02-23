package org.amalitech.bloggingplatformspring.config;

import com.sun.management.OperatingSystemMXBean;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncExecutorTuningStressTest {

  private record PoolProfile(int corePoolSize, int maxPoolSize, int queueCapacity) {
    String label() {
      return "core=" + corePoolSize + ", max=" + maxPoolSize + ", queue=" + queueCapacity;
    }
  }

  private record PoolRunResult(PoolProfile profile, long elapsedMillis, double cpuUtilizationPercent,
      long peakMemoryMb, int failureCount) {
    double score() {
      return elapsedMillis + (cpuUtilizationPercent * 2.0) + (peakMemoryMb * 0.5);
    }
  }

  @Test
  void compareThreadPoolProfiles_shouldProvideStableRecommendation() throws InterruptedException {
    List<PoolProfile> profiles = List.of(
        new PoolProfile(4, 12, 400),
        new PoolProfile(8, 24, 400),
        new PoolProfile(12, 32, 400));

    List<PoolRunResult> results = profiles.stream()
        .map(this::runStressProfile)
        .toList();

    results.forEach(result -> System.out.printf(
        "[ASYNC-TUNING] %s | elapsed=%d ms | cpu=%.2f%% | peakMem=%d MB | failures=%d%n",
        result.profile().label(),
        result.elapsedMillis(),
        result.cpuUtilizationPercent(),
        result.peakMemoryMb(),
        result.failureCount()));

    PoolRunResult recommended = results.stream()
        .filter(result -> result.failureCount() == 0)
        .min(Comparator.comparingDouble(PoolRunResult::score))
        .orElseThrow();

    System.out.printf("[ASYNC-TUNING] Recommended profile: %s%n", recommended.profile().label());
    writeTuningSummary(results, recommended);

    assertTrue(results.stream().allMatch(result -> result.failureCount() == 0),
        "No profile should fail under the stress workload");
  }

  private PoolRunResult runStressProfile(PoolProfile profile) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(profile.corePoolSize());
    executor.setMaxPoolSize(profile.maxPoolSize());
    executor.setQueueCapacity(profile.queueCapacity());
    executor.setThreadNamePrefix("tuning-test-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setAllowCoreThreadTimeOut(true);
    executor.initialize();

    int tasks = 900;
    CountDownLatch doneLatch = new CountDownLatch(tasks);
    CopyOnWriteArrayList<Throwable> failures = new CopyOnWriteArrayList<>();
    AtomicLong peakMemoryBytes = new AtomicLong(currentUsedMemoryBytes());

    OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    long startCpuNanos = osBean.getProcessCpuTime();
    long startNanos = System.nanoTime();

    for (int taskIndex = 0; taskIndex < tasks; taskIndex++) {
      executor.execute(() -> {
        try {
          runMixedWorkload();
          peakMemoryBytes.updateAndGet(previousPeak -> Math.max(previousPeak, currentUsedMemoryBytes()));
        } catch (Throwable throwable) {
          failures.add(throwable);
        } finally {
          doneLatch.countDown();
        }
      });
    }

    boolean completed;
    try {
      completed = doneLatch.await(45, TimeUnit.SECONDS);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      completed = false;
    }

    long elapsedNanos = System.nanoTime() - startNanos;
    long endCpuNanos = osBean.getProcessCpuTime();

    executor.shutdown();

    if (!completed) {
      failures.add(new IllegalStateException("Stress run did not complete before timeout"));
    }

    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
    double cpuUtilization = calculateCpuUtilizationPercent(startCpuNanos, endCpuNanos, elapsedNanos);
    long peakMemoryMb = peakMemoryBytes.get() / (1024 * 1024);

    return new PoolRunResult(profile, elapsedMillis, cpuUtilization, peakMemoryMb, failures.size());
  }

  private void runMixedWorkload() {
    long accumulator = 0L;
    for (int iteration = 1; iteration <= 18_000; iteration++) {
      accumulator += (long) iteration * iteration;
      accumulator ^= (accumulator << 1);
    }

    if (accumulator == Long.MIN_VALUE) {
      throw new IllegalStateException("Unreachable branch to avoid dead-code elimination");
    }

    try {
      Thread.sleep(1);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
    }
  }

  private double calculateCpuUtilizationPercent(long startCpuNanos, long endCpuNanos, long elapsedNanos) {
    if (elapsedNanos <= 0L) {
      return 0.0;
    }

    long cpuDelta = Math.max(0L, endCpuNanos - startCpuNanos);
    int availableProcessors = Runtime.getRuntime().availableProcessors();
    if (availableProcessors <= 0) {
      return 0.0;
    }

    return (cpuDelta * 100.0) / (elapsedNanos * availableProcessors);
  }

  private long currentUsedMemoryBytes() {
    Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  private void writeTuningSummary(List<PoolRunResult> results, PoolRunResult recommended) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    String timestamp = LocalDateTime.now().format(formatter);
    Path outputPath = Path.of("metrics", "profiling", "thread-pool-tuning-" + timestamp + ".txt");

    StringBuilder content = new StringBuilder();
    content.append("ASYNC THREAD POOL TUNING RESULTS\n");
    content.append("timestamp=").append(timestamp).append("\n\n");

    for (PoolRunResult result : results) {
      content.append(result.profile().label())
          .append(" | elapsedMs=").append(result.elapsedMillis())
          .append(" | cpuPercent=").append(String.format("%.2f", result.cpuUtilizationPercent()))
          .append(" | peakMemoryMb=").append(result.peakMemoryMb())
          .append(" | failures=").append(result.failureCount())
          .append("\n");
    }

    content.append("\nrecommended=").append(recommended.profile().label()).append("\n");

    try {
      Files.createDirectories(outputPath.getParent());
      Files.writeString(outputPath, content);
      System.out.printf("[ASYNC-TUNING] Summary written to %s%n", outputPath);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to write tuning summary", exception);
    }
  }
}
