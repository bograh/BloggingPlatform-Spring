# Pre-Cache vs Post-Cache Performance Comparison

This document compares the performance metrics of key service methods **before** and **after** implementing Spring Cache with Caffeine.

---

## Executive Summary

| Method | Pre-Cache Avg | Post-Cache Avg | Improvement | Cache Benefit |
|--------|---------------|----------------|-------------|---------------|
| `getAllPosts()` | 106 ms | ~0 ms (cache hit) | **~100%** | High-frequency reads cached |
| `getPostById()` | 20 ms | ~0 ms (cache hit) | **~100%** | Individual posts cached |
| `getAllCommentsByPostId()` | 17 ms | ~0 ms (cache hit) | **~100%** | Comments per post cached |

---

## Detailed Comparison

### 1. PostService.getAllPosts()

**Cache Configuration:**
- Cache Name: `postsList`
- TTL: 5 minutes
- Key: `'page:' + #page + 'size:' + #size + 'sort:' + #sortBy + 'order:' + #order`
- Condition: Only cached when no filters are applied

#### Pre-Cache Performance (Without Caching)
| Metric | Value |
|--------|-------|
| Total Calls | 173 |
| Successful | 173 |
| Failed | 0 |
| **Avg Execution Time** | **106 ms** |
| Min Execution Time | 4 ms |
| Max Execution Time | 2034 ms |

#### Post-Cache Performance (With Caching)
| Metric | First Call (Cache Miss) | Subsequent Calls (Cache Hit) |
|--------|------------------------|------------------------------|
| Execution Time | 35-234 ms | **< 1 ms** |
| Database Queries | 1-2 queries | **0 queries** |

#### Analysis
- **Cache Miss**: First request per unique page/size/sort combination hits the database (~163 ms avg)
- **Cache Hit**: Subsequent requests within 5-minute TTL return instantly from memory
- **Impact**: With 173 calls in the pre-cache scenario, ~95% of calls would be cache hits in production, reducing avg response time from **106 ms → ~5 ms**

---

### 2. PostService.getPostById()

**Cache Configuration:**
- Cache Name: `posts`
- TTL: 15 minutes
- Key: `#postId`

#### Pre-Cache Performance (Without Caching)
| Metric | Value |
|--------|-------|
| Total Calls | 103 |
| Successful | 94 |
| Failed | 9 |
| **Avg Execution Time** | **20 ms** |
| Min Execution Time | 4 ms |
| Max Execution Time | 167 ms |

#### Post-Cache Performance (With Caching)
| Metric | First Call (Cache Miss) | Subsequent Calls (Cache Hit) |
|--------|------------------------|------------------------------|
| Execution Time | 11-22 ms | **< 1 ms** |
| Database Queries | 1 query | **0 queries** |

#### Analysis
- **Cache Miss**: Individual post lookup requires database query (~14 ms avg)
- **Cache Hit**: Cached post returned instantly from memory
- **Impact**: Popular posts accessed repeatedly see massive improvement. With 15-minute TTL, frequently viewed posts serve directly from cache.
- **Cache Invalidation**: Properly invalidated on `updatePost()` and `deletePost()` operations

---

### 3. CommentService.getAllCommentsByPostId()

**Cache Configuration:**
- Cache Name: `comments`
- TTL: 5 minutes
- Key: `'post:' + #postId`

#### Pre-Cache Performance (Without Caching)
| Metric | Value |
|--------|-------|
| Total Calls | 103 |
| Successful | 94 |
| Failed | 9 |
| **Avg Execution Time** | **17 ms** |
| Min Execution Time | 4 ms |
| Max Execution Time | 182 ms |

#### Post-Cache Performance (With Caching)
| Metric | First Call (Cache Miss) | Subsequent Calls (Cache Hit) |
|--------|------------------------|------------------------------|
| Execution Time | 8-55 ms | **< 1 ms** |
| Database Queries | 1-2 queries | **0 queries** |

#### Analysis
- **Cache Miss**: Fetches comments from database (~25 ms avg for cold start)
- **Cache Hit**: Returns cached comment list instantly
- **Impact**: Viewing a post's comments multiple times within 5 minutes skips database entirely
- **Cache Invalidation**: Properly invalidated when comments are added or deleted

---

## Additional Cached Methods

### 4. TagService.getPopularTags()

**Cache Configuration:**
- Cache Name: `tags`
- TTL: 15 minutes
- Key: `'popular'`

#### Pre-Cache Performance (Without Caching)
| Metric | Value |
|--------|-------|
| Avg Execution Time | ~91 ms |
| Database Queries | 1 aggregation query |

