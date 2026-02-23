# Concurrent API Calls Test Report

Date: 2026-02-23

## Test Scope

- Create and provide Postman collection for concurrent tests
- Simulate multiple simultaneous requests against:
  - `/api/posts`
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
