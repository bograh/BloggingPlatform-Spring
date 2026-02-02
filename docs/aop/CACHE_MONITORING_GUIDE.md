# Cache Monitoring Guide

Complete guide to the caching and monitoring system in the Blogging Platform.

## Table of Contents

- [Overview](#overview)
- [Cache Configuration](#cache-configuration)
- [Available Caches](#available-caches)
- [Cache Metrics](#cache-metrics)
- [API Endpoints](#api-endpoints)
- [Metrics Export](#metrics-export)
- [Performance Optimization](#performance-optimization)
- [Best Practices](#best-practices)

## Overview

The Blogging Platform implements intelligent caching with comprehensive monitoring capabilities to optimize performance and provide insights into cache effectiveness.

### Features

- **Multi-level Caching**: Separate caches for users, posts, comments, and tags
- **Hit/Miss Tracking**: Detailed statistics on cache efficiency
- **Real-time Monitoring**: Live cache performance metrics
- **Export Capabilities**: Export metrics to files for analysis
- **Automatic Statistics**: Hit rates, miss rates, eviction tracking

## Cache Configuration

### Cache Manager Setup

Caches are configured in `CacheConfig.java` with monitoring enabled:

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        String[] cacheNames = {"users", "posts", "allPosts", "comments", "tags"};
        // ... monitoring setup
        return cacheManager;
    }
}
```

### Monitored Cache Implementation

Each cache is wrapped in a `MonitoredCache` that tracks:
- Cache hits
- Cache misses
- Cache puts (additions)
- Cache evictions (removals)
- Cache clears

## Available Caches

### 1. Users Cache (`users`)

**Purpose**: Caches user profiles to reduce database queries

**Key Format**: `'profile:' + userID`

**Cached By**:
- `UserService.getUserById(UUID userID)`

**Evicted On**:
- User update operations
- User deletion

**Example**:
```java
@Cacheable(cacheNames = "users", key = "'profile:' + #userID")
public GetUserDTO getUserById(UUID userID) {
    // ...
}
```

### 2. Posts Cache (`posts`)

**Purpose**: Caches individual post data

**Key Format**: Post ID

**Cached By**:
- `PostService.getPostById(Long postId)`

**Evicted On**:
- Post update
- Post deletion

**Example**:
```java
@Cacheable(cacheNames = "posts", key = "#postId")
public GetPostDTO getPostById(Long postId) {
    // ...
}
```

### 3. All Posts Cache (`allPosts`)

**Purpose**: Caches paginated post listings

**Key Format**: `'page:' + page + 'size:' + size + 'sort:' + sortBy + 'order:' + order`

**Cached By**:
- `PostService.getAllPosts(int page, int size, String sortBy, String order)`

**Evicted On**:
- New post creation
- Any post update
- Any post deletion

**Example**:
```java
@Cacheable(cacheNames = "allPosts",
    key = "'page:' + #page + 'size:' + #size + 'sort:' + #sortBy + 'order:' + #order")
public PaginatedResponse<GetPostDTO> getAllPosts(...) {
    // ...
}
```

### 4. Comments Cache (`comments`)

**Purpose**: Caches comment data and lists

**Key Formats**:
- Individual: Comment ID
- By Post: `'post:' + postId`

**Cached By**:
- `CommentService.getCommentById(String commentId)`
- `CommentService.getCommentsByPostId(Long postId)`

**Evicted On**:
- Comment creation (clears all)
- Comment deletion (clears all)
- User updates (clears related)

### 5. Tags Cache (`tags`)

**Purpose**: Caches popular tags

**Key Format**: `'popular'`

**Cached By**:
- `TagService.getPopularTags()`

**Evicted On**:
- Manual refresh via `/api/v1/tags/refresh`

## Cache Metrics

### Metrics Tracked

For each cache, the following metrics are tracked:

| Metric | Description | Type |
|--------|-------------|------|
| **Hits** | Number of successful cache retrievals | Counter |
| **Misses** | Number of cache misses (data not in cache) | Counter |
| **Hit Rate** | Percentage of requests served from cache | Percentage |
| **Miss Rate** | Percentage of requests requiring database lookup | Percentage |
| **Total Requests** | Total cache access attempts (hits + misses) | Counter |
| **Puts** | Number of items added to cache | Counter |
| **Evictions** | Number of items removed from cache | Counter |
| **Clears** | Number of times cache was completely cleared | Counter |

### Calculating Hit Rate

```
Hit Rate = (Hits / Total Requests) × 100%
Miss Rate = (Misses / Total Requests) × 100%
```

**Example**:
- Hits: 850
- Misses: 150
- Total Requests: 1000
- Hit Rate: 85.00%
- Miss Rate: 15.00%

## API Endpoints

### Get All Cache Metrics

**Endpoint**: `GET /api/metrics/performance/cache`

**Response**:
```json
{
  "totalCaches": 5,
  "timestamp": "2026-02-02T10:30:00.000",
  "caches": {
    "users": {
      "cacheName": "users",
      "hits": 850,
      "misses": 150,
      "hitRate": "85.00%",
      "missRate": "15.00%",
      "totalRequests": 1000,
      "puts": 200,
      "evictions": 10,
      "clears": 1
    },
    "posts": {
      "cacheName": "posts",
      "hits": 523,
      "misses": 87,
      "hitRate": "85.74%",
      "missRate": "14.26%",
      "totalRequests": 610,
      "puts": 120,
      "evictions": 5,
      "clears": 2
    }
    // ... other caches
  }
}
```

### Get Cache Summary

**Endpoint**: `GET /api/metrics/performance/cache/summary`

**Response**:
```json
{
  "totalCaches": 5,
  "totalHits": 1523,
  "totalMisses": 287,
  "totalRequests": 1810,
  "overallHitRate": "84.14%",
  "totalPuts": 342,
  "totalEvictions": 12,
  "bestPerformingCache": {
    "name": "users",
    "hitRate": "92.31%"
  },
  "worstPerformingCache": {
    "name": "allPosts",
    "hitRate": "67.45%"
  },
  "timestamp": "2026-02-02T10:30:00.000"
}
```

### Get Specific Cache Metrics

**Endpoint**: `GET /api/metrics/performance/cache/{cacheName}`

**Example**: `GET /api/metrics/performance/cache/users`

**Response**:
```json
{
  "cacheName": "users",
  "hits": 850,
  "misses": 150,
  "hitRate": "85.00%",
  "missRate": "15.00%",
  "totalRequests": 1000,
  "puts": 200,
  "evictions": 10,
  "clears": 1,
  "timestamp": "2026-02-02T10:30:00.000"
}
```

### Reset Cache Statistics

**Endpoint**: `DELETE /api/metrics/performance/cache/reset`

**Response**:
```json
{
  "status": "success",
  "message": "All cache metrics have been reset"
}
```

**Note**: This resets statistics only, not the cache contents.

## Metrics Export

### Export Cache Metrics Only

**Endpoint**: `POST /api/metrics/performance/cache/export-log`

**Creates**: `metrics/YYYYMMDD-HHmmss-cache-metrics.log`

**Response**:
```json
{
  "status": "success",
  "message": "Cache metrics exported to application log and metrics folder"
}
```

### Export Combined Metrics

**Endpoint**: `POST /api/metrics/performance/export-all`

**Creates**: `metrics/YYYYMMDD-HHmmss-combined-metrics.log`

**Includes**:
- Performance metrics (method execution times)
- Cache metrics (hit rates, statistics)

**Response**:
```json
{
  "status": "success",
  "message": "Combined performance and cache metrics exported to application log and metrics folder"
}
```

### Export File Format

**Example cache metrics export**:
```
================================================================================
CACHE METRICS SUMMARY
================================================================================

Overall Cache Statistics:
  Total Caches: 5
  Total Hits: 1523
  Total Misses: 287
  Total Requests: 1810
  Overall Hit Rate: 84.14%
  Total Puts: 342
  Total Evictions: 12
  Best Performing Cache: users (92.31%)
  Worst Performing Cache: allPosts (67.45%)

--------------------------------------------------------------------------------
Individual Cache Details:
--------------------------------------------------------------------------------

Cache: users
  Hits: 850
  Misses: 150
  Hit Rate: 85.00%
  Miss Rate: 15.00%
  Total Requests: 1000
  Puts: 200
  Evictions: 10
  Clears: 1

Cache: posts
  Hits: 523
  Misses: 87
  Hit Rate: 85.74%
  Miss Rate: 14.26%
  Total Requests: 610
  Puts: 120
  Evictions: 5
  Clears: 2

...
```

## Performance Optimization

### Improving Hit Rates

#### 1. Identify Low-Performing Caches

```bash
curl http://localhost:8080/api/metrics/performance/cache/summary
```

Look for caches with:
- Hit rates < 70%
- High eviction counts
- Unusual miss patterns

#### 2. Analyze Cache Keys

Ensure cache keys are:
- **Consistent**: Same input produces same key
- **Unique**: Different data gets different keys
- **Stable**: Keys don't change unexpectedly

#### 3. Review Eviction Strategies

High evictions may indicate:
- Cache size too small
- Too many updates
- Inefficient eviction patterns

### Cache Tuning Examples

#### Increasing Cache Time-to-Live

```java
@Cacheable(cacheNames = "users",
           key = "'profile:' + #userID",
           condition = "#userID != null")
public GetUserDTO getUserById(UUID userID) {
    // ...
}
```

#### Conditional Caching

```java
@Cacheable(cacheNames = "posts",
           key = "#postId",
           unless = "#result == null")
public GetPostDTO getPostById(Long postId) {
    // ...
}
```

## Best Practices

### 1. Monitor Regularly

- Check cache summary daily
- Set up alerts for hit rates < 70%
- Export metrics weekly for trend analysis

### 2. Strategic Cache Eviction

- Evict specific keys when possible
- Avoid clearing entire caches
- Use targeted eviction in updates

**Good**:
```java
@CacheEvict(cacheNames = "posts", key = "#postId")
public void updatePost(Long postId, UpdatePostDTO dto) {
    // Only evicts this specific post
}
```

**Avoid**:
```java
@CacheEvict(cacheNames = "posts", allEntries = true)
public void updatePost(Long postId, UpdatePostDTO dto) {
    // Clears ALL posts from cache
}
```

### 3. Cache Appropriate Data

**Good candidates for caching**:
- Frequently accessed data
- Relatively static data
- Expensive database queries
- Computed results

**Poor candidates**:
- Highly dynamic data
- User-specific sensitive data
- Very large objects
- Rarely accessed data

### 4. Monitor Cache Size

Keep track of:
- Number of cached items
- Memory usage
- Eviction frequency

### 5. Test Cache Behavior

Include cache testing in your test suite:
```java
@Test
void testUserCaching() {
    // First call - cache miss
    GetUserDTO user1 = userService.getUserById(userId);

    // Second call - should be cache hit
    GetUserDTO user2 = userService.getUserById(userId);

    // Verify caching worked
    CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("users");
    assertTrue(stats.getHits() > 0);
}
```

## Troubleshooting

### Low Hit Rates

**Symptoms**: Hit rate < 60%

**Possible causes**:
1. Cache keys not consistent
2. Data changes too frequently
3. Cache warming not implemented
4. Cache size too small

**Solutions**:
- Review cache key generation
- Implement cache warming on startup
- Adjust eviction strategy
- Increase cache size

### High Evictions

**Symptoms**: Evictions/puts ratio > 0.5

**Possible causes**:
1. Cache size too small
2. Too many update operations
3. Aggressive eviction policy

**Solutions**:
- Increase cache capacity
- Optimize update patterns
- Review eviction conditions

### Cache Stampede

**Symptoms**: Sudden spike in misses after eviction

**Solution**: Implement cache warming:
```java
@PostConstruct
public void warmupCache() {
    // Pre-load frequently accessed data
    popularTagsService.getPopularTags();
}
```

## Integration Example

### Full Integration Test

```java
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CacheIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private PerformanceMetricsService metricsService;

    @Test
    @Order(1)
    void testCacheWarmup() {
        // Generate some cache hits
        for (int i = 0; i < 10; i++) {
            userService.getUserById(testUserId);
        }

        // Check cache metrics
        Map<String, Object> metrics = metricsService.getCacheMetrics("users");
        assertNotNull(metrics.get("hitRate"));
    }

    @Test
    @Order(2)
    void testCacheEviction() {
        // Update user - should evict cache
        userService.updateUser(testUserId, updateDTO);

        // Verify eviction happened
        CacheStatistics stats = CacheConfig.getAllCacheStatistics().get("users");
        assertTrue(stats.getEvictions() > 0);
    }
}
```

## Summary

The cache monitoring system provides comprehensive insights into application performance:

✅ Track hit/miss rates across all caches
✅ Identify performance bottlenecks
✅ Export metrics for analysis
✅ Monitor cache efficiency in real-time
✅ Optimize based on data-driven decisions

For more information, see:
- [Performance Metrics Guide](PERFORMANCE_METRICS_GUIDE.md)
- [AOP Implementation Guide](AOP_IMPLEMENTATION_GUIDE.md)
