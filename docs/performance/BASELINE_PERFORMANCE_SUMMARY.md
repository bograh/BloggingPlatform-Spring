# Baseline Performance Summary

Date: 2026-02-23
Scope: Secured backend traffic on Posts, Comments, and Analytics endpoints.

## Bottleneck Highlights

1. **Posts endpoint tail latency spikes**
   - Evidence: `PostService.getAllPosts(..)` reached `31222 ms` and `20321 ms` in runtime logs.
   - Impact: Severe response delay and reduced throughput under backend dependency failures.

2. **N+1-style query pattern in posts page mapping**
   - Location: `PostUtils.mapPostPageToPostResponsePage(...)`
   - Behavior: Calls `countByPostId(...)` once per post in page, increasing DB calls with page size.

3. **Instrumentation lock contention risk**
   - Location: `PerformanceMonitoringAspect.MethodMetrics.recordExecution(...)`
   - Behavior: `synchronized` metrics updates for each monitored service invocation.

4. **Blocking waits during Mongo availability issues**
   - Evidence: `MongoTimeoutException` and `DataAccessResourceFailureException` correlated with slow requests.
   - Impact: Servlet worker threads remain occupied while waiting for DB timeout.

5. **Hot-path logging overhead**
   - Location: Performance aspect logs every service call at INFO level.
   - Impact: Extra log I/O and CPU usage under high request volume.

## Baseline Metrics

## API/Service Latency Baseline (from artifacts)

| Area | Method / Endpoint | Calls | Avg | Max | Source |
|---|---|---:|---:|---:|---|
| Posts | `PostService.getAllPosts(..)` | 173 (pre-cache) | 106 ms | 2034 ms | `metrics/pre-cache/20260127-173530-performance-summary.log` |
| Posts | `PostService.getAllPosts(..)` | 6 (post-cache sample) | 163 ms | 234 ms | `metrics/20260202-163808-performance-summary.log` |
| Comments | `CommentService.getAllCommentsByPostId(..)` | 103 (pre-cache) | 17 ms | 182 ms | `metrics/pre-cache/20260127-173530-performance-summary.log` |
| Comments | `CommentService.getAllCommentsByPostId(..)` | 11 (post-cache sample) | 25 ms | 55 ms | `metrics/20260202-163808-performance-summary.log` |
| Analytics | `PerformanceMetricsService.getMetricsSummary()` | 17 / 28 | 3 ms / 2 ms | 41 ms / 18 ms | pre/post cache snapshots |

## Runtime Tail Latency Baseline (live log sample)

| Method | Observed Avg* | Max | Calls Observed | Source |
|---|---:|---:|---:|---|
| `PostService.getAllPosts(..)` | 13104.75 ms | 31222 ms | 4 | `logs/blogging-platform.log` |
| `AuthService.signInUser(..)` | 1814 ms | 1814 ms | 1 | `logs/blogging-platform.log` |

\*Average here is from the limited observed log window, not a controlled load-test run.

## Thread Usage Baseline

- Observed active servlet worker threads: **8** (`http-nio-8080-exec-{1,2,3,5,6,7,8,10}`)
- Baseline indicates concurrent request handling is active, with throughput sensitive to blocking DB waits.

## CPU and Memory Baseline Status

- **CPU usage baseline:** Pending controlled run on secured backend.
- **Heap memory footprint baseline:** Pending controlled run on secured backend.

Use this command to generate those metrics with existing script:

```bash
EMAIL=admin@example.com PASSWORD='your-password' REQUESTS=200 CONCURRENCY=40 POST_ID=1 ./dev/profile-secured-backend.sh
```

Output folder:

- `metrics/profiling/<timestamp>/pidstat-cpu-memory.txt` or `top-snapshot.txt`
- `metrics/profiling/<timestamp>/heap-info.txt`
- `metrics/profiling/<timestamp>/thread-states.txt`
- `metrics/profiling/<timestamp>/latency-summary.txt`

## Priority Fix Order

1. Remove N+1 per-post comment count pattern in posts list path.
2. Replace synchronized metric update path with lock-free/minimal-lock implementation.
3. Reduce Mongo timeout blast radius with stricter timeout and fallback behavior.
4. Lower hot-path performance logging verbosity in production.

## Fresh Benchmark Update (Admin Authenticated)

Run timestamp: `2026-02-23 16:21:32`

Run configuration:

- Requests per endpoint: `120`
- Concurrency: `30`
- Profile output: `metrics/profiling/20260223-162132/`

### Endpoint Latency (Admin run)

| Endpoint group | Successful requests | Avg | P95 | P99 | Max |
|---|---:|---:|---:|---:|---:|
| Posts (`/api/posts`) | 120 | 1.114 s | 3.078 s | 3.109 s | 3.131 s |
| Comments (`/api/comments/post/{postId}`) | 120 | 0.260 s | 0.479 s | 0.560 s | 0.668 s |
| Analytics (`/api/metrics/performance/summary`) | 120 | 1.202 s | 4.232 s | 4.377 s | 4.511 s |

### Runtime Snapshot (Admin run)

- Thread states: RUNNABLE `11`, TIMED_WAITING `41`, WAITING `10`
- Heap: total `147456K`, used `117244K`
- Metaspace used: `108642K`
- API performance summary success rate: `100.0%`

### Delta vs Previous Non-Admin Run (`20260223-161401`)

| Endpoint group | Previous Avg | New Avg | Improvement |
|---|---:|---:|---:|
| Posts | 2.605 s | 1.114 s | 57.24% faster |
| Comments | 1.788 s | 0.260 s | 85.46% faster |
| Analytics | 403-forbidden | 1.202 s avg | measurable with admin role |

### Source Artifacts

- `metrics/profiling/20260223-162132/latency-summary.txt`
- `metrics/profiling/20260223-162132/top-snapshot.txt`
- `metrics/profiling/20260223-162132/heap-info.txt`
- `metrics/profiling/20260223-162132/thread-states.txt`
- `metrics/profiling/20260223-162132/api-performance-summary.json`
