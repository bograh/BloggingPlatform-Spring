# Concurrent API Calls Test Report

Date: 2026-02-23

## Test Scope

- Create and provide Postman collection for concurrent tests
- Simulate multiple simultaneous requests against:
  - `/api/posts`
  - `/api/posts/popular?limit=10`
  - `/api/posts/trending?limit=10`
  - `/api/comments/post/{postId}`
  - `/api/metrics/performance/summary`
- Provide JMeter test plan
- Verify no data corruption and no data loss
- Compare average response time with baseline
- Document performance improvement

## Test Assets Delivered

- Postman collection: `dev/performance-tests/postman/Concurrent-API-Tests.postman_collection.json`
- JMeter plan: `dev/performance-tests/jmeter/concurrent-api-load-test.jmx`
- Usage guide: `dev/performance-tests/README.md`

Coverage update:

- Postman burst runner now includes `popular` and `trending` post endpoints in the concurrent endpoint set.
- Shell profiler `dev/profile-secured-backend.sh` now also includes `popular` and `trending` endpoint latency capture.
- Existing measured table in this report reflects the prior run (`posts/comments/analytics`) and should be re-run to capture new endpoint-specific latency numbers.

### Rerun Command (Updated Coverage)

```bash
cd /home/bograh/Code/BloggingPlatform-Spring
set -a && source .env && set +a
REQUESTS=150 CONCURRENCY=40 POST_ID=1 ./dev/profile-secured-backend.sh
```

Expected additional output files in the new profiling run directory:

- `popular-times.txt`
- `trending-times.txt`
- Updated `latency-summary.txt` containing `popular` and `trending` rows

## Execution Summary (Actual Run)

Execution method:

- Concurrent simulation executed using `dev/profile-secured-backend.sh`
- Admin credentials used for secured analytics endpoint

Run configuration:

- Requests per endpoint: `150`
- Concurrency: `40`
- Output directory: `metrics/profiling/20260223-163105`

## Simultaneous Requests Results

| Endpoint | Successful Requests | Avg Response | P95 | P99 | Max |
|---|---:|---:|---:|---:|---:|
| Posts (`/api/posts`) | 150 | 0.468 s | 1.168 s | 1.408 s | 1.713 s |
| Comments (`/api/comments/post/{postId}`) | 150 | 0.982 s | 3.383 s | 3.484 s | 3.714 s |
| Analytics (`/api/metrics/performance/summary`) | 150 | 1.198 s | 3.186 s | 3.260 s | 3.455 s |

## Simultaneous Requests Results (Updated Coverage Run)

Run metadata:

- Run directory: `metrics/profiling/20260223-180931-opt`
- Requests per endpoint: `120`
- Concurrency: `30`
- Auth token source: temporary non-admin test user created via `/api/auth/register` and `/api/auth/sign-in`

| Endpoint | Successful Requests | Avg Response | P95 | P99 | Max | Notes |
|---|---:|---:|---:|---:|---:|---|
| Posts (`/api/posts`) | 120 | 0.164 s | 0.903 s | 0.915 s | 0.930 s | Successful |
| Popular (`/api/posts/popular?limit=10`) | 120 | 0.296 s | 1.088 s | 1.140 s | 1.211 s | Successful |
| Trending (`/api/posts/trending?limit=10`) | 120 | 0.372 s | 1.236 s | 1.278 s | 1.311 s | Successful |
| Comments (`/api/comments/post/{postId}`) | 0 | - | - | - | - | 120x `404` (post id not found in this run context) |
| Analytics (`/api/metrics/performance/summary`) | 0 | - | - | - | - | 120x `403` (requires admin privileges) |

Sources:

- `metrics/profiling/20260223-180931-opt/posts-times.txt`
- `metrics/profiling/20260223-180931-opt/popular-times.txt`
- `metrics/profiling/20260223-180931-opt/trending-times.txt`
- `metrics/profiling/20260223-180931-opt/comments-times.txt.errors`
- `metrics/profiling/20260223-180931-opt/analytics-times.txt.errors`

## Simultaneous Requests Results (Admin Token + Valid Post ID)

Run metadata:

- Run directory: `metrics/profiling/20260223-181435-opt-admin`
- Requests per endpoint: `120`
- Concurrency: `30`
- Token scope: admin-capable token (`roles` included `ADMIN`)
- Resolved post id for comments endpoint: `3`

