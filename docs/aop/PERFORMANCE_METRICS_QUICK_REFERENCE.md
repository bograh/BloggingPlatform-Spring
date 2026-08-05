# Performance Metrics - Quick Reference

## Quick Start

### 1. View All Method Metrics

```bash
curl http://localhost:8080/api/metrics/performance
```

### 2. View Metrics Summary

```bash
curl http://localhost:8080/api/metrics/performance/summary
```

### 3. View Specific Method (by layer/name)

```bash
curl http://localhost:8080/api/metrics/performance/SERVICE/createPost
```

### 4. View Runtime API Metrics

```bash
curl "http://localhost:8080/api/metrics/performance/runtime?limit=10"
```

### 5. Export Runtime Metrics CSV

```bash
curl -X POST "http://localhost:8080/api/metrics/performance/runtime/export?limit=25"
```

### 6. Compare PRE_CACHE vs POST_CACHE (DB)

```bash
curl http://localhost:8080/api/metrics/performance/comparison/database
```

### 7. Export Performance + Cache Metrics

```bash
curl -X POST http://localhost:8080/api/metrics/performance/export-all
```

### 8. Reset Runtime Metrics

```bash
curl -X DELETE http://localhost:8080/api/metrics/performance/runtime/reset
```

### 9. Reset All Metrics

```bash
curl -X DELETE http://localhost:8080/api/metrics/performance/reset
```

### 10. Export to Logs

```bash
curl -X POST http://localhost:8080/api/metrics/performance/export-log
```

---

## Example Response

```json
{
  "totalMethods": 2,
  "timestamp": "2026-02-23T19:00:00",
  "metrics": [
    {
      "methodName": "PostService.getAllPosts(..)",
      "totalCalls": 245,
      "successfulCalls": 243,
      "failedCalls": 2,
      "averageExecutionTime": 125,
      "minExecutionTime": 45,
      "maxExecutionTime": 890,
      "successRate": 99.18
    }
  ]
}
```

---

## Metric Definitions

| Metric           | Description                             |
|------------------|-----------------------------------------|
| totalCalls       | Total number of method invocations      |
| successfulCalls  | Number of successful executions         |
| failedCalls      | Number of failed executions             |
| successRate      | Percentage of successful calls          |
| avgExecutionTime | Average execution time in ms            |
| minExecutionTime | Fastest execution time in ms            |
| maxExecutionTime | Slowest execution time in ms            |


---

## Actuator Endpoints

### Health

```bash
curl http://localhost:8080/actuator/health
```

### All Metrics

```bash
curl http://localhost:8080/actuator/metrics
```

### Prometheus Format

```bash
curl http://localhost:8080/actuator/prometheus
```

---

## Log Format

```log
[PERFORMANCE] Method: PostService.getAllPosts(..) | Execution Time: 125 ms | Status: SUCCESS
[RUNTIME_METRIC] method=GET path=/api/posts status=200 latencyMs=35
```

---

## Common Workflows

### Performance Check After Deployment

```bash
# 1. Check summary
curl http://localhost:8080/api/metrics/performance/summary

# 2. Check runtime snapshot
curl "http://localhost:8080/api/metrics/performance/runtime?limit=10"

# 3. Export runtime and all metrics
curl -X POST "http://localhost:8080/api/metrics/performance/runtime/export?limit=25"
curl -X POST http://localhost:8080/api/metrics/performance/export-all
```

### Investigate Specific Service

```bash
# 1. Find specific method by layer/name
curl http://localhost:8080/api/metrics/performance/SERVICE/createPost

# 2. Find specific method by full method name
curl "http://localhost:8080/api/metrics/performance/method/PostService.getAllPosts(..)"
```

### Reset After Code Changes

```bash
# Reset metrics to get fresh data
curl -X DELETE http://localhost:8080/api/metrics/performance/reset
```

---

## Troubleshooting

**No metrics showing?**

- Execute some operations first
- Methods are only tracked after they're called

**Want to clear old data?**

- Use the reset endpoint

**Need detailed logs or CSV exports?**

- Use the export endpoint to print to logs
- Use runtime export for CSV tables under `metrics/runtime/`
- Check `logs/blogging-platform.log`

---

For detailed documentation, see [PERFORMANCE_METRICS_GUIDE.md](PERFORMANCE_METRICS_GUIDE.md)