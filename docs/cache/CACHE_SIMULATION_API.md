# Cache Performance Simulation API

Complete API documentation for PRE_CACHE vs POST_CACHE performance simulation endpoints.

**Base URL:** `/api/metrics/performance`

---

## Endpoints

### 1. Run Full Simulation

**POST** `/simulation/run`

Runs PRE_CACHE vs POST_CACHE performance simulation for all cached methods.

**Request:**
```bash
curl -X POST http://localhost:8080/api/metrics/performance/simulation/run
```

**Response:**
```json
{
  "simulationStartTime": "2026-02-16T10:30:00",
  "iterations": 5,
  "methodResults": [
    {
      "method": "PostService.getAllPosts()",
      "cached": true,
      "preCache": {
        "description": "Without cache - every call hits database",
        "timesMs": [45, 38, 42, 40, 39],
        "avgMs": 40.8,
        "minMs": 38,
        "maxMs": 45
      },
      "postCache": {
        "description": "With cache - first call misses, subsequent calls hit cache",
        "cacheMissTimeMs": 43,
        "cacheHitTimesMs": [1, 0, 1, 0, 1],
        "cacheHitAvgMs": 0.6,
        "cacheHitMinMs": 0,
        "cacheHitMaxMs": 1
      },
      "improvement": {
        "avgTimeReductionMs": 40.2,
        "avgTimeReductionPercent": "98.53%",
        "speedupFactor": "68.0x"
      }
    },
    {
      "method": "PostService.getPostById(1)",
      "cached": true,
      "preCache": {
        "description": "Without cache - every call hits database",
        "timesMs": [18, 15, 16, 17, 15],
        "avgMs": 16.2,
        "minMs": 15,
        "maxMs": 18
      },
      "postCache": {
        "description": "With cache - first call misses, subsequent calls hit cache",
        "cacheMissTimeMs": 17,
        "cacheHitTimesMs": [0, 1, 0, 1, 0],
        "cacheHitAvgMs": 0.4,
        "cacheHitMinMs": 0,
        "cacheHitMaxMs": 1
      },
      "improvement": {
        "avgTimeReductionMs": 15.8,
        "avgTimeReductionPercent": "97.53%",
        "speedupFactor": "40.5x"
      }
    }
  ],
  "summary": {
    "totalMethodsSimulated": 8,
    "cachedMethods": 7,
    "uncachedMethods": 1,
    "overallAvgPreCacheMs": "25.50",
    "overallAvgPostCacheMs": "0.75",
    "overallImprovementPercent": "97.06%",
    "recommendation": "Cache performance simulation complete. Found 7 method(s) with significant cache benefit (>50ms pre-cache). Use '/api/metrics/cache' to monitor real-time cache hit rates."
  },
  "simulationEndTime": "2026-02-16T10:30:05"
}
```

---

### 2. Simulate Specific Method

**POST** `/simulation/method/{methodType}?resourceId={id}`

Simulates a specific method by name.

**Parameters:**

| Parameter | Type | Location | Required | Description |
|-----------|------|----------|----------|-------------|
| `methodType` | string | path | Yes | Method to simulate: `getAllPosts`, `getPostById`, `getCommentsByPostId`, `getPopularTags`, `getAllComments` |
| `resourceId` | long | query | Conditional | Required for `getPostById` and `getCommentsByPostId` |

**Examples:**

```bash
# Simulate getAllPosts
curl -X POST http://localhost:8080/api/metrics/performance/simulation/method/getAllPosts

# Simulate getPostById (requires resourceId)
curl -X POST "http://localhost:8080/api/metrics/performance/simulation/method/getPostById?resourceId=1"

# Simulate getCommentsByPostId (requires resourceId)
curl -X POST "http://localhost:8080/api/metrics/performance/simulation/method/getCommentsByPostId?resourceId=1"

# Simulate getPopularTags
curl -X POST http://localhost:8080/api/metrics/performance/simulation/method/getPopularTags

# Simulate getAllComments (not cached - shows baseline)
curl -X POST http://localhost:8080/api/metrics/performance/simulation/method/getAllComments
```

**Response:**
```json
{
  "method": "PostService.getPostById(1)",
  "cached": true,
  "preCache": {
    "description": "Without cache - every call hits database",
    "timesMs": [18, 15, 16, 17, 15],
    "avgMs": 16.2,
    "minMs": 15,
    "maxMs": 18
  },
  "postCache": {
    "description": "With cache - first call misses, subsequent calls hit cache",
    "cacheMissTimeMs": 17,
    "cacheHitTimesMs": [0, 1, 0, 1, 0],
    "cacheHitAvgMs": 0.4,
    "cacheHitMinMs": 0,
    "cacheHitMaxMs": 1
  },
  "improvement": {
    "avgTimeReductionMs": 15.8,
    "avgTimeReductionPercent": "97.53%",
    "speedupFactor": "40.5x"
  }
}
```

---

### 3. Simulate getAllPosts

**POST** `/simulation/getAllPosts`

Simulates `PostService.getAllPosts()` method.

**Request:**
```bash
curl -X POST http://localhost:8080/api/metrics/performance/simulation/getAllPosts
```

**Response:** Same structure as method simulation above.

---

### 4. Simulate getPostById

**POST** `/simulation/getPostById/{postId}`

Simulates `PostService.getPostById()` method.

**Parameters:**

| Parameter | Type | Location | Required | Description |
|-----------|------|----------|----------|-------------|
| `postId` | long | path | Yes | ID of the post to simulate |

