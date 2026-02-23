package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect;
import org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect.MethodMetrics;
import org.amalitech.bloggingplatformspring.config.CacheConfig;
import org.amalitech.bloggingplatformspring.config.CacheConfig.CacheStatistics;
import org.amalitech.bloggingplatformspring.dtos.responses.*;
import org.amalitech.bloggingplatformspring.entity.CacheMetricsSnapshot;
import org.amalitech.bloggingplatformspring.entity.PerformanceMetricsSnapshot;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.repository.CacheMetricsRepository;
import org.amalitech.bloggingplatformspring.repository.PerformanceMetricsRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceMetricsService {

    private static final String STRING_FORMAT = "%.2f%%";
    private static final String PRE_CACHE_DIR = "metrics/pre-cache";

    private final PerformanceMonitoringAspect performanceAspect;
    private final PerformanceMetricsRepository performanceMetricsRepository;
    private final CacheMetricsRepository cacheMetricsRepository;

    /**
     * Get all metrics
     */
    public AllMetricsDTO getAllMetrics() {
        ConcurrentHashMap<String, MethodMetrics> allMetrics = performanceAspect.getAllMetrics();

        List<MethodMetricsDTO> metricsList = allMetrics.entrySet().stream()
                .map(entry -> convertToMethodMetricsDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return new AllMetricsDTO(
                allMetrics.size(),
                LocalDateTime.now(),
                metricsList);
    }

    /**
     * Get metrics for a specific method
     */
    public MethodMetricsDTO getMethodMetrics(String methodName) {
        MethodMetrics metrics = performanceAspect.getMetrics(methodName);
        if (metrics == null) {
            throw new BadRequestException("Method metrics not found: " + methodName);
        }
        return convertToMethodMetricsDTO(methodName, metrics);
    }

    /**
     * Get metrics summary
     */
    public MetricsSummaryDTO getMetricsSummary() {
        ConcurrentHashMap<String, MethodMetrics> allMetrics = performanceAspect.getAllMetrics();

        long totalExecutions = allMetrics.values().stream()
                .mapToLong(MethodMetrics::getTotalCalls)
                .sum();

        long totalFailures = allMetrics.values().stream()
                .mapToLong(MethodMetrics::getFailedCalls)
                .sum();

        double overallAvgTime = allMetrics.values().stream()
                .mapToLong(MethodMetrics::getAverageExecutionTime)
                .average()
                .orElse(0.0);

        double successRate = totalExecutions > 0 ? (double) (totalExecutions - totalFailures) / totalExecutions * 100
                : 0.0;

        return new MetricsSummaryDTO(
                allMetrics.size(),
                totalExecutions,
                totalFailures,
                String.format("%.2f ms", overallAvgTime),
                successRate,
                LocalDateTime.now());
    }

    /**
     * Reset all metrics
     */
    public void resetMetrics() {
        performanceAspect.resetMetrics();
    }

    /**
     * Export performance summary to logs
     */
    public void exportPerformanceSummary() {
        performanceAspect.exportPerformanceSummary();
    }

    @Async("applicationTaskExecutor")
    public CompletableFuture<Void> exportPerformanceSummaryAsync() {
        exportPerformanceSummary();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Export combined performance and cache metrics summary to logs and file
     */
    public void exportAllMetrics() {
        performanceAspect.exportCombinedMetrics(buildCacheMetricsMap(), buildCacheSummaryMap());
    }

    @Async("applicationTaskExecutor")
    public CompletableFuture<Void> exportAllMetricsAsync() {
        exportAllMetrics();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Export only cache metrics to logs and file
     */
    public void exportCacheMetrics() {
        performanceAspect.exportCacheMetricsToFile(buildCacheMetricsMap(), buildCacheSummaryMap());
    }

    @Async("applicationTaskExecutor")
    public CompletableFuture<Void> exportCacheMetricsAsync() {
        exportCacheMetrics();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get all cache metrics
     */
    public AllCacheMetricsDTO getAllCacheMetrics() {
        ConcurrentMap<String, CacheStatistics> allStats = CacheConfig.getAllCacheStatistics();

        List<CacheMetricsDTO> cacheMetricsList = allStats.entrySet().stream()
                .map(entry -> setCacheMetrics(entry.getValue()))
                .collect(Collectors.toList());

        return new AllCacheMetricsDTO(
                allStats.size(),
                LocalDateTime.now(),
                cacheMetricsList);
    }

    /**
     * Build cache metrics map for internal use (export functionality)
     */
    private Map<String, Object> buildCacheMetricsMap() {
        ConcurrentMap<String, CacheStatistics> allStats = CacheConfig.getAllCacheStatistics();

        Map<String, CacheMetricsDTO> cacheMetrics = new HashMap<>();

        for (Map.Entry<String, CacheStatistics> entry : allStats.entrySet()) {
            CacheStatistics stats = entry.getValue();
            CacheMetricsDTO metrics = setCacheMetrics(stats);
            cacheMetrics.put(entry.getKey(), metrics);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCaches", cacheMetrics.size());
        result.put("timestamp", LocalDateTime.now());
        result.put("caches", cacheMetrics);

        return result;
    }

    /**
     * Get cache statistics for a specific cache
     */
    public CacheMetricsDTO getCacheMetrics(String cacheName) {
        ConcurrentHashMap<String, CacheStatistics> allStats = CacheConfig.getAllCacheStatistics();
        CacheStatistics stats = allStats.get(cacheName);

        if (stats == null) {
            throw new BadRequestException("Cache not found: " + cacheName);
        }

        return setCacheMetrics(stats);
    }

    /**
     * Get cache summary
     */
    public CacheSummaryDTO getCacheSummary() {
        ConcurrentHashMap<String, CacheStatistics> allStats = CacheConfig.getAllCacheStatistics();

        long totalHits = allStats.values().stream().mapToLong(CacheStatistics::getHits).sum();
        long totalMisses = allStats.values().stream().mapToLong(CacheStatistics::getMisses).sum();
        long totalPuts = allStats.values().stream().mapToLong(CacheStatistics::getPuts).sum();
        long totalEvictions = allStats.values().stream().mapToLong(CacheStatistics::getEvictions).sum();
        long totalRequests = totalHits + totalMisses;
        double overallHitRate = totalRequests == 0 ? 0.0 : (double) totalHits / totalRequests * 100;

        Optional<CacheStatistics> bestCache = allStats.values().stream()
                .filter(s -> s.getTotalRequests() > 0)
                .max(Comparator.comparingDouble(CacheStatistics::getHitRate));

        Optional<CacheStatistics> worstCache = allStats.values().stream()
                .filter(s -> s.getTotalRequests() > 0)
                .min(Comparator.comparingDouble(CacheStatistics::getHitRate));

        CachePerformanceDTO bestPerforming = bestCache
                .map(s -> new CachePerformanceDTO(s.getCacheName(), String.format(STRING_FORMAT, s.getHitRate())))
                .orElse(null);

        CachePerformanceDTO worstPerforming = worstCache
                .map(s -> new CachePerformanceDTO(s.getCacheName(), String.format(STRING_FORMAT, s.getHitRate())))
                .orElse(null);

        return new CacheSummaryDTO(
                allStats.size(),
                totalHits,
                totalMisses,
                totalRequests,
                String.format(STRING_FORMAT, overallHitRate),
                totalPuts,
                totalEvictions,
                bestPerforming,
                worstPerforming,
                LocalDateTime.now());
    }

    /**
     * Build cache summary map for internal use (export functionality)
     */
    private Map<String, Object> buildCacheSummaryMap() {
        ConcurrentHashMap<String, CacheStatistics> allStats = CacheConfig.getAllCacheStatistics();

        long totalHits = allStats.values().stream().mapToLong(CacheStatistics::getHits).sum();
        long totalMisses = allStats.values().stream().mapToLong(CacheStatistics::getMisses).sum();
        long totalPuts = allStats.values().stream().mapToLong(CacheStatistics::getPuts).sum();
        long totalEvictions = allStats.values().stream().mapToLong(CacheStatistics::getEvictions).sum();
        long totalRequests = totalHits + totalMisses;
        double overallHitRate = totalRequests == 0 ? 0.0 : (double) totalHits / totalRequests * 100;

        Optional<CacheStatistics> bestCache = allStats.values().stream()
                .filter(s -> s.getTotalRequests() > 0)
                .max(Comparator.comparingDouble(CacheStatistics::getHitRate));

        Optional<CacheStatistics> worstCache = allStats.values().stream()
                .filter(s -> s.getTotalRequests() > 0)
                .min(Comparator.comparingDouble(CacheStatistics::getHitRate));

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCaches", allStats.size());
        summary.put("totalHits", totalHits);
        summary.put("totalMisses", totalMisses);
        summary.put("totalRequests", totalRequests);
        summary.put("overallHitRate", String.format(STRING_FORMAT, overallHitRate));
        summary.put("totalPuts", totalPuts);
        summary.put("totalEvictions", totalEvictions);

        bestCache.ifPresent(s -> {
            Map<String, Object> best = new HashMap<>();
            best.put("name", s.getCacheName());
            best.put("hitRate", String.format(STRING_FORMAT, s.getHitRate()));
            summary.put("bestPerformingCache", best);
        });

        worstCache.ifPresent(s -> {
            Map<String, Object> worst = new HashMap<>();
            worst.put("name", s.getCacheName());
            worst.put("hitRate", String.format(STRING_FORMAT, s.getHitRate()));
            summary.put("worstPerformingCache", worst);
        });

        summary.put("timestamp", LocalDateTime.now());
        return summary;
    }

    /**
     * Reset cache statistics
     */
    public void resetCacheMetrics() {
        CacheConfig.resetAllStatistics();
    }

    /**
     * Get list of available pre-cache metrics files
     */
    public List<String> getPreCacheFiles() {
        Path preCacheDir = Paths.get(PRE_CACHE_DIR);
        if (!Files.exists(preCacheDir)) {
            return Collections.emptyList();
        }

        try {
            return Files.list(preCacheDir)
                    .filter(p -> p.toString().endsWith(".log"))
                    .map(p -> p.getFileName().toString())
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error listing pre-cache files", e);
            return Collections.emptyList();
        }
    }

    /**
     * Compare pre-cache metrics with current post-cache metrics
     */
    public PerformanceComparisonDTO compareWithPreCache(String preCacheFileName) {
        Map<String, PreCacheMetrics> preCacheMetrics = parsePreCacheFile(preCacheFileName);
        ConcurrentHashMap<String, MethodMetrics> currentMetrics = performanceAspect.getAllMetrics();

        List<MethodComparisonDTO> comparisons = new ArrayList<>();

        double totalImprovementPercent = 0;
        int methodsImproved = 0;
        int methodsDegraded = 0;
        int methodsUnchanged = 0;
        String bestImprovedMethod = null;
        double bestImprovementPercent = Double.MIN_VALUE;
        String worstMethod = null;
        double worstChangePercent = Double.MAX_VALUE;

        // Compare methods that exist in both pre and current
        for (Map.Entry<String, PreCacheMetrics> entry : preCacheMetrics.entrySet()) {
            String methodName = entry.getKey();
            PreCacheMetrics preCacheData = entry.getValue();

            // Find matching current metric
            MethodMetrics currentData = findMatchingMethod(currentMetrics, methodName);
            if (currentData == null) {
                continue; // Skip if no matching current data
            }

            PrePostMetricsDTO preMetrics = new PrePostMetricsDTO(
                    preCacheData.totalCalls,
                    preCacheData.avgExecutionTime,
                    preCacheData.minExecutionTime,
                    preCacheData.maxExecutionTime);

            PrePostMetricsDTO postMetrics = new PrePostMetricsDTO(
                    currentData.getTotalCalls(),
                    currentData.getAverageExecutionTime(),
                    currentData.getMinExecutionTime(),
                    currentData.getMaxExecutionTime());

            long avgTimeReduction = preCacheData.avgExecutionTime - currentData.getAverageExecutionTime();
            double improvementPercent = preCacheData.avgExecutionTime > 0
                    ? (double) avgTimeReduction / preCacheData.avgExecutionTime * 100
                    : 0.0;

            ImprovementDTO improvement = new ImprovementDTO(
                    avgTimeReduction,
                    String.format("%.2f%%", improvementPercent),
                    preCacheData.minExecutionTime - currentData.getMinExecutionTime(),
                    preCacheData.maxExecutionTime - currentData.getMaxExecutionTime(),
                    avgTimeReduction > 0);

            comparisons.add(new MethodComparisonDTO(methodName, preMetrics, postMetrics, improvement));

            totalImprovementPercent += improvementPercent;

            if (avgTimeReduction > 0) {
                methodsImproved++;
                if (improvementPercent > bestImprovementPercent) {
                    bestImprovementPercent = improvementPercent;
                    bestImprovedMethod = methodName;
                }
            } else if (avgTimeReduction < 0) {
                methodsDegraded++;
                if (improvementPercent < worstChangePercent) {
                    worstChangePercent = improvementPercent;
                    worstMethod = methodName;
                }
            } else {
                methodsUnchanged++;
            }
        }

        int methodsCompared = comparisons.size();
        double overallAvgImprovement = methodsCompared > 0 ? totalImprovementPercent / methodsCompared : 0.0;

        ComparisonSummaryDTO summary = new ComparisonSummaryDTO(
                methodsCompared,
                methodsImproved,
                methodsDegraded,
                methodsUnchanged,
                String.format("%.2f%%", overallAvgImprovement),
                bestImprovedMethod,
                bestImprovedMethod != null ? String.format("%.2f%%", bestImprovementPercent) : null,
                worstMethod,
                worstMethod != null ? String.format("%.2f%%", worstChangePercent) : null);

        return new PerformanceComparisonDTO(
                preCacheFileName,
                LocalDateTime.now().toString(),
                comparisons,
                summary,
                LocalDateTime.now());
    }

    /**
     * Compare with the latest pre-cache file
     */
    public PerformanceComparisonDTO compareWithLatestPreCache() {
        List<String> files = getPreCacheFiles();
        if (files.isEmpty()) {
            throw new BadRequestException("No pre-cache metrics files found");
        }
        return compareWithPreCache(files.get(0));
    }

    @Async("applicationTaskExecutor")
    public CompletableFuture<PerformanceComparisonDTO> compareWithLatestPreCacheAsync() {
        return CompletableFuture.completedFuture(compareWithLatestPreCache());
    }

    /**
     * Save current performance metrics to database
     */
    public PerformanceMetricsSnapshot savePerformanceMetricsSnapshot(String snapshotType) {
        ConcurrentHashMap<String, MethodMetrics> allMetrics = performanceAspect.getAllMetrics();

        List<PerformanceMetricsSnapshot.MethodMetricsData> methodDataList = allMetrics.entrySet().stream()
                .map(entry -> PerformanceMetricsSnapshot.MethodMetricsData.builder()
                        .methodName(entry.getKey())
                        .totalCalls(entry.getValue().getTotalCalls())
                        .successfulCalls(entry.getValue().getSuccessfulCalls())
                        .failedCalls(entry.getValue().getFailedCalls())
                        .averageExecutionTime(entry.getValue().getAverageExecutionTime())
                        .minExecutionTime(entry.getValue().getMinExecutionTime())
                        .maxExecutionTime(entry.getValue().getMaxExecutionTime())
                        .build())
                .collect(Collectors.toList());

        long totalExecutions = allMetrics.values().stream().mapToLong(MethodMetrics::getTotalCalls).sum();
        long totalFailures = allMetrics.values().stream().mapToLong(MethodMetrics::getFailedCalls).sum();
        double overallAvgTime = allMetrics.values().stream()
                .mapToLong(MethodMetrics::getAverageExecutionTime).average().orElse(0.0);
        double successRate = totalExecutions > 0 ? (double) (totalExecutions - totalFailures) / totalExecutions * 100
                : 0.0;

        PerformanceMetricsSnapshot snapshot = PerformanceMetricsSnapshot.builder()
                .timestamp(LocalDateTime.now())
                .snapshotType(snapshotType)
                .totalMethodsMonitored(allMetrics.size())
                .totalExecutions(totalExecutions)
                .totalFailures(totalFailures)
                .overallAverageExecutionTime(overallAvgTime)
                .overallSuccessRate(successRate)
                .methodMetrics(methodDataList)
                .build();

        PerformanceMetricsSnapshot saved = performanceMetricsRepository.save(snapshot);
        log.info("Saved performance metrics snapshot: type={}, id={}", snapshotType, saved.getId());
        return saved;
    }

    /**
     * Save current cache metrics to database
     */
    public CacheMetricsSnapshot saveCacheMetricsSnapshot(String snapshotType) {
        ConcurrentHashMap<String, CacheStatistics> allStats = CacheConfig.getAllCacheStatistics();

        List<CacheMetricsSnapshot.CacheMetricsData> cacheDataList = allStats.entrySet().stream()
                .map(entry -> CacheMetricsSnapshot.CacheMetricsData.builder()
                        .cacheName(entry.getValue().getCacheName())
                        .hits(entry.getValue().getHits())
                        .misses(entry.getValue().getMisses())
                        .hitRate(entry.getValue().getHitRate())
                        .missRate(entry.getValue().getMissRate())
                        .totalRequests(entry.getValue().getTotalRequests())
                        .puts(entry.getValue().getPuts())
                        .evictions(entry.getValue().getEvictions())
                        .clears(entry.getValue().getClears())
                        .build())
                .collect(Collectors.toList());

        long totalHits = allStats.values().stream().mapToLong(CacheStatistics::getHits).sum();
        long totalMisses = allStats.values().stream().mapToLong(CacheStatistics::getMisses).sum();
        long totalPuts = allStats.values().stream().mapToLong(CacheStatistics::getPuts).sum();
        long totalEvictions = allStats.values().stream().mapToLong(CacheStatistics::getEvictions).sum();
        long totalRequests = totalHits + totalMisses;
        double overallHitRate = totalRequests == 0 ? 0.0 : (double) totalHits / totalRequests * 100;

        Optional<CacheStatistics> bestCache = allStats.values().stream()
                .filter(s -> s.getTotalRequests() > 0)
                .max(Comparator.comparingDouble(CacheStatistics::getHitRate));

        Optional<CacheStatistics> worstCache = allStats.values().stream()
                .filter(s -> s.getTotalRequests() > 0)
                .min(Comparator.comparingDouble(CacheStatistics::getHitRate));

        CacheMetricsSnapshot snapshot = CacheMetricsSnapshot.builder()
                .timestamp(LocalDateTime.now())
                .snapshotType(snapshotType)
                .totalCaches(allStats.size())
                .totalHits(totalHits)
                .totalMisses(totalMisses)
                .totalRequests(totalRequests)
                .overallHitRate(overallHitRate)
                .totalPuts(totalPuts)
                .totalEvictions(totalEvictions)
                .bestPerformingCacheName(bestCache.map(CacheStatistics::getCacheName).orElse(null))
                .bestPerformingCacheHitRate(bestCache.map(CacheStatistics::getHitRate).orElse(0.0))
                .worstPerformingCacheName(worstCache.map(CacheStatistics::getCacheName).orElse(null))
                .worstPerformingCacheHitRate(worstCache.map(CacheStatistics::getHitRate).orElse(0.0))
                .cacheMetrics(cacheDataList)
                .build();

        CacheMetricsSnapshot saved = cacheMetricsRepository.save(snapshot);
        log.info("Saved cache metrics snapshot: type={}, id={}", snapshotType, saved.getId());
        return saved;
    }

    /**
     * Save all metrics (performance + cache) to database
     */
    public void saveAllMetricsSnapshot(String snapshotType) {
        savePerformanceMetricsSnapshot(snapshotType);
        saveCacheMetricsSnapshot(snapshotType);
    }

    @Async("applicationTaskExecutor")
    public CompletableFuture<Void> saveAllMetricsSnapshotAsync(String snapshotType) {
        saveAllMetricsSnapshot(snapshotType);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get performance metrics history from database
     */
    public List<PerformanceMetricsSnapshot> getPerformanceMetricsHistory(int limit) {
        return performanceMetricsRepository.findAllByOrderByTimestampDesc(
                org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }

    /**
     * Get cache metrics history from database
     */
    public List<CacheMetricsSnapshot> getCacheMetricsHistory(int limit) {
        return cacheMetricsRepository.findAllByOrderByTimestampDesc(
                org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }

    private CacheMetricsDTO setCacheMetrics(CacheStatistics stats) {
        return new CacheMetricsDTO(
                stats.getCacheName(),
                stats.getHits(),
                stats.getMisses(),
                String.format(STRING_FORMAT, stats.getHitRate()),
                String.format(STRING_FORMAT, stats.getMissRate()),
                stats.getTotalRequests(),
                stats.getPuts(),
                stats.getEvictions(),
                stats.getClears(),
                String.valueOf(LocalDateTime.now()));
    }

    private MethodMetricsDTO convertToMethodMetricsDTO(String name, MethodMetrics metrics) {
        double successRate = metrics.getTotalCalls() > 0
                ? (double) metrics.getSuccessfulCalls() / metrics.getTotalCalls() * 100
                : 0.0;
        return new MethodMetricsDTO(
                name,
                metrics.getTotalCalls(),
                metrics.getSuccessfulCalls(),
                metrics.getFailedCalls(),
                metrics.getAverageExecutionTime(),
                metrics.getMinExecutionTime(),
                metrics.getMaxExecutionTime(),
                successRate);
    }

    private MethodMetrics findMatchingMethod(ConcurrentHashMap<String, MethodMetrics> metrics, String methodName) {
        // Direct match first
        if (metrics.containsKey(methodName)) {
            return metrics.get(methodName);
        }
        // Try to match by short name
        for (Map.Entry<String, MethodMetrics> entry : metrics.entrySet()) {
            if (entry.getKey().contains(methodName) || methodName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<String, PreCacheMetrics> parsePreCacheFile(String fileName) {
        Path filePath = Paths.get(PRE_CACHE_DIR, fileName);
        Map<String, PreCacheMetrics> result = new HashMap<>();

        if (!Files.exists(filePath)) {
            throw new BadRequestException("Pre-cache file not found: " + fileName);
        }

        try {
            List<String> lines = Files.readAllLines(filePath);
            Pattern methodPattern = Pattern.compile("^Method:\\s*(.+)$");
            Pattern totalCallsPattern = Pattern.compile("^\\s*Total Calls:\\s*(\\d+)$");
            Pattern avgTimePattern = Pattern.compile("^\\s*Avg Execution Time:\\s*(\\d+)\\s*ms$");
            Pattern minTimePattern = Pattern.compile("^\\s*Min Execution Time:\\s*(\\d+)\\s*ms$");
            Pattern maxTimePattern = Pattern.compile("^\\s*Max Execution Time:\\s*(\\d+)\\s*ms$");

            String currentMethod = null;
            PreCacheMetrics currentMetrics = null;

            for (String line : lines) {
                Matcher methodMatcher = methodPattern.matcher(line);
                if (methodMatcher.matches()) {
                    if (currentMethod != null && currentMetrics != null) {
                        result.put(currentMethod, currentMetrics);
                    }
                    currentMethod = methodMatcher.group(1).trim();
                    currentMetrics = new PreCacheMetrics();
                    continue;
                }

                if (currentMetrics == null)
                    continue;

                Matcher totalCallsMatcher = totalCallsPattern.matcher(line);
                if (totalCallsMatcher.matches()) {
                    currentMetrics.totalCalls = Long.parseLong(totalCallsMatcher.group(1));
                    continue;
                }

                Matcher avgTimeMatcher = avgTimePattern.matcher(line);
                if (avgTimeMatcher.matches()) {
                    currentMetrics.avgExecutionTime = Long.parseLong(avgTimeMatcher.group(1));
                    continue;
                }

                Matcher minTimeMatcher = minTimePattern.matcher(line);
                if (minTimeMatcher.matches()) {
                    currentMetrics.minExecutionTime = Long.parseLong(minTimeMatcher.group(1));
                    continue;
                }

                Matcher maxTimeMatcher = maxTimePattern.matcher(line);
                if (maxTimeMatcher.matches()) {
                    currentMetrics.maxExecutionTime = Long.parseLong(maxTimeMatcher.group(1));
                }
            }

            // Don't forget the last method
            if (currentMethod != null && currentMetrics != null) {
                result.put(currentMethod, currentMetrics);
            }

        } catch (IOException e) {
            log.error("Error reading pre-cache file: {}", fileName, e);
            throw new BadRequestException("Error reading pre-cache file: " + fileName);
        }

        return result;
    }

    /**
     * Save current metrics as PRE_CACHE baseline and reset metrics for fresh
     * measurement
     */
    public PerformanceMetricsSnapshot savePreCacheBaseline() {
        PerformanceMetricsSnapshot snapshot = savePerformanceMetricsSnapshot("PRE_CACHE");
        performanceAspect.resetMetrics();
        log.info("Saved PRE_CACHE baseline and reset metrics for fresh measurement");
        return snapshot;
    }

    /**
     * Save current metrics as POST_CACHE for comparison
     */
    public PerformanceMetricsSnapshot savePostCacheMetrics() {
        return savePerformanceMetricsSnapshot("POST_CACHE");
    }

    /**
     * Get the latest PRE_CACHE snapshot
     */
    public PerformanceMetricsSnapshot getLatestPreCacheSnapshot() {
        return performanceMetricsRepository.findTopBySnapshotTypeOrderByTimestampDesc("PRE_CACHE")
                .orElseThrow(() -> new BadRequestException(
                        "No PRE_CACHE baseline found. Save a baseline first using /baseline endpoint."));
    }

    /**
     * Get the latest POST_CACHE snapshot
     */
    public PerformanceMetricsSnapshot getLatestPostCacheSnapshot() {
        return performanceMetricsRepository.findTopBySnapshotTypeOrderByTimestampDesc("POST_CACHE")
                .orElseThrow(() -> new BadRequestException(
                        "No POST_CACHE snapshot found. Save post-cache metrics first using /postcache endpoint."));
    }

    /**
     * Get all PRE_CACHE snapshots
     */
    public List<PerformanceMetricsSnapshot> getPreCacheSnapshots(int limit) {
        return performanceMetricsRepository.findBySnapshotTypeOrderByTimestampDesc("PRE_CACHE",
                org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }

    /**
     * Get all POST_CACHE snapshots
     */
    public List<PerformanceMetricsSnapshot> getPostCacheSnapshots(int limit) {
        return performanceMetricsRepository.findBySnapshotTypeOrderByTimestampDesc("POST_CACHE",
                org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }

    /**
     * Compare latest PRE_CACHE with latest POST_CACHE from database
     */
    public PerformanceComparisonDTO compareFromDatabase() {
        PerformanceMetricsSnapshot preCacheSnapshot = getLatestPreCacheSnapshot();
        PerformanceMetricsSnapshot postCacheSnapshot = getLatestPostCacheSnapshot();
        return compareSnapshots(preCacheSnapshot, postCacheSnapshot);
    }

    /**
     * Compare specific PRE_CACHE and POST_CACHE snapshots by ID
     */
    public PerformanceComparisonDTO compareFromDatabase(String preCacheId, String postCacheId) {
        PerformanceMetricsSnapshot preCacheSnapshot = performanceMetricsRepository.findById(preCacheId)
                .orElseThrow(() -> new BadRequestException("PRE_CACHE snapshot not found: " + preCacheId));
        PerformanceMetricsSnapshot postCacheSnapshot = performanceMetricsRepository.findById(postCacheId)
                .orElseThrow(() -> new BadRequestException("POST_CACHE snapshot not found: " + postCacheId));
        return compareSnapshots(preCacheSnapshot, postCacheSnapshot);
    }

    /**
     * Compare two snapshots and generate comparison DTO
     */
    private PerformanceComparisonDTO compareSnapshots(PerformanceMetricsSnapshot preCacheSnapshot,
            PerformanceMetricsSnapshot postCacheSnapshot) {
        List<MethodComparisonDTO> comparisons = new ArrayList<>();

        double totalImprovementPercent = 0;
        int methodsImproved = 0;
        int methodsDegraded = 0;
        int methodsUnchanged = 0;
        String bestImprovedMethod = null;
        double bestImprovementPercent = Double.MIN_VALUE;
        String worstMethod = null;
        double worstChangePercent = Double.MAX_VALUE;

        // Build map from pre-cache snapshot
        Map<String, PerformanceMetricsSnapshot.MethodMetricsData> preCacheMap = preCacheSnapshot.getMethodMetrics()
                .stream()
                .collect(Collectors.toMap(PerformanceMetricsSnapshot.MethodMetricsData::getMethodName, m -> m));

        // Compare methods
        for (PerformanceMetricsSnapshot.MethodMetricsData postCacheData : postCacheSnapshot.getMethodMetrics()) {
            String methodName = postCacheData.getMethodName();
            PerformanceMetricsSnapshot.MethodMetricsData preCacheData = preCacheMap.get(methodName);

            if (preCacheData == null) {
                // Try partial match
                preCacheData = findMatchingMethodData(preCacheMap, methodName);
                if (preCacheData == null) {
                    continue; // Skip if no matching pre-cache data
                }
            }

            PrePostMetricsDTO preMetrics = new PrePostMetricsDTO(
                    preCacheData.getTotalCalls(),
                    preCacheData.getAverageExecutionTime(),
                    preCacheData.getMinExecutionTime(),
                    preCacheData.getMaxExecutionTime());

            PrePostMetricsDTO postMetrics = new PrePostMetricsDTO(
                    postCacheData.getTotalCalls(),
                    postCacheData.getAverageExecutionTime(),
                    postCacheData.getMinExecutionTime(),
                    postCacheData.getMaxExecutionTime());

            long avgTimeReduction = preCacheData.getAverageExecutionTime() - postCacheData.getAverageExecutionTime();
            double improvementPercent = preCacheData.getAverageExecutionTime() > 0
                    ? (double) avgTimeReduction / preCacheData.getAverageExecutionTime() * 100
                    : 0.0;

            ImprovementDTO improvement = new ImprovementDTO(
                    avgTimeReduction,
                    String.format("%.2f%%", improvementPercent),
                    preCacheData.getMinExecutionTime() - postCacheData.getMinExecutionTime(),
                    preCacheData.getMaxExecutionTime() - postCacheData.getMaxExecutionTime(),
                    avgTimeReduction > 0);

            comparisons.add(new MethodComparisonDTO(methodName, preMetrics, postMetrics, improvement));

            totalImprovementPercent += improvementPercent;

            if (avgTimeReduction > 0) {
                methodsImproved++;
                if (improvementPercent > bestImprovementPercent) {
                    bestImprovementPercent = improvementPercent;
                    bestImprovedMethod = methodName;
                }
            } else if (avgTimeReduction < 0) {
                methodsDegraded++;
                if (improvementPercent < worstChangePercent) {
                    worstChangePercent = improvementPercent;
                    worstMethod = methodName;
                }
            } else {
                methodsUnchanged++;
            }
        }

        int methodsCompared = comparisons.size();
        double overallAvgImprovement = methodsCompared > 0 ? totalImprovementPercent / methodsCompared : 0.0;

        ComparisonSummaryDTO summary = new ComparisonSummaryDTO(
                methodsCompared,
                methodsImproved,
                methodsDegraded,
                methodsUnchanged,
                String.format("%.2f%%", overallAvgImprovement),
                bestImprovedMethod,
                bestImprovedMethod != null ? String.format("%.2f%%", bestImprovementPercent) : null,
                worstMethod,
                worstMethod != null ? String.format("%.2f%%", worstChangePercent) : null);

        return new PerformanceComparisonDTO(
                "Database: PRE_CACHE(" + preCacheSnapshot.getId() + ") @ " + preCacheSnapshot.getTimestamp(),
                postCacheSnapshot.getTimestamp().toString(),
                comparisons,
                summary,
                LocalDateTime.now());
    }

    /**
     * Find matching method data by partial name match
     */
    private PerformanceMetricsSnapshot.MethodMetricsData findMatchingMethodData(
            Map<String, PerformanceMetricsSnapshot.MethodMetricsData> methodMap, String methodName) {
        for (Map.Entry<String, PerformanceMetricsSnapshot.MethodMetricsData> entry : methodMap.entrySet()) {
            if (entry.getKey().contains(methodName) || methodName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Helper class for parsed pre-cache metrics
     */
    private static class PreCacheMetrics {
        long totalCalls;
        long avgExecutionTime;
        long minExecutionTime;
        long maxExecutionTime;
    }
}