package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.requests.CommentFilterRequest;
import org.amalitech.bloggingplatformspring.dtos.requests.PostFilterRequest;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for simulating and comparing cache performance.
 * Runs methods with and without cache to demonstrate performance differences.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachePerformanceSimulationService {

  private static final int SIMULATION_ITERATIONS = 25;
  private final PostService postService;
  private final CommentService commentService;
  private final TagService tagService;
  private final PostRepository postRepository;
  private final CacheManager cacheManager;

  /**
   * Run full cache performance simulation for all cached methods
   */
  public Map<String, Object> runFullSimulation() {
    log.info("Starting full cache performance simulation...");

    Map<String, Object> results = new LinkedHashMap<>();
    results.put("simulationStartTime", LocalDateTime.now().toString());
    results.put("iterations", SIMULATION_ITERATIONS);

    List<Post> samplePosts = postRepository.findAll(PageRequest.of(0, 3)).getContent();

    if (samplePosts.isEmpty()) {
      results.put("error", "No posts found in database. Please create some posts first.");
      return results;
    }

    List<Map<String, Object>> methodResults = new ArrayList<>();

    methodResults.add(simulateGetAllPosts());

    for (Post post : samplePosts) {
      methodResults.add(simulateGetPostById(post.getId()));
    }

    for (Post post : samplePosts) {
      methodResults.add(simulateGetAllCommentsByPostId(post.getId()));
    }

    methodResults.add(simulateGetPopularTags());

    methodResults.add(simulateGetAllComments());

    results.put("methodResults", methodResults);
    results.put("summary", buildSummary(methodResults));
    results.put("simulationEndTime", LocalDateTime.now().toString());

    log.info("Cache performance simulation completed");
    return results;
  }

  /**
   * Simulate getAllPosts with and without cache
   */
  public Map<String, Object> simulateGetAllPosts() {
    String methodName = "PostService.getAllPosts()";
    log.info("Simulating: {}", methodName);

    PostFilterRequest noFilters = new PostFilterRequest(null, null, null);

    clearCache(Constants.POST_LIST_CACHE_NAME);
    List<Long> preCacheTimes = new ArrayList<>();

    for (int i = 0; i < SIMULATION_ITERATIONS; i++) {
      clearCache(Constants.POST_LIST_CACHE_NAME); // Clear before each call
      long start = System.nanoTime();
      postService.getAllPosts(0, 10, "createdAt", "desc", noFilters);
      long end = System.nanoTime();
      preCacheTimes.add((end - start) / 1_000_000); // Convert to ms
    }

    clearCache(Constants.POST_LIST_CACHE_NAME);
    List<Long> postCacheTimes = new ArrayList<>();

    long firstCallStart = System.nanoTime();
    postService.getAllPosts(0, 10, "createdAt", "desc", noFilters);
    long firstCallEnd = System.nanoTime();
    long cacheMissTime = (firstCallEnd - firstCallStart) / 1_000_000;

    // Subsequent calls (cache hits)
    for (int i = 0; i < SIMULATION_ITERATIONS; i++) {
      long start = System.nanoTime();
      postService.getAllPosts(0, 10, "createdAt", "desc", noFilters);
      long end = System.nanoTime();
      postCacheTimes.add((end - start) / 1_000_000);
    }

    return buildMethodResult(methodName, preCacheTimes, postCacheTimes, cacheMissTime);
  }

  /**
   * Simulate getPostById with and without cache
   */
  public Map<String, Object> simulateGetPostById(Long postId) {
    String methodName = "PostService.getPostById(" + postId + ")";
    log.info("Simulating: {}", methodName);

    clearCache(Constants.POSTS_CACHE_NAME);
    List<Long> preCacheTimes = new ArrayList<>();

    for (int i = 0; i < SIMULATION_ITERATIONS; i++) {
      clearCache(Constants.POSTS_CACHE_NAME);
      long start = System.nanoTime();
      try {
        postService.getPostById(postId);
      } catch (Exception e) {
        log.warn("Error getting post {}: {}", postId, e.getMessage());
      }
      long end = System.nanoTime();
      preCacheTimes.add((end - start) / 1_000_000);
    }

    clearCache(Constants.POSTS_CACHE_NAME);
    List<Long> postCacheTimes = new ArrayList<>();

    long firstCallStart = System.nanoTime();
    try {
      postService.getPostById(postId);
    } catch (Exception e) {
      log.warn("Error getting post {}: {}", postId, e.getMessage());
    }
    long firstCallEnd = System.nanoTime();
    long cacheMissTime = (firstCallEnd - firstCallStart) / 1_000_000;

    for (int i = 0; i < SIMULATION_ITERATIONS; i++) {
      long start = System.nanoTime();
      try {
        postService.getPostById(postId);
      } catch (Exception e) {
      }
      long end = System.nanoTime();
      postCacheTimes.add((end - start) / 1_000_000);
    }

    return buildMethodResult(methodName, preCacheTimes, postCacheTimes, cacheMissTime);
  }

  /**
   * Simulate getAllCommentsByPostId with and without cache
   */
  public Map<String, Object> simulateGetAllCommentsByPostId(Long postId) {
    String methodName = "CommentService.getAllCommentsByPostId(" + postId + ")";
    log.info("Simulating: {}", methodName);

    clearCache(Constants.COMMENTS_CACHE_NAME);
    List<Long> preCacheTimes = new ArrayList<>();

    for (int i = 0; i < SIMULATION_ITERATIONS; i++) {
      clearCache(Constants.COMMENTS_CACHE_NAME);
      long start = System.nanoTime();
      try {
        commentService.getAllCommentsByPostId(postId);
      } catch (Exception e) {
        log.warn("Error getting comments for post {}: {}", postId, e.getMessage());
      }
      long end = System.nanoTime();
      preCacheTimes.add((end - start) / 1_000_000);
    }

    clearCache(Constants.COMMENTS_CACHE_NAME);
    List<Long> postCacheTimes = new ArrayList<>();

    long firstCallStart = System.nanoTime();
    try {
      commentService.getAllCommentsByPostId(postId);
    } catch (Exception e) {
      log.warn("Error getting comments for post {}: {}", postId, e.getMessage());
    }
    long firstCallEnd = System.nanoTime();
    long cacheMissTime = (firstCallEnd - firstCallStart) / 1_000_000;

    for (int i = 0; i < SIMULATION_ITERATIONS; i++) {
      long start = System.nanoTime();
      try {
        commentService.getAllCommentsByPostId(postId);
      } catch (Exception e) {
      }
      long end = System.nanoTime();
      postCacheTimes.add((end - start) / 1_000_000);
    }

    return buildMethodResult(methodName, preCacheTimes, postCacheTimes, cacheMissTime);
  }

  /**
   * Simulate getPopularTags with and without cache
   */
  public Map<String, Object> simulateGetPopularTags() {
    String methodName = "TagService.getPopularTags()";
    log.info("Simulating: {}", methodName);

    clearCache(Constants.TAGS_CACHE_NAME);
    List<Long> preCacheTimes = new ArrayList<>();

    for (int i = 0; i < SIMULATION_ITERATIONS; i++) {
      clearCache(Constants.TAGS_CACHE_NAME);
      long start = System.nanoTime();
      tagService.getPopularTags();
      long end = System.nanoTime();
      preCacheTimes.add((end - start) / 1_000_000);
    }

    clearCache(Constants.TAGS_CACHE_NAME);
    List<Long> postCacheTimes = new ArrayList<>();

    long firstCallStart = System.nanoTime();
    tagService.getPopularTags();
    long firstCallEnd = System.nanoTime();
    long cacheMissTime = (firstCallEnd - firstCallStart) / 1_000_000;

    for (int i = 0; i < SIMULATION_ITERATIONS; i++) {
      long start = System.nanoTime();
      tagService.getPopularTags();
      long end = System.nanoTime();
      postCacheTimes.add((end - start) / 1_000_000);
    }

    return buildMethodResult(methodName, preCacheTimes, postCacheTimes, cacheMissTime);
  }

  /**
   * Simulate getAllComments with and without cache (not cached - shows baseline)
   */
  public Map<String, Object> simulateGetAllComments() {
    String methodName = "CommentService.getAllComments()";
    log.info("Simulating: {}", methodName);

    CommentFilterRequest noFilters = new CommentFilterRequest(null, null, null);
    List<Long> preCacheTimes = new ArrayList<>();

    for (int i = 0; i < SIMULATION_ITERATIONS; i++) {
      long start = System.nanoTime();
      commentService.getAllComments(0, 10, "commentedAt", "desc", noFilters);
      long end = System.nanoTime();
      preCacheTimes.add((end - start) / 1_000_000);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("method", methodName);
    result.put("cached", false);
    result.put("note", "This method is NOT cached - showing baseline database performance");
    result.put("preCacheTimesMs", preCacheTimes);
    result.put("preCacheAvgMs", calculateAverage(preCacheTimes));
    result.put("preCacheMinMs", Collections.min(preCacheTimes));
    result.put("preCacheMaxMs", Collections.max(preCacheTimes));

    return result;
  }

  /**
   * Build result map for a simulated method
   */
  private Map<String, Object> buildMethodResult(String methodName, List<Long> preCacheTimes,
      List<Long> postCacheTimes, long cacheMissTime) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("method", methodName);
    result.put("cached", true);

    Map<String, Object> preCache = new LinkedHashMap<>();
    preCache.put("description", "Without cache - every call hits database");
    preCache.put("timesMs", preCacheTimes);
    preCache.put("avgMs", calculateAverage(preCacheTimes));
    preCache.put("minMs", Collections.min(preCacheTimes));
    preCache.put("maxMs", Collections.max(preCacheTimes));
    result.put("preCache", preCache);

    Map<String, Object> postCache = new LinkedHashMap<>();
    postCache.put("description", "With cache - first call misses, subsequent calls hit cache");
    postCache.put("cacheMissTimeMs", cacheMissTime);
    postCache.put("cacheHitTimesMs", postCacheTimes);
    postCache.put("cacheHitAvgMs", calculateAverage(postCacheTimes));
    postCache.put("cacheHitMinMs", Collections.min(postCacheTimes));
    postCache.put("cacheHitMaxMs", Collections.max(postCacheTimes));
    result.put("postCache", postCache);

    double preCacheAvg = calculateAverage(preCacheTimes);
    double postCacheHitAvg = calculateAverage(postCacheTimes);
    double improvementPercent = preCacheAvg > 0 ? ((preCacheAvg - postCacheHitAvg) / preCacheAvg) * 100 : 0;

    Map<String, Object> improvement = new LinkedHashMap<>();
    improvement.put("avgTimeReductionMs", preCacheAvg - postCacheHitAvg);
    improvement.put("avgTimeReductionPercent", String.format("%.2f%%", improvementPercent));
    improvement.put("speedupFactor", postCacheHitAvg > 0 ? String.format("%.1fx", preCacheAvg / postCacheHitAvg) : "∞");
    result.put("improvement", improvement);

    return result;
  }

  /**
   * Build overall summary of simulation results
   */
  private Map<String, Object> buildSummary(List<Map<String, Object>> methodResults) {
    Map<String, Object> summary = new LinkedHashMap<>();

    long cachedMethods = methodResults.stream()
        .filter(r -> Boolean.TRUE.equals(r.get("cached")))
        .count();

    summary.put("totalMethodsSimulated", methodResults.size());
    summary.put("cachedMethods", cachedMethods);
    summary.put("uncachedMethods", methodResults.size() - cachedMethods);

    double totalPreCacheAvg = 0;
    double totalPostCacheAvg = 0;
    int cachedCount = 0;

    for (Map<String, Object> result : methodResults) {
      if (Boolean.TRUE.equals(result.get("cached"))) {
        @SuppressWarnings("unchecked")
        Map<String, Object> preCache = (Map<String, Object>) result.get("preCache");
        @SuppressWarnings("unchecked")
        Map<String, Object> postCache = (Map<String, Object>) result.get("postCache");

        if (preCache != null && postCache != null) {
          totalPreCacheAvg += (Double) preCache.get("avgMs");
          totalPostCacheAvg += (Double) postCache.get("cacheHitAvgMs");
          cachedCount++;
        }
      }
    }

    if (cachedCount > 0) {
      double avgPreCache = totalPreCacheAvg / cachedCount;
      double avgPostCache = totalPostCacheAvg / cachedCount;
      double overallImprovement = avgPreCache > 0 ? ((avgPreCache - avgPostCache) / avgPreCache) * 100 : 0;

      summary.put("overallAvgPreCacheMs", String.format("%.2f", avgPreCache));
      summary.put("overallAvgPostCacheMs", String.format("%.2f", avgPostCache));
      summary.put("overallImprovementPercent", String.format("%.2f%%", overallImprovement));
    }

    summary.put("recommendation", buildRecommendation(methodResults));

    return summary;
  }

  /**
   * Build recommendation based on results
   */
  private String buildRecommendation(List<Map<String, Object>> methodResults) {
    StringBuilder sb = new StringBuilder();
    sb.append("Cache performance simulation complete. ");

    long slowMethods = methodResults.stream()
        .filter(r -> {
          if (!Boolean.TRUE.equals(r.get("cached")))
            return false;
          @SuppressWarnings("unchecked")
          Map<String, Object> preCache = (Map<String, Object>) r.get("preCache");
          return preCache != null && (Double) preCache.get("avgMs") > 50;
        })
        .count();

    if (slowMethods > 0) {
      sb.append("Found ").append(slowMethods).append(" method(s) with significant cache benefit (>50ms pre-cache). ");
    }

    sb.append("Use '/api/metrics/cache' to monitor real-time cache hit rates.");

    return sb.toString();
  }

  /**
   * Clear a specific cache
   */
  private void clearCache(String cacheName) {
    var cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      cache.clear();
    }
  }

  /**
   * Calculate average of a list of longs
   */
  private double calculateAverage(List<Long> values) {
    if (values.isEmpty())
      return 0.0;
    return values.stream().mapToLong(Long::longValue).average().orElse(0.0);
  }

  /**
   * Simulate a single method by name
   */
  public Map<String, Object> simulateMethod(String methodType, Long resourceId) {
    return switch (methodType.toLowerCase()) {
      case "getallposts" -> simulateGetAllPosts();
      case "getpostbyid" -> {
        if (resourceId == null) {
          throw new IllegalArgumentException("resourceId required for getPostById");
        }
        yield simulateGetPostById(resourceId);
      }
      case "getcommentsbypostid" -> {
        if (resourceId == null) {
          throw new IllegalArgumentException("resourceId required for getCommentsByPostId");
        }
        yield simulateGetAllCommentsByPostId(resourceId);
      }
      case "getpopulartags" -> simulateGetPopularTags();
      case "getallcomments" -> simulateGetAllComments();
      default -> throw new IllegalArgumentException("Unknown method type: " + methodType +
          ". Valid options: getAllPosts, getPostById, getCommentsByPostId, getPopularTags, getAllComments");
    };
  }
}