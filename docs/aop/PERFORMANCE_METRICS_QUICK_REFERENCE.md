# Performance Metrics - Quick Reference

## Quick Start

### 1. View All Metrics

```bash
curl http://localhost:8080/api/metrics/performance
```

### 2. View Specific Method

```bash
curl http://localhost:8080/api/metrics/performance/SERVICE/PostServiceImpl.createPost(..)
```

### 3. View Summary

```bash
curl http://localhost:8080/api/metrics/performance/summary
```

### 4. Find Slow Methods (>1 second)

```bash
curl http://localhost:8080/api/metrics/performance/slow
```

### 5. Top 10 Slowest Methods

```bash
curl http://localhost:8080/api/metrics/performance/top?limit=10
```

### 6. Service Layer Only

```bash
curl http://localhost:8080/api/metrics/performance/layer/SERVICE
```

### 7. Repository Layer Only

```bash
curl http://localhost:8080/api/metrics/performance/layer/REPOSITORY
```

### 8. Failure Statistics

```bash
curl http://localhost:8080/api/metrics/performance/failures
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
  "totalMethods": 15,
  "timestamp": "2026-01-20T10:30:45.123Z",
  "methods": [
    {
      "method": "SERVICE::PostServiceImpl.createPost(..)",
      "totalCalls": 245,
      "successfulCalls": 243,
      "failedCalls": 2,
      "failureRate": "0.82%",
      "avgExecutionTime": 125,
      "minExecutionTime": 45,
      "maxExecutionTime": 890,
      "p50": 110,
      "p95": 450,
      "p99": 750,
      "stdDev": "95.23",
      "unit": "ms",
      "performanceLevel": "NORMAL"
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
| failureRate      | Percentage of failed calls              |
| avgExecutionTime | Average execution time in ms            |
| minExecutionTime | Fastest execution time in ms            |
| maxExecutionTime | Slowest execution time in ms            |
| p50              | Median execution time (50th percentile) |
| p95              | 95th percentile execution time          |
| p99              | 99th percentile execution time          |
| stdDev           | Standard deviation of execution times   |

---

## Performance Levels

- **FAST**: < 100ms
- **NORMAL**: 100ms - 500ms
- **SLOW**: 500ms - 1000ms
- **CRITICAL**: > 1000ms

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
[PERFORMANCE] 2026-01-20 10:30:45 | NORMAL | Method: SERVICE::PostServiceImpl.createPost(..) | Execution Time: 125 ms | Memory: 1024 KB | Status: SUCCESS

[PERFORMANCE] SLOW SERVICE OPERATION DETECTED: PostServiceImpl.getAllPosts(..) took 1250 ms
```

---

## Common Workflows

### Performance Check After Deployment

```bash
# 1. Check summary
curl http://localhost:8080/api/metrics/performance/summary

# 2. Check for slow methods
curl http://localhost:8080/api/metrics/performance/slow

# 3. Check failures
curl http://localhost:8080/api/metrics/performance/failures
```

### Investigate Specific Service

```bash
# 1. Get all service layer metrics
curl http://localhost:8080/api/metrics/performance/layer/SERVICE

# 2. Find specific method
curl http://localhost:8080/api/metrics/performance/SERVICE/PostServiceImpl.createPost(..)
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

**Need detailed logs?**

- Use the export endpoint to print to logs
- Check `logs/blogging-platform.log`

---

For detailed documentation, see [PERFORMANCE_METRICS_GUIDE.md](PERFORMANCE_METRICS_GUIDE.md)