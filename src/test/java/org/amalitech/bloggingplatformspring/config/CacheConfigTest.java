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
        CacheConfig cacheConfig = new CacheConfig();
        cacheManager = cacheConfig.cacheManager();

        // SimpleCacheManager requires initialization
        ((org.springframework.cache.support.SimpleCacheManager) cacheManager).initializeCaches();

        CacheConfig.resetAllStatistics();
    }

    @Test
    void testCacheManagerCreation() {
        assertNotNull(cacheManager);

        assertNotNull(cacheManager.getCache("users"));
        assertNotNull(cacheManager.getCache("posts"));
        assertNotNull(cacheManager.getCache("postsList"));
        assertNotNull(cacheManager.getCache("comments"));
        assertNotNull(cacheManager.getCache("tags"));
    }

    @Test
    void testCacheHitTracking() {
        Cache usersCache = cacheManager.getCache("users");
        assertNotNull(usersCache);

        usersCache.put("user1", "John Doe");

        Cache.ValueWrapper result = usersCache.get("user1");
        assertNotNull(result);
        assertEquals("John Doe", result.get());

        CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("users");
        assertTrue(stats.getHits() > 0, "At least one hit should be recorded");
    }

    @Test
    void testCacheMissTracking() {
        Cache postsCache = cacheManager.getCache("posts");
        assertNotNull(postsCache);

        Cache.ValueWrapper result = postsCache.get("nonexistent");
        assertNull(result);

        CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("posts");
        assertTrue(stats.getMisses() > 0, "At least one miss should be recorded");
    }

    @Test
    void testCacheHitRate() {
        Cache commentsCache = cacheManager.getCache("comments");
        assertNotNull(commentsCache);

        commentsCache.put("comment1", "First comment");
        commentsCache.put("comment2", "Second comment");

        commentsCache.get("comment1");
        commentsCache.get("comment2");
        commentsCache.get("comment1");

        commentsCache.get("comment3");
        commentsCache.get("comment4");

        CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("comments");

        assertEquals(3, stats.getHits());
        assertEquals(2, stats.getMisses());
        assertEquals(5, stats.getTotalRequests());
        assertEquals(2, stats.getPuts());

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
        Cache allPostsCache = cacheManager.getCache("postsList");
        assertNotNull(allPostsCache);

        allPostsCache.put("post1", "Post 1");
        allPostsCache.put("post2", "Post 2");

        allPostsCache.clear();

        CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("postsList");
        assertEquals(1, stats.getClears());
    }

    @Test
    void testResetStatistics() {
        Cache usersCache = cacheManager.getCache("users");
        assertNotNull(usersCache);

        usersCache.put("user1", "John");
        usersCache.get("user1");
        usersCache.get("user2");

        CacheConfig.CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("users");
        assertTrue(stats.getHits() > 0);
        assertTrue(stats.getMisses() > 0);
        assertTrue(stats.getPuts() > 0);

        CacheConfig.resetAllStatistics();

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