package org.amalitech.bloggingplatformspring.cache;

import org.amalitech.bloggingplatformspring.config.CacheConfig;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.junit.jupiter.api.*;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cache Performance Simulation Test
 *
 * This test simulates PRE_CACHE vs POST_CACHE performance comparisons
 * for key operations: getAllPosts(), getPostById(), getComments(), getTags()
 *
 * PRE_CACHE: Simulates database access (with artificial delay)
 * POST_CACHE: Retrieves from cache (near-instant)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CachePerformanceSimulationTest {

  private CacheManager cacheManager;

  // Simulated database delay in milliseconds
  private static final long SIMULATED_DB_DELAY_MS = 50;

  // Number of iterations for performance testing
  private static final int ITERATIONS = 10;

  // Test data
  private static final List<MockPost> MOCK_POSTS = new ArrayList<>();
  private static final List<MockComment> MOCK_COMMENTS = new ArrayList<>();
  private static final List<String> MOCK_TAGS = Arrays.asList("java", "spring", "hibernate", "cache", "performance");

  static {
    // Initialize mock data
    for (int i = 1; i <= 20; i++) {
      MOCK_POSTS.add(new MockPost((long) i, "Post Title " + i, "Post body content " + i, "author" + (i % 5)));
    }
    for (int i = 1; i <= 50; i++) {
      MOCK_COMMENTS.add(new MockComment("comment" + i, (long) (i % 20 + 1), "Comment content " + i, "user" + (i % 10)));
    }
  }

  @BeforeEach
  void setUp() {
    CacheConfig cacheConfig = new CacheConfig();
    cacheManager = cacheConfig.cacheManager();
    ((org.springframework.cache.support.SimpleCacheManager) cacheManager).initializeCaches();
    CacheConfig.resetAllStatistics();
  }

  @Test
  @Order(1)
  @DisplayName("PRE_CACHE vs POST_CACHE: getAllPosts()")
  void testGetAllPostsPerformance() {
    System.out.println("\n" + "=".repeat(80));
    System.out.println("PERFORMANCE SIMULATION: getAllPosts()");
    System.out.println("=".repeat(80));

    Cache postsListCache = cacheManager.getCache(Constants.POST_LIST_CACHE_NAME);
    assertNotNull(postsListCache);

    String cacheKey = "page:0size:10sort:createdAtorder:desc";

    System.out.println("\n📊 PRE_CACHE Phase (Cold Cache - Database Access)");
    System.out.println("-".repeat(50));

    long[] preCacheTimes = new long[ITERATIONS];
    for (int i = 0; i < ITERATIONS; i++) {
      postsListCache.clear();
      CacheConfig.resetAllStatistics();

      long startTime = System.nanoTime();

      // Simulate cache miss - must hit database
      Cache.ValueWrapper cached = postsListCache.get(cacheKey);
      assertNull(cached, "Cache should be empty (PRE_CACHE)");

      // Simulate database access with delay
      List<MockPost> dbResult = simulateDatabaseGetAllPosts(0, 10);

      // Store in cache for next time
      postsListCache.put(cacheKey, dbResult);

      long endTime = System.nanoTime();
      preCacheTimes[i] = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
    }

    double preCacheAvg = Arrays.stream(preCacheTimes).average().orElse(0);
    long preCacheMin = Arrays.stream(preCacheTimes).min().orElse(0);
    long preCacheMax = Arrays.stream(preCacheTimes).max().orElse(0);

    System.out.printf("  Iterations: %d%n", ITERATIONS);
    System.out.printf("  Average Time: %.2f μs%n", preCacheAvg);
    System.out.printf("  Min Time: %d μs%n", preCacheMin);
    System.out.printf("  Max Time: %d μs%n", preCacheMax);

    System.out.println("\n📊 POST_CACHE Phase (Warm Cache - Cache Hit)");
    System.out.println("-".repeat(50));

    // Pre-populate cache
    List<MockPost> cachedPosts = simulateDatabaseGetAllPosts(0, 10);
    postsListCache.put(cacheKey, cachedPosts);
    CacheConfig.resetAllStatistics();

    long[] postCacheTimes = new long[ITERATIONS];
    for (int i = 0; i < ITERATIONS; i++) {
      long startTime = System.nanoTime();

      // Cache hit - no database access
      Cache.ValueWrapper cached = postsListCache.get(cacheKey);
      assertNotNull(cached, "Cache should contain data (POST_CACHE)");

      @SuppressWarnings("unchecked")
      List<MockPost> result = (List<MockPost>) cached.get();
      assertNotNull(result);

      long endTime = System.nanoTime();
      postCacheTimes[i] = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
    }

    double postCacheAvg = Arrays.stream(postCacheTimes).average().orElse(0);
    long postCacheMin = Arrays.stream(postCacheTimes).min().orElse(0);
    long postCacheMax = Arrays.stream(postCacheTimes).max().orElse(0);

    System.out.printf("  Iterations: %d%n", ITERATIONS);
    System.out.printf("  Average Time: %.2f μs%n", postCacheAvg);
    System.out.printf("  Min Time: %d μs%n", postCacheMin);
    System.out.printf("  Max Time: %d μs%n", postCacheMax);

    printPerformanceComparison("getAllPosts()", preCacheAvg, postCacheAvg);

    // Verify cache statistics
    CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get(Constants.POST_LIST_CACHE_NAME);
    System.out.printf("%n  Cache Statistics:%n");
    System.out.printf("    Hits: %d%n", stats.getHits());
    System.out.printf("    Misses: %d%n", stats.getMisses());
    System.out.printf("    Hit Rate: %.2f%%%n", stats.getHitRate());

    assertTrue(postCacheAvg < preCacheAvg, "POST_CACHE should be faster than PRE_CACHE");
  }

  @Test
  @Order(2)
  @DisplayName("PRE_CACHE vs POST_CACHE: getPostById()")
  void testGetPostByIdPerformance() {
    System.out.println("\n" + "=".repeat(80));
    System.out.println("PERFORMANCE SIMULATION: getPostById()");
    System.out.println("=".repeat(80));

    Cache postsCache = cacheManager.getCache(Constants.POSTS_CACHE_NAME);
    assertNotNull(postsCache);

    Long postId = 1L;

    System.out.println("\n📊 PRE_CACHE Phase (Cold Cache - Database Access)");
    System.out.println("-".repeat(50));

    long[] preCacheTimes = new long[ITERATIONS];
    for (int i = 0; i < ITERATIONS; i++) {
      postsCache.clear();
      CacheConfig.resetAllStatistics();

      long startTime = System.nanoTime();

      Cache.ValueWrapper cached = postsCache.get(postId);
      assertNull(cached, "Cache should be empty (PRE_CACHE)");

      // Simulate database access with delay
      MockPost dbResult = simulateDatabaseGetPostById(postId);
      postsCache.put(postId, dbResult);

      long endTime = System.nanoTime();
      preCacheTimes[i] = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
    }

    double preCacheAvg = Arrays.stream(preCacheTimes).average().orElse(0);
    long preCacheMin = Arrays.stream(preCacheTimes).min().orElse(0);
    long preCacheMax = Arrays.stream(preCacheTimes).max().orElse(0);

    System.out.printf("  Iterations: %d%n", ITERATIONS);
    System.out.printf("  Average Time: %.2f μs%n", preCacheAvg);
    System.out.printf("  Min Time: %d μs%n", preCacheMin);
    System.out.printf("  Max Time: %d μs%n", preCacheMax);

    System.out.println("\n📊 POST_CACHE Phase (Warm Cache - Cache Hit)");
    System.out.println("-".repeat(50));

    MockPost cachedPost = simulateDatabaseGetPostById(postId);
    postsCache.put(postId, cachedPost);
    CacheConfig.resetAllStatistics();

    long[] postCacheTimes = new long[ITERATIONS];
    for (int i = 0; i < ITERATIONS; i++) {
      long startTime = System.nanoTime();

      Cache.ValueWrapper cached = postsCache.get(postId);
      assertNotNull(cached, "Cache should contain data (POST_CACHE)");
      MockPost result = (MockPost) cached.get();
      assertNotNull(result);
      assertEquals(postId, result.id());

      long endTime = System.nanoTime();
      postCacheTimes[i] = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
    }

    double postCacheAvg = Arrays.stream(postCacheTimes).average().orElse(0);
    long postCacheMin = Arrays.stream(postCacheTimes).min().orElse(0);
    long postCacheMax = Arrays.stream(postCacheTimes).max().orElse(0);

    System.out.printf("  Iterations: %d%n", ITERATIONS);
    System.out.printf("  Average Time: %.2f μs%n", postCacheAvg);
    System.out.printf("  Min Time: %d μs%n", postCacheMin);
    System.out.printf("  Max Time: %d μs%n", postCacheMax);

    printPerformanceComparison("getPostById()", preCacheAvg, postCacheAvg);

    CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get(Constants.POSTS_CACHE_NAME);
    System.out.printf("%n  Cache Statistics:%n");
    System.out.printf("    Hits: %d%n", stats.getHits());
    System.out.printf("    Misses: %d%n", stats.getMisses());
    System.out.printf("    Hit Rate: %.2f%%%n", stats.getHitRate());

    assertTrue(postCacheAvg < preCacheAvg, "POST_CACHE should be faster than PRE_CACHE");
  }

  @Test
  @Order(3)
  @DisplayName("PRE_CACHE vs POST_CACHE: getAllCommentsByPostId()")
  void testGetCommentsPerformance() {
    System.out.println("\n" + "=".repeat(80));
    System.out.println("PERFORMANCE SIMULATION: getAllCommentsByPostId()");
    System.out.println("=".repeat(80));

    Cache commentsCache = cacheManager.getCache(Constants.COMMENTS_CACHE_NAME);
    assertNotNull(commentsCache);

    Long postId = 1L;
    String cacheKey = "post:" + postId;

    System.out.println("\n📊 PRE_CACHE Phase (Cold Cache - Database Access)");
    System.out.println("-".repeat(50));

    long[] preCacheTimes = new long[ITERATIONS];
    for (int i = 0; i < ITERATIONS; i++) {
      commentsCache.clear();
      CacheConfig.resetAllStatistics();

      long startTime = System.nanoTime();

      Cache.ValueWrapper cached = commentsCache.get(cacheKey);
      assertNull(cached, "Cache should be empty (PRE_CACHE)");

      List<MockComment> dbResult = simulateDatabaseGetCommentsByPostId(postId);
      commentsCache.put(cacheKey, dbResult);

      long endTime = System.nanoTime();
      preCacheTimes[i] = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
    }

    double preCacheAvg = Arrays.stream(preCacheTimes).average().orElse(0);
    long preCacheMin = Arrays.stream(preCacheTimes).min().orElse(0);
    long preCacheMax = Arrays.stream(preCacheTimes).max().orElse(0);

    System.out.printf("  Iterations: %d%n", ITERATIONS);
    System.out.printf("  Average Time: %.2f μs%n", preCacheAvg);
    System.out.printf("  Min Time: %d μs%n", preCacheMin);
    System.out.printf("  Max Time: %d μs%n", preCacheMax);

    System.out.println("\n📊 POST_CACHE Phase (Warm Cache - Cache Hit)");
    System.out.println("-".repeat(50));

    List<MockComment> cachedComments = simulateDatabaseGetCommentsByPostId(postId);
    commentsCache.put(cacheKey, cachedComments);
    CacheConfig.resetAllStatistics();

    long[] postCacheTimes = new long[ITERATIONS];
    for (int i = 0; i < ITERATIONS; i++) {
      long startTime = System.nanoTime();

      Cache.ValueWrapper cached = commentsCache.get(cacheKey);
      assertNotNull(cached, "Cache should contain data (POST_CACHE)");

      @SuppressWarnings("unchecked")
      List<MockComment> result = (List<MockComment>) cached.get();
      assertNotNull(result);

      long endTime = System.nanoTime();
      postCacheTimes[i] = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
    }

    double postCacheAvg = Arrays.stream(postCacheTimes).average().orElse(0);
    long postCacheMin = Arrays.stream(postCacheTimes).min().orElse(0);
    long postCacheMax = Arrays.stream(postCacheTimes).max().orElse(0);

    System.out.printf("  Iterations: %d%n", ITERATIONS);
    System.out.printf("  Average Time: %.2f μs%n", postCacheAvg);
    System.out.printf("  Min Time: %d μs%n", postCacheMin);
    System.out.printf("  Max Time: %d μs%n", postCacheMax);

    printPerformanceComparison("getAllCommentsByPostId()", preCacheAvg, postCacheAvg);

    CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get(Constants.COMMENTS_CACHE_NAME);
    System.out.printf("%n  Cache Statistics:%n");
    System.out.printf("    Hits: %d%n", stats.getHits());
    System.out.printf("    Misses: %d%n", stats.getMisses());
    System.out.printf("    Hit Rate: %.2f%%%n", stats.getHitRate());

    assertTrue(postCacheAvg < preCacheAvg, "POST_CACHE should be faster than PRE_CACHE");
  }

  @Test
  @Order(4)
  @DisplayName("PRE_CACHE vs POST_CACHE: getPopularTags()")
  void testGetPopularTagsPerformance() {
    System.out.println("\n" + "=".repeat(80));
    System.out.println("PERFORMANCE SIMULATION: getPopularTags()");
    System.out.println("=".repeat(80));

    Cache tagsCache = cacheManager.getCache(Constants.TAGS_CACHE_NAME);
    assertNotNull(tagsCache);

    String cacheKey = "popular";

    System.out.println("\n📊 PRE_CACHE Phase (Cold Cache - Database Access)");
    System.out.println("-".repeat(50));

    long[] preCacheTimes = new long[ITERATIONS];
    for (int i = 0; i < ITERATIONS; i++) {
      tagsCache.clear();
      CacheConfig.resetAllStatistics();

      long startTime = System.nanoTime();

      Cache.ValueWrapper cached = tagsCache.get(cacheKey);
      assertNull(cached, "Cache should be empty (PRE_CACHE)");

      List<String> dbResult = simulateDatabaseGetPopularTags();
      tagsCache.put(cacheKey, dbResult);

      long endTime = System.nanoTime();
      preCacheTimes[i] = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
    }

    double preCacheAvg = Arrays.stream(preCacheTimes).average().orElse(0);
    long preCacheMin = Arrays.stream(preCacheTimes).min().orElse(0);
    long preCacheMax = Arrays.stream(preCacheTimes).max().orElse(0);

    System.out.printf("  Iterations: %d%n", ITERATIONS);
    System.out.printf("  Average Time: %.2f μs%n", preCacheAvg);
    System.out.printf("  Min Time: %d μs%n", preCacheMin);
    System.out.printf("  Max Time: %d μs%n", preCacheMax);

    System.out.println("\n📊 POST_CACHE Phase (Warm Cache - Cache Hit)");
    System.out.println("-".repeat(50));

    List<String> cachedTags = simulateDatabaseGetPopularTags();
    tagsCache.put(cacheKey, cachedTags);
    CacheConfig.resetAllStatistics();

    long[] postCacheTimes = new long[ITERATIONS];
    for (int i = 0; i < ITERATIONS; i++) {
      long startTime = System.nanoTime();

      Cache.ValueWrapper cached = tagsCache.get(cacheKey);
      assertNotNull(cached, "Cache should contain data (POST_CACHE)");

      @SuppressWarnings("unchecked")
      List<String> result = (List<String>) cached.get();
      assertNotNull(result);
      assertEquals(5, result.size());

      long endTime = System.nanoTime();
      postCacheTimes[i] = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
    }

    double postCacheAvg = Arrays.stream(postCacheTimes).average().orElse(0);
    long postCacheMin = Arrays.stream(postCacheTimes).min().orElse(0);
    long postCacheMax = Arrays.stream(postCacheTimes).max().orElse(0);

    System.out.printf("  Iterations: %d%n", ITERATIONS);
    System.out.printf("  Average Time: %.2f μs%n", postCacheAvg);
    System.out.printf("  Min Time: %d μs%n", postCacheMin);
    System.out.printf("  Max Time: %d μs%n", postCacheMax);

    printPerformanceComparison("getPopularTags()", preCacheAvg, postCacheAvg);

    CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get(Constants.TAGS_CACHE_NAME);
    System.out.printf("%n  Cache Statistics:%n");
    System.out.printf("    Hits: %d%n", stats.getHits());
    System.out.printf("    Misses: %d%n", stats.getMisses());
    System.out.printf("    Hit Rate: %.2f%%%n", stats.getHitRate());

    assertTrue(postCacheAvg < preCacheAvg, "POST_CACHE should be faster than PRE_CACHE");
  }

  @Test
  @Order(5)
  @DisplayName("Comprehensive Cache Performance Summary")
  void testComprehensivePerformanceSummary() {
    System.out.println("\n" + "=".repeat(80));
    System.out.println("COMPREHENSIVE CACHE PERFORMANCE SUMMARY");
    System.out.println("=".repeat(80));

    Map<String, PerformanceResult> results = new LinkedHashMap<>();

    // Test all operations
    results.put("getAllPosts()", measurePerformance(
        Constants.POST_LIST_CACHE_NAME,
        "page:0size:10sort:createdAtorder:desc",
        () -> simulateDatabaseGetAllPosts(0, 10)));

    results.put("getPostById(1L)", measurePerformance(
        Constants.POSTS_CACHE_NAME,
        1L,
        () -> simulateDatabaseGetPostById(1L)));

    results.put("getCommentsByPostId(1L)", measurePerformance(
        Constants.COMMENTS_CACHE_NAME,
        "post:1",
        () -> simulateDatabaseGetCommentsByPostId(1L)));

    results.put("getPopularTags()", measurePerformance(
        Constants.TAGS_CACHE_NAME,
        "popular",
        () -> simulateDatabaseGetPopularTags()));

    // Print summary table
    System.out.println("\n┌" + "─".repeat(94) + "┐");
    System.out.printf("│ %-30s │ %15s │ %15s │ %12s │ %12s │%n",
        "Operation", "PRE_CACHE (μs)", "POST_CACHE (μs)", "Speedup", "Improvement");
    System.out.println("├" + "─".repeat(94) + "┤");

    for (Map.Entry<String, PerformanceResult> entry : results.entrySet()) {
      PerformanceResult r = entry.getValue();
      System.out.printf("│ %-30s │ %15.2f │ %15.2f │ %10.1fx │ %10.1f%% │%n",
          entry.getKey(), r.preCacheAvg(), r.postCacheAvg(), r.speedup(), r.improvementPercent());
    }

    System.out.println("└" + "─".repeat(94) + "┘");

    // Cache statistics summary
    System.out.println("\n📈 Cache Statistics Summary:");
    System.out.println("┌" + "─".repeat(60) + "┐");
    System.out.printf("│ %-20s │ %8s │ %8s │ %10s │%n", "Cache Name", "Hits", "Misses", "Hit Rate");
    System.out.println("├" + "─".repeat(60) + "┤");

    for (String cacheName : Arrays.asList(Constants.POST_LIST_CACHE_NAME, Constants.POSTS_CACHE_NAME,
        Constants.COMMENTS_CACHE_NAME, Constants.TAGS_CACHE_NAME)) {
      CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get(cacheName);
      System.out.printf("│ %-20s │ %8d │ %8d │ %9.1f%% │%n",
          cacheName, stats.getHits(), stats.getMisses(), stats.getHitRate());
    }

    System.out.println("└" + "─".repeat(60) + "┘");
  }

  private void printPerformanceComparison(String operation, double preCacheAvg, double postCacheAvg) {
    double speedup = preCacheAvg / postCacheAvg;
    double improvement = ((preCacheAvg - postCacheAvg) / preCacheAvg) * 100;

    System.out.println("\n🚀 Performance Comparison: " + operation);
    System.out.println("-".repeat(50));
    System.out.printf("  PRE_CACHE Average:  %.2f μs%n", preCacheAvg);
    System.out.printf("  POST_CACHE Average: %.2f μs%n", postCacheAvg);
    System.out.printf("  Speedup Factor:     %.1fx faster%n", speedup);
    System.out.printf("  Performance Gain:   %.1f%% improvement%n", improvement);
  }

  private PerformanceResult measurePerformance(String cacheName, Object cacheKey, DataFetcher fetcher) {
    Cache cache = cacheManager.getCache(cacheName);
    assertNotNull(cache);

    long[] preCacheTimes = new long[ITERATIONS];
    for (int i = 0; i < ITERATIONS; i++) {
      cache.clear();
      long startTime = System.nanoTime();
      cache.get(cacheKey); // Miss
      Object data = fetcher.fetch();
      cache.put(cacheKey, data);
      long endTime = System.nanoTime();
      preCacheTimes[i] = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
    }

    // POST_CACHE measurements
    Object cachedData = fetcher.fetch();
    cache.put(cacheKey, cachedData);

    long[] postCacheTimes = new long[ITERATIONS];
    for (int i = 0; i < ITERATIONS; i++) {
      long startTime = System.nanoTime();
      cache.get(cacheKey); // Hit
      long endTime = System.nanoTime();
      postCacheTimes[i] = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
    }

    double preCacheAvg = Arrays.stream(preCacheTimes).average().orElse(0);
    double postCacheAvg = Arrays.stream(postCacheTimes).average().orElse(0);

    return new PerformanceResult(preCacheAvg, postCacheAvg);
  }

  private List<MockPost> simulateDatabaseGetAllPosts(int page, int size) {
    simulateDbDelay();
    return MOCK_POSTS.stream()
        .skip((long) page * size)
        .limit(size)
        .toList();
  }

  private MockPost simulateDatabaseGetPostById(Long postId) {
    simulateDbDelay();
    return MOCK_POSTS.stream()
        .filter(p -> p.id().equals(postId))
        .findFirst()
        .orElse(null);
  }

  private List<MockComment> simulateDatabaseGetCommentsByPostId(Long postId) {
    simulateDbDelay();
    return MOCK_COMMENTS.stream()
        .filter(c -> c.postId().equals(postId))
        .toList();
  }

  private List<String> simulateDatabaseGetPopularTags() {
    simulateDbDelay();
    return new ArrayList<>(MOCK_TAGS);
  }

  private void simulateDbDelay() {
    try {
      Thread.sleep(SIMULATED_DB_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @FunctionalInterface
  interface DataFetcher {
    Object fetch();
  }

  record MockPost(Long id, String title, String body, String author) {
  }

  record MockComment(String id, Long postId, String content, String author) {
  }

  record PerformanceResult(double preCacheAvg, double postCacheAvg) {
    double speedup() {
      return preCacheAvg / postCacheAvg;
    }

    double improvementPercent() {
      return ((preCacheAvg - postCacheAvg) / preCacheAvg) * 100;
    }
  }
}