**Request:**
```bash
curl -X POST http://localhost:8080/api/metrics/performance/simulation/getPostById/1
```

**Response:** Same structure as method simulation above.

---

### 5. Simulate getCommentsByPostId

**POST** `/simulation/getCommentsByPostId/{postId}`

Simulates `CommentService.getAllCommentsByPostId()` method.

**Parameters:**

| Parameter | Type | Location | Required | Description |
|-----------|------|----------|----------|-------------|
| `postId` | long | path | Yes | ID of the post to get comments for |

**Request:**
```bash
curl -X POST http://localhost:8080/api/metrics/performance/simulation/getCommentsByPostId/1
```

**Response:** Same structure as method simulation above.

---

### 6. Simulate getPopularTags

**POST** `/simulation/getPopularTags`

Simulates `TagService.getPopularTags()` method.

**Request:**
```bash
curl -X POST http://localhost:8080/api/metrics/performance/simulation/getPopularTags
```

**Response:** Same structure as method simulation above.

---

## How Simulation Works

### PRE_CACHE Simulation
- Cache is cleared before **each call**
- Every call forces a database query
- Measures baseline performance without caching
- Runs 5 iterations by default

### POST_CACHE Simulation
- Cache is cleared once at the start
- **First call** = cache miss (populates cache)
- **Subsequent calls** = cache hits (return from memory)
- Runs 5 iterations for cache hits
- Demonstrates real-world cached performance

### Metrics Calculated

| Metric | Description |
|--------|-------------|
| `timesMs` | Individual execution times in milliseconds |
| `avgMs` | Average execution time |
| `minMs` | Minimum execution time |
| `maxMs` | Maximum execution time |
| `cacheMissTimeMs` | Time for first call (cache population) |
| `cacheHitTimesMs` | Times for subsequent cached calls |
| `avgTimeReductionMs` | Average time saved by caching |
| `avgTimeReductionPercent` | Percentage improvement |
| `speedupFactor` | How many times faster (e.g., "40.5x") |

---

## Methods Tested

| Method | Cached? | Cache Name | TTL |
|--------|---------|------------|-----|
| `PostService.getAllPosts()` | ✅ Yes | `postListCache` | 5 min |
| `PostService.getPostById()` | ✅ Yes | `postsCache` | 15 min |
| `CommentService.getAllCommentsByPostId()` | ✅ Yes | `commentsCache` | 5 min |
| `TagService.getPopularTags()` | ✅ Yes | `tagsCache` | 15 min |
| `CommentService.getAllComments()` | ❌ No | - | - |

---

## Example Usage Flow

### 1. Run Full Simulation
```bash
# Run complete simulation for all methods
curl -X POST http://localhost:8080/api/metrics/performance/simulation/run
```

### 2. Test Specific Post
```bash
# Test caching for a specific post ID
curl -X POST http://localhost:8080/api/metrics/performance/simulation/getPostById/5
```

### 3. Compare with Real Cache Metrics
```bash
# Get actual cache hit rates
curl -X GET http://localhost:8080/api/metrics/performance/cache
```

### 4. View Cache Summary
```bash
# See overall cache performance
curl -X GET http://localhost:8080/api/metrics/performance/cache/summary
```

---

## Understanding Results

### Example Result Interpretation

```json
{
  "method": "PostService.getAllPosts()",
  "preCache": { "avgMs": 106 },
  "postCache": { "cacheHitAvgMs": 0.6 },
  "improvement": {
    "avgTimeReductionMs": 105.4,
    "avgTimeReductionPercent": "99.43%",
    "speedupFactor": "176.7x"
  }
}
```

**Interpretation:**
- Without cache: ~106ms per call (database query every time)
- With cache: ~0.6ms per call (served from memory)
- **Result**: Cache makes this endpoint **176x faster**, saving 105ms per request
- With 1000 requests/day, this saves ~105 seconds of database query time

---

## Prerequisites

Before running simulations:

1. **Database must have data**
   - At least 1 post with comments
   - Some tags in the system

2. **Application must be running**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Caching must be enabled**
   - Spring Cache is configured in `CacheConfig.java`
   - Caffeine cache manager is active

---

## Error Responses

### No Posts Found
```json
{
  "error": "No posts found in database. Please create some posts first."
}
```

**Solution:** Create posts via `/api/posts` endpoint first.

### Invalid Method Type
```json
{
  "timestamp": "2026-02-16T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Unknown method type: invalidMethod. Valid options: getAllPosts, getPostById, getCommentsByPostId, getPopularTags, getAllComments"
}
```

**Solution:** Use valid method type from the list.

### Missing Resource ID
```json
{
  "timestamp": "2026-02-16T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "resourceId required for getPostById"
}
```

**Solution:** Provide `resourceId` query parameter.

---

## Configuration

Default simulation settings (in `CachePerformanceSimulationService.java`):

```java
private static final int SIMULATION_ITERATIONS = 5;
```

To change iterations, modify this constant and recompile.

---

## Related Endpoints

| Endpoint | Purpose |
|----------|---------|
| `GET /api/metrics/performance` | View all performance metrics |
| `GET /api/metrics/performance/cache` | View cache statistics |
| `GET /api/metrics/performance/cache/summary` | Cache summary |
| `DELETE /api/metrics/performance/cache/reset` | Reset cache stats |
| `POST /api/metrics/performance/export-log` | Export metrics to file |

---

## Swagger/OpenAPI Documentation

Access interactive API documentation at:

```
http://localhost:8080/swagger-ui.html
```

Look for the **"5. Performance Metrics"** section.
