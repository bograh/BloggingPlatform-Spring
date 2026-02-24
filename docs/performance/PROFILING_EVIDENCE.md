# Profiling Evidence

Date: 2026-02-23

This file indexes profiler and load-test evidence used in the optimization reports.

## Profiler Screenshots

Add screenshot files under `docs/performance/evidence/` and update links below.

Suggested captures:

1. CPU profile snapshot (`top`/`pidstat`) during load
2. Heap profile snapshot (`jcmd GC.heap_info`) during load
3. Thread state snapshot (`jcmd Thread.print -l`) during load
4. Swagger runtime metrics endpoint response

Template links:

- CPU snapshot: `docs/performance/evidence/cpu-profile.png`
- Heap snapshot: `docs/performance/evidence/heap-profile.png`
- Thread snapshot: `docs/performance/evidence/thread-states.png`
- Runtime endpoint snapshot: `docs/performance/evidence/runtime-endpoint.png`

## Postman Results

Assets:

- Collection: `dev/performance-tests/postman/Concurrent-API-Tests.postman_collection.json`
- Report details: `docs/performance/CONCURRENT_API_CALLS_TEST_REPORT.md`

Recommended screenshot captures:

- Postman collection run summary dashboard
- Request-level latency distribution for `posts`, `popular`, `trending`, `comments`, `analytics`

Template links:

- Postman run summary: `docs/performance/evidence/postman-run-summary.png`
- Postman endpoint metrics: `docs/performance/evidence/postman-endpoint-latency.png`

## JMeter Results

Assets:

- Test plan: `dev/performance-tests/jmeter/concurrent-api-load-test.jmx`
- CLI run command:

```bash
jmeter -n -t dev/performance-tests/jmeter/concurrent-api-load-test.jmx -l metrics/jmeter-results.jtl -e -o metrics/jmeter-report
```

Recommended screenshot captures:

- JMeter HTML dashboard summary
- Throughput over time chart
- Response time percentile chart

Template links:

- JMeter dashboard summary: `docs/performance/evidence/jmeter-summary.png`
- JMeter throughput chart: `docs/performance/evidence/jmeter-throughput.png`
- JMeter percentile chart: `docs/performance/evidence/jmeter-percentiles.png`

## Runtime Metric Tables/Charts

Primary exported table:

- `metrics/runtime/<timestamp>-runtime-metrics.csv`

Use this CSV in spreadsheet tooling to produce:

- Avg latency by endpoint chart
- Throughput (`req/sec`) chart
- Error-rate chart
- Memory usage trend chart