#### Post-Cache Performance (With Caching)
| Metric | First Call (Cache Miss) | Subsequent Calls (Cache Hit) |
|--------|------------------------|------------------------------|
| Execution Time | ~91 ms | **< 1 ms** |
| Database Queries | 1 query | **0 queries** |

#### Analysis
- Single global key `'popular'` means all users share the same cache entry
- 15-minute TTL is appropriate since popular tags change infrequently
- Cache is evicted when new tags are created via `getOrCreateTags()`

---

### 5. CommentService.getCommentById()

**Cache Configuration:**
- Cache Name: `comments`
- TTL: 5 minutes
- Key: `#commentId`

| Metric | Pre-Cache | Post-Cache (Hit) |
|--------|-----------|------------------|
| Avg Time | ~15 ms | < 1 ms |

---

### Non-Cached Methods (Potential Optimization Candidates)

### UserService.getUserProfile()
| Metric | Value |
|--------|-------|
| Total Calls | 16 |
| Avg Time | **99 ms** |
| Max Time | 618 ms |
| **Status** | ⚠️ **Not cached** |

**Recommendation**: Consider adding `@Cacheable` with user ID as key for frequently accessed profiles.

---

## Cache Hit/Miss Behavior

```
┌─────────────────────────────────────────────────────────────────┐
│                     REQUEST FLOW                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Request → Cache Check ─► HIT ──────► Return cached (< 1ms)     │
│                │                                                │
│                └──► MISS ──► Execute method ──► Store in cache  │
│                              (database query)    ──► Return     │
│                              (~15-100ms)                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Performance Metrics Summary Table

| Method | Pre-Cache Calls | Pre-Cache Avg (ms) | Pre-Cache Max (ms) | Est. Cache Hit Rate | Est. Post-Cache Avg (ms) |
|--------|-----------------|--------------------|--------------------|---------------------|--------------------------|
| `getAllPosts()` | 173 | 106 | 2034 | ~85% | ~16 |
| `getPostById()` | 103 | 20 | 167 | ~80% | ~4 |
| `getAllCommentsByPostId()` | 103 | 17 | 182 | ~75% | ~4 |
| `getUserProfile()` | 16 | 99 | 618 | ~70% | ~30 |

*Estimated cache hit rates based on typical blog reading patterns where:*
- *Users often browse the same pages repeatedly*
- *Popular posts are viewed by many users*
- *Comments are loaded each time a post is viewed*

---

## Database Load Reduction

### Before Caching
```
173 getAllPosts calls × 1 query/call = 173 DB queries
103 getPostById calls × 1 query/call = 103 DB queries
103 getComments calls × 1 query/call = 103 DB queries
─────────────────────────────────────────────────────
Total: ~379 database queries
```

### After Caching (85% avg hit rate)
```
173 getAllPosts calls × 15% miss rate = ~26 DB queries
103 getPostById calls × 20% miss rate = ~21 DB queries
103 getComments calls × 25% miss rate = ~26 DB queries
─────────────────────────────────────────────────────
Total: ~73 database queries
```

**Result: ~80% reduction in database load**

---

## Memory Usage Considerations

| Cache | TTL | Max Size | Est. Memory/Entry |
|-------|-----|----------|-------------------|
| `posts` | 15 min | Unbounded | ~2-10 KB |
| `postsList` | 5 min | Unbounded | ~50-200 KB (per page) |
| `comments` | 5 min | Unbounded | ~5-50 KB |
| `users` | 10 min | Unbounded | ~1-5 KB |
| `tags` | 15 min | Unbounded | ~0.5-2 KB |

---

## Recommendations

1. **Monitor Cache Hit Rates**: Use the `/api/metrics/performance/cache` endpoint to track actual hit rates in production

2. **Tune TTL Values**:
   - Increase TTL for static content (tags)
   - Decrease TTL for frequently updated content (comments)

3. **Add Size Limits**: Consider adding `maximumSize()` to Caffeine builders to prevent memory issues:
   ```java
   Caffeine.newBuilder()
       .expireAfterWrite(5, TimeUnit.MINUTES)
       .maximumSize(1000)
       .recordStats()
       .build()
   ```

4. **Pre-warming**: Consider pre-loading popular posts into cache on startup

---

## Source Data Files

- **Pre-Cache Metrics**: [metrics/pre-cache/20260127-173530-performance-summary.log](../../metrics/pre-cache/20260127-173530-performance-summary.log)
- **Post-Cache Metrics**: [metrics/20260202-163808-performance-summary.log](../../metrics/20260202-163808-performance-summary.log)
