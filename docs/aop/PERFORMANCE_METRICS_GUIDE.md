# Performance Metrics Viewing Guide

## Overview
The Blogging Platform integrates comprehensive performance monitoring through AOP (Aspect-Oriented Programming) aspects that automatically track execution times, call counts, percentiles, and other metrics for all service and repository layer methods.

## Table of Contents
1. [Architecture](#architecture)
2. [Viewing Metrics](#viewing-metrics)
3. [API Endpoints](#api-endpoints)
4. [Metrics Details](#metrics-details)
5. [Spring Boot Actuator Integration](#spring-boot-actuator-integration)
6. [Performance Analysis](#performance-analysis)
7. [Best Practices](#best-practices)

---

## Architecture

### Components

**PerformanceMonitoringAspect**: Core AOP aspect that intercepts method calls and collects metrics
- Monitors service layer methods (`org.amalitech.bloggingplatformspring.services..*`)
- Monitors repository layer methods (`org.amalitech.bloggingplatformspring.repository..*`)
- Tracks execution times, memory usage, success/failure rates
- Calculates percentiles (P50, P95, P99)

**PerformanceMetricsService**: Service layer for aggregating and formatting metrics data
- Provides formatted views of metrics
- Filters and sorts metrics by various criteria
- Calculates summary statistics

**PerformanceMetricsController**: REST API for accessing metrics
- Exposes endpoints for querying performance data
- Provides various views: all metrics, by layer, slow methods, etc.

---

## Viewing Metrics

### 1. REST API Endpoints

All performance metrics are accessible via REST API at `/api/metrics/performance`

#### Get All Metrics
```bash
GET http://localhost:8080/api/metrics/performance
```

**Response:**
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

#### Get Metrics for Specific Method
```bash
GET http://localhost:8080/api/metrics/performance/{layer}/{methodName}

# Example:
GET http://localhost:8080/api/metrics/performance/SERVICE/PostServiceImpl.createPost(..)
```

#### Get Summary Statistics
```bash
GET http://localhost:8080/api/metrics/performance/summary
```

**Response:**
```json
{
  "totalMethodsMonitored": 15,
  "totalExecutions": 5432,
  "totalFailures": 12,
  "overallAverageExecutionTime": "156.7 ms"
}
```

#### Get Slow Methods
```bash
GET http://localhost:8080/api/metrics/performance/slow?thresholdMs=500

# Default threshold is 1000ms if not specified
```

#### Get Top N Slowest Methods
```bash
GET http://localhost:8080/api/metrics/performance/top?limit=10

# Default limit is 10 if not specified
```

#### Get Metrics by Layer
```bash
GET http://localhost:8080/api/metrics/performance/layer/SERVICE
GET http://localhost:8080/api/metrics/performance/layer/REPOSITORY
```

#### Get Failure Statistics
```bash
GET http://localhost:8080/api/metrics/performance/failures
```

**Response:**
```json
{
  "totalFailures": 12,
  "methodsWithFailures": 3,
  "methods": [
    {
      "method": "SERVICE::UserServiceImpl.deleteUser(..)",
      "totalCalls": 100,
      "failedCalls": 8,
      "successfulCalls": 92,
      "failureRate": "8.00%"
    }
  ]
}
```

#### Reset Metrics
```bash
DELETE http://localhost:8080/api/metrics/performance/reset
```

#### Export Metrics to Log
```bash
POST http://localhost:8080/api/metrics/performance/export/log
```

---

### 2. Application Logs

Performance metrics are automatically logged with each method execution:

```log
[PERFORMANCE] 2026-01-20 10:30:45 | NORMAL | Method: SERVICE::PostServiceImpl.createPost(..) | Execution Time: 125 ms | Memory: 1024 KB | Status: SUCCESS

[PERFORMANCE] SLOW SERVICE OPERATION DETECTED: PostServiceImpl.getAllPosts(..) took 1250 ms
```

**Log Files Location:**
- `logs/blogging-platform.log` - Main application log with performance data

### 3. Viewing in Console

To print a comprehensive performance summary to the console/log:

```bash
POST http://localhost:8080/api/metrics/performance/export/log
```

This will log a detailed summary:

```log
================================================================================
PERFORMANCE METRICS SUMMARY
================================================================================
Method: SERVICE::PostServiceImpl.createPost(..)
  Total Calls: 245
  Successful: 243
  Failed: 2
  Avg Execution Time: 125 ms
  Min Execution Time: 45 ms
  Max Execution Time: 890 ms
  P50 (Median): 110 ms
  P95: 450 ms
  P99: 750 ms
--------------------------------------------------------------------------------
Method: SERVICE::CommentServiceImpl.addComment(..)
  Total Calls: 532
  Successful: 532
  Failed: 0
  Avg Execution Time: 67 ms
  Min Execution Time: 23 ms
  Max Execution Time: 234 ms
  P50 (Median): 60 ms
  P95: 145 ms
  P99: 200 ms
--------------------------------------------------------------------------------
```

---

### 4. Spring Boot Actuator Endpoints

The application exposes standard Spring Boot Actuator endpoints:

#### Health Check
```bash
GET http://localhost:8080/actuator/health
```

#### Metrics Overview
```bash
GET http://localhost:8080/actuator/metrics
```

#### Prometheus Format (for monitoring tools)
```bash
GET http://localhost:8080/actuator/prometheus
```

---

## API Endpoints

### Base URL
```
http://localhost:8080/api/metrics/performance
```

### Endpoint Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get all metrics |
| GET | `/{layer}/{methodName}` | Get specific method metrics |
| GET | `/summary` | Get summary statistics |
| GET | `/slow?thresholdMs={ms}` | Get slow methods |
| GET | `/top?limit={n}` | Get top N slowest methods |
| GET | `/layer/{layer}` | Get metrics by layer |
| GET | `/failures` | Get failure statistics |
| DELETE | `/reset` | Reset all metrics |
| POST | `/export/log` | Export to application log |

---

## Metrics Details

### Collected Metrics

For each monitored method, the following metrics are tracked:

**Call Statistics:**
- `totalCalls`: Total number of method invocations
- `successfulCalls`: Number of successful executions
- `failedCalls`: Number of failed executions
- `failureRate`: Percentage of failed calls

**Execution Time:**
- `avgExecutionTime`: Average execution time in milliseconds
- `minExecutionTime`: Minimum execution time recorded
- `maxExecutionTime`: Maximum execution time recorded
- `p50`: 50th percentile (median) execution time
- `p95`: 95th percentile execution time
- `p99`: 99th percentile execution time
- `stdDev`: Standard deviation of execution times

**Performance Classification:**
- `FAST`: < 100ms
- `NORMAL`: 100ms - 500ms
- `SLOW`: 500ms - 1000ms
- `CRITICAL`: > 1000ms

**Memory Usage:**
- Memory consumption per method call (logged but not in API response)

---

## Spring Boot Actuator Integration

### Configuration

Actuator is configured in `application.properties`:

```properties
# Actuator Configuration
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.endpoint.metrics.enabled=true
management.metrics.enable.all=true
management.endpoints.web.base-path=/actuator
management.prometheus.metrics.export.enabled=true
```

### Available Actuator Endpoints

1. **Health**: `/actuator/health`
   - Application health status
   - Database connectivity
   - Disk space

2. **Metrics**: `/actuator/metrics`
   - JVM metrics
   - System metrics
   - Application metrics

3. **Prometheus**: `/actuator/prometheus`
   - Metrics in Prometheus format
   - Can be scraped by Prometheus server

---

## Performance Analysis

### Using the Metrics

#### 1. Identify Slow Methods

```bash
# Get all methods slower than 500ms
curl http://localhost:8080/api/metrics/performance/slow?thresholdMs=500
```

**Analysis:**
- Methods in the response need optimization
- Check if the threshold is consistently exceeded
- Review P95 and P99 to understand outliers

#### 2. Monitor Failure Rates

```bash
curl http://localhost:8080/api/metrics/performance/failures
```

**Analysis:**
- High failure rates indicate reliability issues
- Check exception logs for failing methods
- May indicate database issues, external service problems, or validation errors

#### 3. Layer Performance Comparison

```bash
# Compare service vs repository layer
curl http://localhost:8080/api/metrics/performance/layer/SERVICE
curl http://localhost:8080/api/metrics/performance/layer/REPOSITORY
```

**Analysis:**
- Repository layer should generally be faster
- Slow repository methods may indicate database query issues
- Service layer includes business logic, so some overhead is expected

#### 4. Percentile Analysis

Percentiles help understand the distribution of execution times:

- **P50 (Median)**: Typical user experience
- **P95**: 95% of requests faster than this
- **P99**: 99% of requests faster than this

Large gaps between P50 and P99 indicate inconsistent performance.

### Performance Optimization Workflow

1. **Identify**: Use `/top` or `/slow` endpoints to find problematic methods
2. **Analyze**: Check percentiles and standard deviation
3. **Monitor**: Track failure rates and error logs
4. **Optimize**: Improve code, database queries, or caching
5. **Verify**: Compare metrics before and after changes
6. **Reset**: Use `/reset` to clear old metrics after changes

---

## Best Practices

### 1. Regular Monitoring

- Check performance metrics daily or after each deployment
- Set up automated alerts for slow methods
- Monitor failure rates continuously

### 2. Baseline Establishment

- Record baseline metrics after deployment
- Compare new metrics against baseline
- Reset metrics after major changes to get clean data

### 3. Thresholds

Current thresholds:
- **Slow threshold**: 1000ms (configurable in `PerformanceMonitoringAspect`)
- Adjust based on your application's SLA requirements

### 4. Metric Interpretation

- **High P99 vs P50**: Investigate outliers, may indicate occasional issues
- **High failure rate**: Critical - investigate immediately
- **Increasing avg time**: Potential memory leaks or resource exhaustion
- **High standard deviation**: Inconsistent performance, investigate causes

### 5. Memory Management

The aspect stores the last 1000 execution times per method for percentile calculation. This is configurable in the `MethodMetrics` class (`maxSampleSize`).

### 6. Log Analysis

- Logs include detailed performance data
- Use log aggregation tools (ELK, Splunk) for advanced analysis
- Set up alerts for SLOW or CRITICAL operations

### 7. Integration with Monitoring Tools

For production environments, integrate with:
- **Prometheus**: Scrape `/actuator/prometheus`
- **Grafana**: Create dashboards using Prometheus data
- **Application Performance Monitoring (APM)**: New Relic, DataDog, etc.

---

## Example Use Cases

### Use Case 1: Performance Regression Detection

**Scenario**: After a deployment, check if performance has degraded

```bash
# Before deployment - export baseline
POST http://localhost:8080/api/metrics/performance/export/log

# After deployment - compare
GET http://localhost:8080/api/metrics/performance/summary
GET http://localhost:8080/api/metrics/performance/top?limit=5
```

### Use Case 2: Database Query Optimization

**Scenario**: Identify slow database operations

```bash
# Get all repository layer metrics
GET http://localhost:8080/api/metrics/performance/layer/REPOSITORY

# Focus on slow queries
GET http://localhost:8080/api/metrics/performance/slow?thresholdMs=100
```

### Use Case 3: Service Health Check

**Scenario**: Quick health check of all services

```bash
# Get summary
GET http://localhost:8080/api/metrics/performance/summary

# Check failures
GET http://localhost:8080/api/metrics/performance/failures

# Verify no critical slow methods
GET http://localhost:8080/api/metrics/performance/slow?thresholdMs=1000
```

---

## Troubleshooting

### Issue: No metrics showing

**Cause**: Methods haven't been called yet
**Solution**: Execute some operations on the application, then check metrics

### Issue: Metrics seem inaccurate

**Cause**: Old data from previous runs
**Solution**: Reset metrics using `DELETE /api/metrics/performance/reset`

### Issue: High memory usage

**Cause**: Large number of method calls recorded
**Solution**:
- Reset metrics periodically
- Reduce `maxSampleSize` in `MethodMetrics` class
- Implement metric rotation

### Issue: Performance data in logs is overwhelming

**Cause**: Logging level set too verbose
**Solution**: Adjust logging level in `application.properties`:
```properties
logging.level.org.amalitech.bloggingplatformspring.aop=WARN
```

---

## Configuration

### Adjusting Performance Thresholds

Edit [PerformanceMonitoringAspect.java](../src/main/java/org/amalitech/bloggingplatformspring/aop/PerformanceMonitoringAspect.java):

```java
private static final long SLOW_THRESHOLD_MS = 1000; // Adjust this value
```

### Adjusting Sample Size

Edit the `MethodMetrics` inner class:

```java
private final int maxSampleSize = 1000; // Adjust this value
```

### Adding More Pointcuts

To monitor additional layers (e.g., controllers):

```java
@Pointcut("execution(* org.amalitech.bloggingplatformspring.controllers..*(..))")
public void controllerMethods() {
}

@Around("controllerMethods()")
public Object monitorControllerPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
    return monitorMethodPerformance(joinPoint, "CONTROLLER");
}
```

---

## Conclusion

The integrated AOP-based performance monitoring provides comprehensive insights into your application's performance. Regular monitoring and analysis of these metrics will help maintain optimal performance and quickly identify issues before they impact users.

For questions or issues, refer to the [AOP Implementation Guide](AOP_IMPLEMENTATION_GUIDE.md) or check the application logs.
