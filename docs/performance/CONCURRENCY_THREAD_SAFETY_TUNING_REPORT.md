# Concurrency, Thread-Safety, and Thread-Pool Tuning Report

Date: 2026-02-23
Scope: Thread safety hardening, concurrent stress validation, and async executor tuning.

## 1) Shared Mutable Resources Identified

### Token session state
- Location: `TokenSessionService`
- Resource: `activeSessions` (`ConcurrentHashMap<String, SessionInfo>`)
- Risk: Session objects were mutable and updated in place, which can produce race windows during concurrent activity updates.

### Failed-attempt tracking state
- Location: `SecurityAuditService`
- Resource: `lastFailedAttemptTime` (`ConcurrentHashMap<String, LocalDateTime>`)
- Risk: Rapid-attempt detection used a read-then-write (`get` then `put`) sequence that was not atomic under contention.

## 2) Thread-Safety Hardening Applied

### Immutable session objects + atomic map update
- `SessionInfo` changed from mutable DTO to immutable value object (`@Value`, `@Builder(toBuilder = true)`).
- `TokenSessionService.updateSessionActivity(...)` now uses `computeIfPresent(...)` to atomically replace the session snapshot.
- Result: no in-place mutation of shared session objects.

### Atomic rapid-attempt detection
- `SecurityAuditService.trackFailedAttempt(...)` now updates `lastFailedAttemptTime` via `compute(...)`.
- Rapid-attempt flag is computed in the same atomic operation using `AtomicBoolean`.
- Result: no race window between reading previous attempt time and writing current attempt time.

### Concurrent collection usage
- Shared map state remains on `ConcurrentHashMap`.
- Concurrency stress tests use `CopyOnWriteArrayList` for cross-thread error collection without external locks.

## 3) Concurrent Stress Tests Executed

## Test classes
- `TokenSessionServiceConcurrencyTest`
- `SecurityAuditServiceConcurrencyTest`
- `AsyncExecutorTuningStressTest`

## Outcomes
- Test run: 7 passed, 0 failed.
- No race-condition exceptions observed in concurrent session updates, revoke/cleanup operations, or failed sign-in tracking.
- IP blocking logic remained consistent under high concurrent failed-attempt load.

## 4) Thread-Pool Tuning Experiments

Source artifact:
- `metrics/profiling/thread-pool-tuning-20260223-165817.txt`

Workload characteristics:
- 900 mixed tasks (CPU loop + short blocking wait) per profile.
- Rejection handling: `CallerRunsPolicy`.
- Metrics captured: elapsed time, process CPU utilization, peak heap usage, failures.

Measured results:

| Profile | Elapsed (ms) | CPU (%) | Peak Memory (MB) | Failures |
|---|---:|---:|---:|---:|
| core=4, max=12, queue=400 | 272 | 85.92 | 59 | 0 |
| core=8, max=24, queue=400 | 74 | 57.10 | 63 | 0 |
| core=12, max=32, queue=400 | 84 | 50.09 | 27 | 0 |

## 5) Optimal Configuration and Justification

Selected baseline:
- `app.async.core-pool-size=8`
- `app.async.max-pool-size=24`
- `app.async.queue-capacity=400`
- `app.async.keep-alive-seconds=60`
- `app.async.await-termination-seconds=30`

Why this profile:
1. Lowest elapsed execution time (best throughput/latency balance).
2. CPU utilization remained below saturation (57.10%), leaving headroom for web/request threads.
3. Memory remained stable and bounded under load.
4. No task failures or rejection errors observed.

## 6) Additional Executor Safety Settings Applied

- `CallerRunsPolicy` added to prevent silent task drops under burst load.
- Core-thread timeout enabled (`allowCoreThreadTimeOut=true`) to reduce idle-thread footprint.
- Shutdown behavior remains graceful (`waitForTasksToCompleteOnShutdown=true`).

## 7) Validation Conclusion

- Shared mutable resource race windows identified and removed in high-risk paths.
- Concurrent stress tests passed without race-condition symptoms.
- Async thread pool baseline tuned and documented from measured load data.
- Current baseline is ready for deployment-level validation with production-like traffic.