| Endpoint | Successful Requests | Avg Response | P95 | P99 | Max | Notes |
|---|---:|---:|---:|---:|---:|---|
| Posts (`/api/posts`) | 120 | 0.260 s | 0.850 s | 0.875 s | 0.884 s | Successful |
| Popular (`/api/posts/popular?limit=10`) | 120 | 0.231 s | 0.459 s | 0.468 s | 0.472 s | Successful |
| Trending (`/api/posts/trending?limit=10`) | 120 | 0.211 s | 0.439 s | 0.447 s | 0.467 s | Successful |
| Comments (`/api/comments/post/{postId}`) | 120 | 0.149 s | 0.734 s | 0.737 s | 0.740 s | Successful |
| Analytics (`/api/metrics/performance/summary`) | 120 | 0.326 s | 0.878 s | 0.890 s | 1.235 s | Successful |

Sources:

- `metrics/profiling/20260223-181435-opt-admin/posts-times.txt`
- `metrics/profiling/20260223-181435-opt-admin/popular-times.txt`
- `metrics/profiling/20260223-181435-opt-admin/trending-times.txt`
- `metrics/profiling/20260223-181435-opt-admin/comments-times.txt`
- `metrics/profiling/20260223-181435-opt-admin/analytics-times.txt`

## Simultaneous Requests Results (Latest Wrapper Rerun)

Run metadata:

- Run directory: `metrics/profiling/20260223-183509-opt-admin`
- Execution command: `bash dev/performance-tests/run-admin-profile.sh`
- Requests per endpoint: `120`
- Concurrency: `30`
- Resolved post id for comments endpoint: `3`

| Endpoint | Successful Requests | Avg Response | P95 | P99 | Max | Notes |
|---|---:|---:|---:|---:|---:|---|
| Posts (`/api/posts`) | 120 | 0.227 s | 0.697 s | 0.707 s | 0.718 s | Successful |
| Popular (`/api/posts/popular?limit=10`) | 120 | 0.347 s | 0.752 s | 0.809 s | 0.829 s | Successful |
| Trending (`/api/posts/trending?limit=10`) | 120 | 0.220 s | 0.493 s | 0.506 s | 0.520 s | Successful |
| Comments (`/api/comments/post/{postId}`) | 120 | 0.144 s | 0.452 s | 0.460 s | 0.462 s | Successful |
| Analytics (`/api/metrics/performance/summary`) | 120 | 0.335 s | 0.618 s | 0.734 s | 0.740 s | Successful |

Sources:

- `metrics/profiling/20260223-183509-opt-admin/posts-times.txt`
- `metrics/profiling/20260223-183509-opt-admin/popular-times.txt`
- `metrics/profiling/20260223-183509-opt-admin/trending-times.txt`
- `metrics/profiling/20260223-183509-opt-admin/comments-times.txt`
- `metrics/profiling/20260223-183509-opt-admin/analytics-times.txt`

Sources:

- `metrics/profiling/20260223-163105/posts-times.txt`
- `metrics/profiling/20260223-163105/comments-times.txt`
- `metrics/profiling/20260223-163105/analytics-times.txt`

## Data Integrity Verification

Verification checks:

1. No request failures recorded in endpoint timing outputs
2. Metrics summary reports zero failed monitored calls
3. Overall success rate remained 100%

Observed:

- `totalFailures = 0`
- `overallSuccessRate = 100.0%`
- No `*.errors` files produced for this run

Interpretation:

- No data loss detected for the tested request set
- No response corruption detected from API-level success/failure perspective
- Since tested endpoints are read-heavy in this run, no write-path corruption signal was observed

## Baseline Comparison (Avg Response Time)

Baseline reference run:

- `metrics/profiling/20260223-162132`
- Requests/concurrency: `120 / 30`

| Endpoint | Baseline Avg | Current Avg | Change |
|---|---:|---:|---:|
| Posts | 1.114 s | 0.468 s | 57.99% faster |
| Comments | 0.260 s | 0.982 s | 277.69% slower |
| Analytics | 1.202 s | 1.198 s | 0.33% faster |

## Performance Improvement Notes

- Posts endpoint improved significantly under higher load in this run.
- Analytics remained stable and effectively unchanged.
- Comments degraded under the heavier `150/40` load profile and is the current bottleneck candidate.

## JMeter Execution Status

- JMeter CLI was not available in this environment (`jmeter` command missing).
- Provided ready-to-run JMeter plan for local/CI execution:

  - `dev/performance-tests/jmeter/concurrent-api-load-test.jmx`

When JMeter is installed, run:

```bash
jmeter -n -t dev/performance-tests/jmeter/concurrent-api-load-test.jmx -l metrics/jmeter-results.jtl -e -o metrics/jmeter-report
```
