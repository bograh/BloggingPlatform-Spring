# Secured Backend Performance Bottleneck Report

Date: 2026-02-23

## Scope

Target traffic and analysis focus:

- Posts endpoints (`/api/posts`)
- Comments endpoints (`/api/comments/**`)
- Analytics endpoints (`/api/metrics/performance/**`)
- Secured request path through JWT filter chain and role checks

## Profiling Tooling

- VisualVM (CPU sampler, memory sampler, threads tab)
- JProfiler (CPU call tree, allocation hotspots, monitor contention)
- Application-exported metrics (`/api/metrics/performance/export-log`)
- JVM snapshots from `jcmd` (heap/thread dump)

Reusable script for secured profiling sessions:

- `dev/profile-secured-backend.sh`

## Baseline Metrics (from existing runtime artifacts)

### A. Service-level execution baseline

Source files:

- `metrics/pre-cache/20260127-173530-performance-summary.log`
- `metrics/20260202-163808-performance-summary.log`

Representative method timings:

| Method | Calls | Avg | Max | Notes |
|---|---:|---:|---:|---|
| `PostService.getAllPosts(..)` | 173 (pre-cache), 6 (post-cache) | 106 ms (pre-cache), 163 ms (cold post-cache run) | 2034 ms (pre-cache), 234 ms (post-cache sample) | High-traffic endpoint, cache hit ratio dominates user-perceived latency |
| `CommentService.getAllCommentsByPostId(..)` | 103 (pre-cache), 11 (post-cache) | 17 ms (pre-cache), 25 ms (post-cache sample) | 182 ms (pre-cache), 55 ms (post-cache sample) | Fast on cache hit, sensitive to Mongo round-trips |
| `PerformanceMetricsService.getMetricsSummary()` | 17 / 28 | 3 ms / 2 ms | 41 ms / 18 ms | Analytics endpoint itself is lightweight |

### B. Live log-derived high latency events

Source file:

- `logs/blogging-platform.log`

Observed:

- `PostService.getAllPosts(..)` max `31222 ms` with `MongoTimeoutException`/`DataAccessResourceFailureException`
- Additional very slow `PostService.getAllPosts(..)` event at `20321 ms`
- During same period, sign-in path also showed latency spikes (`AuthService.signInUser(..)` at `1814 ms`)

Interpretation: extreme tail latency is strongly correlated with database connectivity/availability and request blocking while waiting for Mongo driver server selection timeout.

### C. Thread usage baseline (log-derived)

Observed servlet workers in active use:

- `http-nio-8080-exec-1`
- `http-nio-8080-exec-2`
- `http-nio-8080-exec-3`
- `http-nio-8080-exec-5`
- `http-nio-8080-exec-6`
- `http-nio-8080-exec-7`
- `http-nio-8080-exec-8`
- `http-nio-8080-exec-10`

Total observed worker threads: `8`

## Identified Bottlenecks

## 1) N+1-style comment count query in posts listing

Where:

- `src/main/java/org/amalitech/bloggingplatformspring/utils/PostUtils.java`

Issue:

- `mapPostPageToPostResponsePage(...)` executes `commentRepository.countByPostId(post.getId())` for each post in page.
- This adds one DB call per post, increasing latency and DB load under high traffic.

Risk profile:

- High impact on `/api/posts` when cache miss occurs.
- Scales poorly with page size and concurrent requests.

## 2) Synchronized metric recording on every monitored service call

Where:

- `src/main/java/org/amalitech/bloggingplatformspring/aop/PerformanceMonitoringAspect.java`

Issue:

- `MethodMetrics.recordExecution(...)` is `synchronized` and invoked for each service call.
- Under high concurrency this can create lock contention in instrumentation itself.

Risk profile:

- Medium impact in normal traffic, potentially high under burst traffic.
- Can inflate tail latency while collecting metrics.

## 3) Blocking database waits causing extreme tail latency

Where evidence appears:

- `logs/blogging-platform.log`

Issue:

- Requests block while waiting for Mongo primary selection, causing 20-31 second latency spikes.

Risk profile:

- Critical impact during Mongo degradation/outage.
- Consumes servlet worker threads and reduces throughput for all endpoints.

## 4) Extra database call in comment deletion flow

Where:

- `src/main/java/org/amalitech/bloggingplatformspring/services/CommentService.java`

Issue:

- `deleteComment(...)` loads `Post` entity and does not use it for authorization/response.
- Adds avoidable DB latency for write path.

Risk profile:

- Low-to-medium impact per request, becomes relevant at scale.

## 5) High-volume INFO logging on hot path

Where:

- `src/main/java/org/amalitech/bloggingplatformspring/aop/PerformanceMonitoringAspect.java`

Issue:

- Every monitored service call writes `[PERFORMANCE]` log at `INFO` level.
- Logging I/O can amplify latency and CPU usage under high QPS.

Risk profile:

- Medium impact in high-traffic scenarios.

## Baseline Data Coverage Status

- API response latency: Available from artifacts and runtime logs.
- Thread usage: Available (servlet worker activity from logs; full thread-state capture enabled in script).
- Heap footprint: Not available in existing artifacts; captured by `jcmd GC.heap_info` in profiling script.
- CPU usage: Not available in existing artifacts; captured by `pidstat`/`top` in profiling script.

## Secured Profiling Session Procedure

1. Start backend with valid PostgreSQL and MongoDB connectivity.
2. Run traffic + metrics capture:

```bash
chmod +x dev/profile-secured-backend.sh
EMAIL=admin@example.com PASSWORD='your-password' REQUESTS=200 CONCURRENCY=40 POST_ID=1 ./dev/profile-secured-backend.sh
```

3. Open VisualVM or JProfiler attached to app JVM during the run.
4. Review artifacts in `metrics/profiling/<timestamp>/`:

- `latency-summary.txt`
- `heap-info.txt`
- `thread-dump.txt`
- `thread-states.txt`
- `pidstat-cpu-memory.txt` or `top-snapshot.txt`
- `api-performance-summary.json`

## Priority Recommendations

1. Eliminate per-post `countByPostId` loop with batched aggregate query for page IDs.
2. Replace `synchronized` metric recorder with lock-free min/max update strategy.
3. Set tighter Mongo client timeouts and add graceful fallback for comments-dependent paths.
4. Remove unnecessary post lookup in `CommentService.deleteComment(...)`.
5. Reduce hot-path performance logging verbosity in production.
