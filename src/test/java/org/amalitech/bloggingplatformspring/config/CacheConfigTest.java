package org.amalitech.bloggingplatformspring.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CacheConfig
 */
class CacheConfigTest {

  private CacheManager cacheManager;

  @BeforeEach
  void setUp() {
    // Create cache manager for testing
    CacheConfig cacheConfig = new CacheConfig();
    cacheManager = cacheConfig.cacheManager();

    // Reset statistics before each test
    CacheConfig.resetAllStatistics();
  }

  @Test
  void testCacheManagerCreation() {
    assertNotNull(cacheManager);

    // Verify all expected caches are created
    assertNotNull(cacheManager.getCache("users"));
    assertNotNull(cacheManager.getCache("posts"));
    assertNotNull(cacheManager.getCache("allPosts"));
    assertNotNull(cacheManager.getCache("comments"));
    assertNotNull(cacheManager.getCache("tags"));
  }

  @Test
  void testCacheHitTracking() {
    Cache usersCache = cacheManager.getCache("users");
    assertNotNull(usersCache);

    // Put a value in cache
    usersCache.put("user1", "John Doe");

    // First get should be a miss (already tracked in put operation flow)
    // Second get should be a hit
    Cache.ValueWrapper result = usersCache.get("user1");
    assertNotNull(result);
    assertEquals("John Doe", result.get());

    // Verify hit was recorded
    CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("users");
    assertTrue(stats.getHits() > 0, "At least one hit should be recorded");
  }

  @Test
  void testCacheMissTracking() {
    Cache postsCache = cacheManager.getCache("posts");
    assertNotNull(postsCache);

    // Try to get non-existent value
    Cache.ValueWrapper result = postsCache.get("nonexistent");
    assertNull(result);

    // Verify miss was recorded
    CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("posts");
    assertTrue(stats.getMisses() > 0, "At least one miss should be recorded");
  }

  @Test
  void testCacheHitRate() {
    Cache commentsCache = cacheManager.getCache("comments");
    assertNotNull(commentsCache);

    // Add some data
    commentsCache.put("comment1", "First comment");
    commentsCache.put("comment2", "Second comment");

    // Generate hits
    commentsCache.get("comment1"); // hit
    commentsCache.get("comment2"); // hit
    commentsCache.get("comment1"); // hit

    // Generate misses
    commentsCache.get("comment3"); // miss
    commentsCache.get("comment4"); // miss

    // Verify statistics
    CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("comments");

    assertEquals(3, stats.getHits());
    assertEquals(2, stats.getMisses());
    assertEquals(5, stats.getTotalRequests());
    assertEquals(2, stats.getPuts());

    // Hit rate should be 60% (3 hits out of 5 requests)
    assertEquals(60.0, stats.getHitRate(), 0.01);
    assertEquals(40.0, stats.getMissRate(), 0.01);
  }

  @Test
  void testCachePutTracking() {
    Cache tagsCache = cacheManager.getCache("tags");
    assertNotNull(tagsCache);

    tagsCache.put("tag1", "Java");
    tagsCache.put("tag2", "Spring");
    tagsCache.put("tag3", "Hibernate");

    CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("tags");
    assertEquals(3, stats.getPuts());
  }

  @Test
  void testCacheEvictionTracking() {
    Cache usersCache = cacheManager.getCache("users");
    assertNotNull(usersCache);

    usersCache.put("user1", "John");
    usersCache.put("user2", "Jane");

    usersCache.evict("user1");

    CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("users");
    assertEquals(1, stats.getEvictions());
  }

  @Test
  void testCacheClearTracking() {
    Cache allPostsCache = cacheManager.getCache("allPosts");
    assertNotNull(allPostsCache);

    allPostsCache.put("post1", "Post 1");
    allPostsCache.put("post2", "Post 2");

    allPostsCache.clear();

    CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("allPosts");
    assertEquals(1, stats.getClears());
  }

  @Test
  void testResetStatistics() {
    Cache usersCache = cacheManager.getCache("users");
    assertNotNull(usersCache);

    // Generate some statistics
    usersCache.put("user1", "John");
    usersCache.get("user1"); // hit
    usersCache.get("user2"); // miss

    CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("users");
    assertTrue(stats.getHits() > 0);
    assertTrue(stats.getMisses() > 0);
    assertTrue(stats.getPuts() > 0);

    // Reset statistics
    CacheConfig.resetAllStatistics();

    // Verify all statistics are reset
    assertEquals(0, stats.getHits());
    assertEquals(0, stats.getMisses());
    assertEquals(0, stats.getPuts());
    assertEquals(0, stats.getEvictions());
    assertEquals(0, stats.getClears());
    assertEquals(0, stats.getTotalRequests());
    assertEquals(0.0, stats.getHitRate());
  }

  @Test
  void testMultipleCacheStatistics() {
    // Add data to multiple caches
    Cache usersCache = cacheManager.getCache("users");
    Cache postsCache = cacheManager.getCache("posts");

    assertNotNull(usersCache);
    assertNotNull(postsCache);

    usersCache.put("user1", "John");
    postsCache.put("post1", "Post 1");

    usersCache.get("user1"); // hit
    postsCache.get("post2"); // miss

    var allStats = CacheConfig.getAllCacheStatistics();
    assertEquals(5, allStats.size());

    CacheConfig.CacheStatistics userStats = allStats.get("users");
    CacheConfig.CacheStatistics postStats = allStats.get("posts");

    assertNotNull(userStats);
    assertNotNull(postStats);

    assertEquals(1, userStats.getHits());
    assertEquals(0, userStats.getMisses());

    assertEquals(0, postStats.getHits());
    assertEquals(1, postStats.getMisses());
  }
}
