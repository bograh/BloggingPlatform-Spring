# Lab Review Guide: Performance, Concurrency & Async Optimization

**Course Phase:** System Performance, Responsiveness & Scalability
**Stack:** Spring Boot 3.x · Java 21 · JPA · Caffeine Cache · AOP
**Last Updated:** February 24, 2026

---

## Table of Contents

1. [Epic 1 — Profiling & Bottleneck Analysis](#epic-1--profiling--bottleneck-analysis)
2. [Epic 2 — Asynchronous Programming](#epic-2--asynchronous-programming)
3. [Epic 3 — Concurrency & Thread Safety](#epic-3--concurrency--thread-safety)
4. [Epic 4 — Data & Algorithmic Optimization](#epic-4--data--algorithmic-optimization)
5. [Epic 5 — Metrics Collection & Reporting](#epic-5--metrics-collection--reporting)
6. [Cross-Cutting: Thread Pool Tuning](#cross-cutting-thread-pool-tuning)
7. [Self-Check Questions](#self-check-questions)
8. [Common Pitfalls](#common-pitfalls)
9. [Evaluation Checklist](#evaluation-checklist)

---

## Epic 1 — Profiling & Bottleneck Analysis

### Concepts to Know

**Profiling** is the act of measuring a running application to find where time, CPU, and memory are spent. Tools differ in overhead and depth:

| Tool | Overhead | Best For |
|------|----------|----------|
| VisualVM | Low | Quick CPU/heap sampling |
| JProfiler | Medium | Method-level CPU + allocation |
| Java Flight Recorder (JFR) | Very Low (< 1%) | Production-safe continuous recording |
| Spring Boot Actuator (`/actuator/metrics`) | None | JVM, HTTP request stats at runtime |

### What Was Measured in This Project

The `PerformanceMonitoringAspect` captures every service method call via AOP:

```
[PERFORMANCE] Method: PostService.getPostById(..) | Execution Time: 42 ms | Status: SUCCESS
[PERFORMANCE] SLOW OPERATION: FeedAggregationService.aggregateFeed(..) took 1340 ms
```

Key file: [src/main/java/org/amalitech/bloggingplatformspring/aop/PerformanceMonitoringAspect.java](../src/main/java/org/amalitech/bloggingplatformspring/aop/PerformanceMonitoringAspect.java)

The aspect uses a `ConcurrentHashMap<String, MethodMetrics>` to accumulate:
- Total invocation count
- Cumulative execution time
- Success / failure split
- Max and average latency

The `SLOW_THRESHOLD_MS = 1000` constant defines what counts as a "slow" operation for `WARN`-level logging.

### Identified Bottlenecks

Based on the baseline profiling reports in [`docs/performance/`](../docs/performance/):

| Endpoint / Operation | Baseline Latency | Root Cause |
|----------------------|-----------------|------------|
| `GET /api/posts` (all posts) | ~800 ms | Full table scan, no caching |
| Feed aggregation | ~1300 ms | N+1 queries, sequential source fetching |
| Bulk comment moderation | Blocking main thread | Synchronous batch loop |
| Notification dispatch | Blocking HTTP thread | No outbox / async offload |
| Report generation | Minutes | Single-threaded data aggregation |

### Review Questions — Epic 1

1. What is the difference between *CPU sampling* and *CPU tracing* in a profiler?
2. How does `@Around` AOP advice allow non-invasive latency measurement?
3. Why must the `ConcurrentHashMap` be used for `metricsMap` rather than `HashMap`?
4. What is a "slow threshold" and how would you choose its value?
5. Where are the baseline performance screenshots/CSV files stored in this project?

---

## Epic 2 — Asynchronous Programming

### Core Tools Used

| Mechanism | Where Used in Project |
|-----------|----------------------|
| `@Async("applicationTaskExecutor")` | `FeedAggregationService`, `BulkCommentModerationService`, `AsyncImageUploadService`, `NotificationOutboxProcessor`, `LiveTrendingScoreService` |
| `CompletableFuture<T>` | Return type of every `@Async` method; combined via `CompletableFuture.allOf(...)` |
| `@Scheduled` | Periodic notification flush, index rebuild, cache cleanup |

### Feed Aggregation — Parallel Pattern

File: [src/main/java/org/amalitech/bloggingplatformspring/services/FeedAggregationService.java](../src/main/java/org/amalitech/bloggingplatformspring/services/FeedAggregationService.java)

```java
CompletableFuture<List<FeedItemDTO>> recentFuture   = fetchRecentPostsAsync(limit);
CompletableFuture<List<FeedItemDTO>> trendingFuture = fetchTrendingPostsAsync(limit);
CompletableFuture<List<FeedItemDTO>> popularFuture  = fetchPopularPostsAsync(limit);

// Fan-out: all three execute concurrently on the thread pool
CompletableFuture.allOf(recentFuture, trendingFuture, popularFuture).join();
```

**Why does this improve latency?**
Sequential execution: `T_recent + T_trending + T_popular`
Parallel execution: `max(T_recent, T_trending, T_popular)`

If each source takes ~300 ms, sequential = 900 ms, parallel ≈ 300 ms.

### Notification Outbox — Reliable Async Pattern

File: [src/main/java/org/amalitech/bloggingplatformspring/services/NotificationOutboxProcessor.java](../src/main/java/org/amalitech/bloggingplatformspring/services/NotificationOutboxProcessor.java)

The **Outbox Pattern** decouples the action (writing a notification record) from the side effect (sending the email):

1. `queueNotification()` persists a `NotificationOutbox` record with `status = PENDING` — this is synchronous and fast.
2. A `@Scheduled` method polls for `PENDING` records and processes them asynchronously in batches.
3. Failures increment `retryCount`; after `MAX_RETRY_COUNT = 3` the record is marked `FAILED`.

**Key benefit:** the HTTP request returns immediately; email delivery is decoupled and retryable.

### Image Upload — Fire-and-Forget Async

File: [src/main/java/org/amalitech/bloggingplatformspring/services/AsyncImageUploadService.java](../src/main/java/org/amalitech/bloggingplatformspring/services/AsyncImageUploadService.java)

```java
// Synchronous: validate, create DB record, return DTO
ImageUploadDTO dto = initiateUpload(postId, file);

// Asynchronous: heavy I/O (file write, thumbnail generation) happens off-thread
processUploadAsync(saved.getId(), file.getBytes());
```

The client receives an immediate response with a `status = PENDING` tracking ID and polls `/api/images/{id}/status` for completion.

### Bulk Moderation — Async Task Submission

File: [src/main/java/org/amalitech/bloggingplatformspring/services/BulkCommentModerationService.java](../src/main/java/org/amalitech/bloggingplatformspring/services/BulkCommentModerationService.java)

- `BATCH_SIZE = 50` — comments processed in chunks to avoid memory spikes.
- `MAX_COMMENTS_PER_TASK = 1000` — guards against unbounded requests.
- Task status (`PENDING → PROCESSING → COMPLETED`) tracked in `ModerationTask` entity.

### Review Questions — Epic 2

1. What must be true about the method signature of an `@Async` method for Spring's proxy to intercept it?
2. What happens if you call an `@Async`-annotated method from within the *same* bean?
3. Explain the difference between `.join()` and `.get()` on a `CompletableFuture`.
4. Why is the `@Scheduled` outbox processor better than calling the email sender directly in the controller?
5. What is `CallerRunsPolicy` and when does it activate in `AsyncConfig`?

---

## Epic 3 — Concurrency & Thread Safety

### Thread-Safe Data Structures Used

| Structure | Location | Purpose |
|-----------|----------|---------|
| `ConcurrentHashMap<String, MethodMetrics>` | `PerformanceMonitoringAspect` | Metrics accumulation across threads |
| `ConcurrentHashMap<Long, PostSnapshot>` | `PostRankingIndexService` | Post snapshot store |
| `ConcurrentHashMap<String, CacheStatistics>` | `CacheConfig` | Per-cache hit/miss counters |
| `ConcurrentSkipListSet<RankedPost>` | `PostRankingIndexService` | Sorted ranking indexes |
| `ConcurrentHashMap<Long, ScoreSnapshot>` | `LiveTrendingScoreService` | Previous score snapshots |
| `AtomicLong lastRefreshEpochMillis` | `PostRankingIndexService` | Lock-free TTL check |
| `AtomicInteger` / `AtomicLong` | `CacheConfig.CacheStatistics` | Lock-free counter updates |

### Ranking Index — Concurrent Read / Bulk Write Pattern

File: [src/main/java/org/amalitech/bloggingplatformspring/services/PostRankingIndexService.java](../src/main/java/org/amalitech/bloggingplatformspring/services/PostRankingIndexService.java)

```java
// Reads are constant-time because ConcurrentSkipListSet is always sorted
public List<PostResponseDTO> getTrendingPosts(int limit) {
    ensureFreshIndex();           // AtomicLong TTL check — no lock needed
    return buildTopPosts(trendingIndex, limit);
}

// Writes rebuild the full index then swap atomically
public void rebuildIndexes() {
    ConcurrentHashMap<Long, PostSnapshot> rebuilt = new ConcurrentHashMap<>();
    // ... populate rebuilt ...
    snapshotsById.clear();
    snapshotsById.putAll(rebuilt);          // atomic per-segment
    lastRefreshEpochMillis.set(...);        // atomic publish
}
```

**Why `ConcurrentSkipListSet`?**
- Thread-safe sorted set with O(log n) insert and O(log n) range queries.
- `NavigableSet` interface enables `descendingIterator()` to read the top-N posts without sorting.

### Atomic Counters vs. `synchronized`

```java
// Bad — requires monitor lock, blocks all other threads
synchronized void increment() { count++; }

// Good — CAS (compare-and-swap), no blocking
AtomicLong count = new AtomicLong(0);
count.incrementAndGet();
```

The project uses `AtomicLong` for hit/miss counters in `CacheStatistics` and for the index TTL timestamp.

### `ConcurrentHashMap.compute()` for Safe Upserts

In `PerformanceMonitoringAspect`:

```java
metricsMap.compute(methodName, (key, metrics) -> {
    if (metrics == null) metrics = new MethodMetrics(key);
    metrics.recordExecution(executionTime, success);
    return metrics;
});
```

`compute()` guarantees the lambda runs atomically for the given key — no `get()` + `put()` race condition.

### Review Questions — Epic 3

1. What is the difference between `HashMap` and `ConcurrentHashMap` in a multi-threaded context?
2. Why is `count++` not thread-safe even though it looks like one operation?
3. When would you prefer `CopyOnWriteArrayList` over `ConcurrentHashMap`?
4. What does "lock striping" mean in the context of `ConcurrentHashMap`?
5. Why is `NavigableSet` preferred over a plain `Set` for ranked lists?
6. Explain how `AtomicLong.set()` acts as a "publication barrier" for index rebuilds.

---

## Epic 4 — Data & Algorithmic Optimization

### A. Caching with Caffeine

File: [src/main/java/org/amalitech/bloggingplatformspring/config/CacheConfig.java](../src/main/java/org/amalitech/bloggingplatformspring/config/CacheConfig.java)

| Cache | TTL | Notes |
|-------|-----|-------|
| `users` | 10 min | Low churn — user data changes rarely |
| `posts` | 15 min | Individual post detail |
| `post-list` | 5 min | Listing pages — higher churn |
| `tags` | 30 min | Near-static data |
| `popular-posts` | Set on index | Backed by `PostRankingIndexService` |
| `trending-posts` | Set on index | Velocity-scored, refreshed every minute |

```java
@Cacheable(cacheNames = "posts", key = "#id")
public PostResponseDTO getPostById(Long id) { ... }

@CacheEvict(cacheNames = "posts", key = "#id")
public void deletePost(Long id) { ... }
```

**Cache hit ratio** is tracked in `CacheStatistics` and exposed at `GET /api/performance/cache-metrics`.

### B. In-Memory Ranking Index (O(log n) reads)

Traditional approach: every request → SQL `ORDER BY likes DESC LIMIT 10` → O(n log n) sort in DB.

Optimized approach:

```
rebuildIndexes() [DB query once per TTL]
    ↓
ConcurrentSkipListSet<RankedPost> always sorted
    ↓
getTrendingPosts(limit) → iterate top-N in O(limit × log n)
```

The index is refreshed on a 60-second TTL (`INDEX_TTL_MILLIS = 60_000`) checked via `AtomicLong`.

### C. Trending Score Algorithm

File: [src/main/java/org/amalitech/bloggingplatformspring/services/PostRankingIndexService.java](../src/main/java/org/amalitech/bloggingplatformspring/services/PostRankingIndexService.java)

$$\text{score} = (\text{comments} \times 10.0) + \left(\frac{1}{\text{hours\_since\_update} + 1} \times 2.0\right)$$

This is a **gravity-based scoring formula** (similar to Hacker News ranking):
- Recent posts get a recency boost that decays over time.
- High comment volume provides a sustained popularity signal.

### D. DSA Concepts Applied

| Concept | Implementation |
|---------|---------------|
| **Hash Map** | `ConcurrentHashMap` for O(1) post snapshot lookup by ID |
| **Sorted Set (Skip List)** | `ConcurrentSkipListSet` for O(log n) ranked insertion |
| **Caching** | Caffeine LRU cache with TTL eviction |
| **Batch Processing** | `BATCH_SIZE = 50` in moderation to bound memory |
| **Parallel Streams / Futures** | `CompletableFuture.allOf()` fan-out in feed aggregation |
| **Hashing** | Spring Cache key generation via `#id`, `#limit` SpEL expressions |

### E. Database Query Optimization

- `findAllWithAuthorAndTags()` — JOIN FETCH to avoid N+1 on author and tags.
- `PostCommentCountProjection` — aggregate query returning `(postId, count)` pairs instead of loading all comments.
- Pageable passed to `findAll()` to avoid full table scans on listing endpoints.

### Review Questions — Epic 4

1. What is the N+1 query problem and how does `JOIN FETCH` solve it?
2. Explain the trade-off between cache TTL duration and data staleness.
3. Why is a `ConcurrentSkipListSet` used instead of a `TreeSet` for the ranking index?
4. How does the gravity formula prevent old, high-comment posts from permanently occupying the trending list?
5. What happens to cache entries when `@CacheEvict(allEntries = true)` is called?
6. At what point does in-memory indexing become less effective than a proper search engine?

---

## Epic 5 — Metrics Collection & Reporting

### Runtime Metrics Pipeline

```
Request → RuntimeMetricsFilter (Servlet filter)
               ↓
         records latency, URI, status code
               ↓
         RuntimeMetricsService (ConcurrentHashMap store)
               ↓
         GET /api/performance/metrics  ←── admin dashboard / Postman
```

The `PerformanceMonitoringAspect` adds service-layer granularity on top of HTTP-level metrics.

### Accessing Metrics Endpoints

| Endpoint | Description | Auth Required |
|----------|-------------|---------------|
| `GET /api/performance/metrics` | Per-method invocation stats | ADMIN |
| `GET /api/performance/cache-metrics` | Hit ratio per cache | ADMIN |
| `GET /api/performance/snapshots` | Historical snapshots | ADMIN |
| `GET /actuator/metrics` | JVM / Spring internals | ADMIN |
| `GET /actuator/health` | Liveness / readiness | Public |

### `PerformanceMetricsSnapshot` Entity

Periodic snapshots are persisted to the database and exportable as CSV. Each snapshot captures:
- Timestamp
- Average / max / p95 response time
- Request rate (req/s)
- Active thread count
- JVM heap usage

File: [src/main/java/org/amalitech/bloggingplatformspring/entity/PerformanceMetricsSnapshot.java](../src/main/java/org/amalitech/bloggingplatformspring/entity/PerformanceMetricsSnapshot.java)

### Before / After Evidence Already in the Project

| Report | Location |
|--------|----------|
| Baseline performance summary | [docs/performance/BASELINE_PERFORMANCE_SUMMARY.md](../docs/performance/BASELINE_PERFORMANCE_SUMMARY.md) |
| Concurrency & thread safety tuning | [docs/performance/CONCURRENCY_THREAD_SAFETY_TUNING_REPORT.md](../docs/performance/CONCURRENCY_THREAD_SAFETY_TUNING_REPORT.md) |
| Concurrent API calls test | [docs/performance/CONCURRENT_API_CALLS_TEST_REPORT.md](../docs/performance/CONCURRENT_API_CALLS_TEST_REPORT.md) |
| Final optimization report | [docs/performance/FINAL_OPTIMIZATION_REPORT.md](../docs/performance/FINAL_OPTIMIZATION_REPORT.md) |
| Retrieval optimization | [docs/performance/RETRIEVAL_OPTIMIZATION_REPORT.md](../docs/performance/RETRIEVAL_OPTIMIZATION_REPORT.md) |
| Secured backend bottleneck report | [docs/performance/SECURED_BACKEND_BOTTLENECK_REPORT.md](../docs/performance/SECURED_BACKEND_BOTTLENECK_REPORT.md) |
| Thread-pool tuning logs | [metrics/profiling/](../metrics/profiling/) |

### Review Questions — Epic 5

1. What is the difference between *latency* (ms) and *throughput* (req/s)?
2. Why is a servlet filter (`RuntimeMetricsFilter`) the right place to record end-to-end HTTP latency?
3. What additional information does the AOP aspect add that the filter cannot provide?
4. How would you export metrics snapshots for inclusion in a final report?
5. What chart type best visualizes latency distributions — bar chart, line chart, or histogram?

---

## Cross-Cutting: Thread Pool Tuning

### `AsyncConfig` Configuration

File: [src/main/java/org/amalitech/bloggingplatformspring/config/AsyncConfig.java](../src/main/java/org/amalitech/bloggingplatformspring/config/AsyncConfig.java)

| Property | Default | Purpose |
|----------|---------|---------|
| `core-pool-size` | 8 | Minimum always-alive threads |
| `max-pool-size` | 32 | Hard ceiling on thread count |
| `queue-capacity` | 500 | Tasks buffered before rejecting |
| `keep-alive-seconds` | 60 | Idle thread eviction delay |
| `await-termination-seconds` | 30 | Graceful shutdown window |
| `rejected-handler` | `CallerRunsPolicy` | Throttle: caller executes the task |

### Tuning Formula (reference)

$$\text{Optimal threads} \approx N_{cpu} \times \left(1 + \frac{W}{C}\right)$$

Where:
- $N_{cpu}$ = number of available processor cores
- $W$ = average I/O wait time per task (ms)
- $C$ = average CPU computation time per task (ms)

For I/O-heavy blog operations (DB + network) where $W/C \approx 4$:

$$\text{threads} \approx 8 \times (1 + 4) = 40 \text{ threads}$$

The project uses `max-pool-size = 32` as a conservative start, tunable via `application.properties` without recompilation.

### `CallerRunsPolicy` Explained

When the queue is full and no new threads can be created, Spring falls back to executing the submitted task on the **calling thread**. This:
- Prevents `RejectedExecutionException` crashes.
- Acts as natural back-pressure — the HTTP thread is blocked, slowing incoming requests organically.
- Is appropriate for non-critical async work (feed aggregation, reports).

---

## Self-Check Questions

These cover the entire lab. Answer without looking at the code first.

1. Name three long-running operations in this project that were refactored to use `@Async`. For each, explain *why* it was a good candidate.
2. Draw the execution timeline for `FeedAggregationService.aggregateFeed()` showing which tasks run in parallel and on which threads.
3. A `ConcurrentHashMap` and a `HashMap` both store key-value pairs. Describe a scenario where using `HashMap` in a multi-threaded service would cause data corruption.
4. The `PostRankingIndexService` uses an `AtomicLong` for `lastRefreshEpochMillis`. Why is a regular `long` field insufficient here, even though only one thread writes it?
5. Explain step-by-step how a request for trending posts flows through the system: from the controller, through caching, through the ranking index, and back.
6. What would happen if two requests arrive simultaneously and both see `ensureFreshIndex()` needs a rebuild? Is there a race condition?
7. The notification outbox retries up to 3 times before marking a notification as `FAILED`. What design principle does this implement? What are the risks if `MAX_RETRY_COUNT` is set too high?
8. How does `CacheEvict` on `rebuildIndexes()` coordinate with `@Cacheable` on `getPopularPosts()`?
9. Describe how you would detect a thread pool saturation event at runtime without looking at logs.
10. A team member says "just add more cache TTL time to make the app faster." What are the downsides of unbounded cache TTL?

---

## Common Pitfalls

### 1. Self-Invocation Bypass of `@Async`

**Symptom:** The method runs synchronously on the calling thread despite the `@Async` annotation.

**Root cause:** Spring implements `@Async` via a proxy. When a method inside a bean calls another method on `this`, it bypasses the proxy entirely — the annotation is never seen.

**Live example in this project:** `NotificationOutboxProcessor.processPendingNotifications()` calls `this.processNotificationAsync(notification)`. Because the call goes through `this` instead of through the Spring proxy, `processNotificationAsync` executes synchronously on the scheduler thread, blocking subsequent scheduled tasks.

```java
// BROKEN — self-invocation bypasses the Spring proxy
@Scheduled(fixedRate = 30000)
public void processPendingNotifications() {
    pending.forEach(this::processNotificationAsync); // @Async is ignored
}

// FIX — inject the bean into itself (self-injection) or extract to a separate bean
@Autowired
private NotificationOutboxProcessor self; // Spring injects the proxy

@Scheduled(fixedRate = 30000)
public void processPendingNotifications() {
    pending.forEach(self::processNotificationAsync); // proxy is called
}
```

The same rule applies to `@Transactional` and any other proxy-based annotation.

---

### 2. Non-`Future` Return Type on `@Async` Method

**Symptom:** The caller receives `null` for the return value, not an exception. No error is logged.

**Root cause:** Spring's async proxy only propagates results back through `Future` / `CompletableFuture` / `void`. If you return any other type (e.g., `List<T>`), the method still executes asynchronously — but the proxy discards the return value and the caller gets `null`.

```java
// BROKEN — List<T> is not a Future; caller always gets null
@Async("applicationTaskExecutor")
public List<PostResponseDTO> getTrendingPostsAsync(int limit) {
    return postRepository.findTrending(limit); // discarded by proxy
}

// FIX — wrap in CompletableFuture so the proxy knows how to deliver the result
@Async("applicationTaskExecutor")
public CompletableFuture<List<PostResponseDTO>> getTrendingPostsAsync(int limit) {
    return CompletableFuture.completedFuture(postRepository.findTrending(limit));
}
```

---

### 3. Passing a Managed Entity into an `@Async` Method

**Symptom:** `LazyInitializationException` — no session or session already closed.

**Root cause:** JPA entities are attached to a Hibernate `Session` that lives inside the originating transaction. Once the transaction commits on the calling thread, the session closes. When the async method runs on a different thread and tries to access a `LAZY`-loaded association, there is no open session.

```java
// BROKEN — post.getTags() triggers a lazy load after session closes
@Async("applicationTaskExecutor")
public CompletableFuture<Void> processPostAsync(Post post) {
    post.getTags().forEach(...); // LazyInitializationException
}

// FIX — pass only the ID; reload the entity inside the async method
@Async("applicationTaskExecutor")
public CompletableFuture<Void> processPostAsync(Long postId) {
    Post post = postRepository.findById(postId).orElseThrow();
    post.getTags().forEach(...); // safe — new session opened by this method
}
```

This project correctly applies the fix: `BulkCommentModerationService.processTaskAsync(UUID taskId)` and `AsyncImageUploadService.processUploadAsync(Long imageId, byte[] data)` both accept IDs, not entities.

---

### 4. Silent Exception Loss in `@Async` Methods

**Symptom:** An operation silently fails — no error visible to the caller, no retry, downstream state is inconsistent.

**Root cause:** When an `@Async` method throws an unchecked exception and the caller discards the returned `CompletableFuture` (or the method returns `void`), the exception is swallowed unless you set an `AsyncUncaughtExceptionHandler`.

```java
// BROKEN — if processTaskAsync throws, the exception is lost
processTaskAsync(savedTask.getId()); // return value ignored

// FIX option A — inspect the future
CompletableFuture<Void> future = processTaskAsync(savedTask.getId());
future.exceptionally(ex -> { log.error("Task failed", ex); return null; });

// FIX option B — configure a global handler in AsyncConfig
@Override
public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (ex, method, params) ->
        log.error("Async method {} threw: {}", method.getName(), ex.getMessage(), ex);
}
```

---

### 5. `HashMap` in Shared Service State

**Symptom:** `ConcurrentModificationException` or silently dropped entries under concurrent load.

**Root cause:** `HashMap` is not thread-safe. Concurrent `put` / `get` operations can corrupt internal state (infinite loops in Java 7, lost entries in Java 8+).

```java
// BROKEN — multiple async threads share this map
private final Map<String, MethodMetrics> metricsMap = new HashMap<>();

// FIX — use the concurrent equivalent
private final ConcurrentHashMap<String, MethodMetrics> metricsMap = new ConcurrentHashMap<>();
```

For compound operations (check-then-act), use `compute()` rather than separate `get()` + `put()` calls — as done in `PerformanceMonitoringAspect`.

---

### 6. Unbounded Thread Pool Queue

**Symptom:** Heap exhaustion / `OutOfMemoryError` under sustained burst traffic; tasks enqueue faster than they are processed.

**Root cause:** `ThreadPoolTaskExecutor` with `Integer.MAX_VALUE` queue capacity (the default if not set) buffers an unlimited number of tasks.

```properties
# FIX — always set an explicit, finite queue capacity
app.async.queue-capacity=500
```

When the queue is full, the configured `RejectedExecutionHandler` activates. This project uses `CallerRunsPolicy`, which makes the submitting HTTP thread execute the task itself — providing natural back-pressure without crashing the application.

---

### 7. `@Scheduled` Method Blocked by Long-Running Synchronous Work

**Symptom:** Scheduled tasks fire late or not at all; the single scheduler thread is occupied.

**Root cause:** Spring's default `ThreadPoolTaskScheduler` uses one thread for all `@Scheduled` tasks. A method that performs long-running database queries or I/O blocks all other scheduled tasks from firing on time. This is compounded by the self-invocation problem (see Pitfall 1): if the scheduled method calls an `@Async` sibling on `this`, the async offload never happens and the scheduler thread stays blocked.

```java
// BROKEN — long DB work blocks the scheduler thread;
//           self.processNotificationAsync via 'this' makes @Async a no-op
@Scheduled(fixedRate = 30000)
public void processPendingNotifications() {
    pending.forEach(this::processNotificationAsync); // runs synchronously
}

// FIX — delegate to a separate injected bean so the proxy is invoked
@Scheduled(fixedRate = 30000)
public void processPendingNotifications() {
    pending.forEach(self::processNotificationAsync); // truly async
}
```

Alternatively, annotate the `@Scheduled` method itself with `@Async` (on a different bean) or configure `ThreadPoolTaskScheduler` with multiple threads.

---

### 8. Missing `.join()` Before Reading a `CompletableFuture` Result

**Symptom:** Empty lists or stale data returned in the response when the feed aggregation has not finished; no exception thrown.

**Root cause:** `CompletableFuture` is non-blocking by default. If you call `.join()` on each future individually before calling `allOf()`, the individual futures still run sequentially instead of in parallel.

```java
// BROKEN — joins are sequential; parallel benefit is lost
List<FeedItemDTO> recent   = fetchRecentPostsAsync(limit).join();
List<FeedItemDTO> trending = fetchTrendingPostsAsync(limit).join();
List<FeedItemDTO> popular  = fetchPopularPostsAsync(limit).join();

// FIX — start all tasks, then wait for all to finish, then read
CompletableFuture<List<FeedItemDTO>> recentFuture   = fetchRecentPostsAsync(limit);
CompletableFuture<List<FeedItemDTO>> trendingFuture = fetchTrendingPostsAsync(limit);
CompletableFuture<List<FeedItemDTO>> popularFuture  = fetchPopularPostsAsync(limit);

CompletableFuture.allOf(recentFuture, trendingFuture, popularFuture).join(); // barrier

List<FeedItemDTO> recent   = recentFuture.join();
List<FeedItemDTO> trending = trendingFuture.join();
List<FeedItemDTO> popular  = popularFuture.join();
```

Prefer `.join()` over `.get()` inside async chains — `.join()` rethrows as an unchecked `CompletionException`, which is easier to propagate through lambda pipelines.

---

### 9. `@CacheEvict` Not Paired With Every Write Path

**Symptom:** Stale data served after a post is updated or deleted; cache shows the old value until TTL expires.

**Root cause:** `@CacheEvict` only evicts the specific cache entry named in the annotation. If a single entity is cached under multiple cache names (e.g., both `posts` and `post-list`) and only one is evicted on update, the other cache continues serving the old value.

```java
// BROKEN — evicts single-post cache but post-list and trending caches stay stale
@CacheEvict(cacheNames = "posts", key = "#id")
public PostResponseDTO updatePost(Long id, UpdatePostRequest request) { ... }

// FIX — evict all affected caches atomically using @Caching
@Caching(evict = {
    @CacheEvict(cacheNames = "posts",      key = "#id"),
    @CacheEvict(cacheNames = "post-list",  allEntries = true),
    @CacheEvict(cacheNames = "popular-posts", allEntries = true),
    @CacheEvict(cacheNames = "trending-posts", allEntries = true)
})
public PostResponseDTO updatePost(Long id, UpdatePostRequest request) { ... }
```

Review every mutating service method (`create`, `update`, `delete`) and verify it evicts all caches that could contain a derived view of the modified data.

---

## Evaluation Checklist

Use this checklist to verify deliverables before submission.

### Category 1 — Profiling & Bottleneck Analysis (15 pts)

- [ ] At least one profiling tool was used and evidence (screenshot or CSV) is present.
- [ ] A baseline metrics table (latency, CPU, memory) is documented.
- [ ] At least three bottleneck areas are identified with supporting data.
- [ ] The `PerformanceMonitoringAspect` is wired and producing logs.

### Category 2 — Asynchronous Programming (20 pts)

- [ ] `FeedAggregationService` fans out three sources with `CompletableFuture.allOf()`.
- [ ] `NotificationOutboxProcessor` uses `@Async` + `@Scheduled` outbox pattern.
- [ ] `BulkCommentModerationService` processes in async batches with task tracking.
- [ ] `AsyncImageUploadService` offloads file I/O asynchronously.
- [ ] `AsyncConfig` defines the named `applicationTaskExecutor` bean.
- [ ] Thread name prefix (`blog-async-`) is visible in thread dumps.

### Category 3 — Concurrency & Thread Safety (15 pts)

- [ ] `ConcurrentHashMap` used (not `HashMap`) for all shared mutable state.
- [ ] `AtomicLong` / `AtomicInteger` used for shared counters.
- [ ] `ConcurrentSkipListSet` used for the sorted ranking index.
- [ ] `metricsMap.compute()` used for atomic upsert in `PerformanceMonitoringAspect`.
- [ ] No `synchronized` blocks where lock-free alternatives suffice.

### Category 4 — Algorithmic Optimization (15 pts)

- [ ] Caffeine cache configured with appropriate TTLs per cache name.
- [ ] `@Cacheable` / `@CacheEvict` / `@CachePut` annotations wired correctly.
- [ ] `PostRankingIndexService` avoids repeated full-table sorts via in-memory index.
- [ ] `JOIN FETCH` used to eliminate N+1 queries.
- [ ] `PostCommentCountProjection` aggregate query used instead of loading all comments.

### Category 5 — Performance Reporting (15 pts)

- [ ] Before-and-after metric comparison table exists in documentation.
- [ ] `GET /api/performance/metrics` endpoint returns per-method stats.
- [ ] `GET /api/performance/cache-metrics` returns hit ratio data.
- [ ] At least one Postman or JMeter test collection included.
- [ ] Runtime metrics CSV or snapshot is present in `metrics/`.

### Category 6 — Code Quality & Documentation (20 pts)

- [ ] All async service classes have Javadoc on public methods.
- [ ] `AsyncConfig` properties are externalized to `application.properties` (`@Value`).
- [ ] Reports in `docs/performance/` cover methodology, data, and conclusions.
- [ ] No blocking calls (e.g., `Thread.sleep()`, `Future.get()` without timeout) in controller layer.
- [ ] Thread pool tuning report documents tested configurations with justification.

---

*Guide prepared from codebase analysis of the BloggingPlatform-Spring project.*
