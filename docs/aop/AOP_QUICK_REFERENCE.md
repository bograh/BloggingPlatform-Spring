# AOP Quick Reference Card

## 🎯 Quick Overview

**What is AOP?** Aspect-Oriented Programming - a way to add cross-cutting concerns (logging, monitoring) without
modifying business code.

**Status:** ✅ Fully implemented and active

## 📦 Components

### Aspects Created

| Aspect                      | File                                   | Purpose                  |
|-----------------------------|----------------------------------------|--------------------------|
| LoggingAspect               | `aop/LoggingAspect.java`               | Method execution logging |
| PerformanceMonitoringAspect | `aop/PerformanceMonitoringAspect.java` | Performance measurement  |
| ExceptionMonitoringAspect   | `aop/ExceptionMonitoringAspect.java`   | Exception tracking       |
| AopConfig                   | `aop/config/AopConfig.java`            | Configuration            |

## 🔍 What Gets Monitored

### Automatically Monitored Layers

- ✅ **Services** - All methods in `services.*` package
- ✅ **Controllers** - All methods in `controllers.*` package
- ✅ **Repositories** - All methods in `repository.*` package

### Special Monitoring

- 📝 **CRUD Operations** - create*, update*, delete*, save*
- 🔍 **Query Operations** - get*, find*, search*
- 📊 **Analytics** - analytics*, statistics*, report*

## 📊 Log Symbol Guide

### Execution Flow

| Symbol | Meaning         | When                    |
|--------|-----------------|-------------------------|
| 🔵     | BEFORE          | Before method execution |
| ⚫      | AFTER           | After method completion |
| ✅      | AFTER-RETURNING | Successful execution    |
| ❌      | AFTER-THROWING  | Exception thrown        |

### Performance

| Symbol | Meaning     | Threshold      |
|--------|-------------|----------------|
| ⚡      | FAST/NORMAL | < 1000ms       |
| 🟡     | SLOW        | 1000-5000ms    |
| 🔴     | VERY SLOW   | > 5000ms       |
| 🐌     | SLOW QUERY  | Query > 1000ms |

### Operations

| Symbol | Type                |
|--------|---------------------|
| 📝     | CRUD operation      |
| 🔍     | Query operation     |
| 📊     | Analytics operation |
| 💾     | Repository/Database |
| 🌐     | Controller/API      |

### Exceptions

| Symbol | Category           | HTTP |
|--------|--------------------|------|
| 📂     | RESOURCE_NOT_FOUND | 404  |
| ⚠️     | BAD_REQUEST        | 400  |
| 🔒     | UNAUTHORIZED       | 401  |
| 🚫     | FORBIDDEN          | 403  |
| 💾     | DATABASE_ERROR     | 500  |

## 📝 Example Logs

### Normal Execution

```
🔵 [BEFORE] Executing service method: PostService.createPost(..)
⚡ [PERFORMANCE] PostService.createPost(..) executed in 87 ms
✅ [AFTER-RETURNING] Method returned: PostResponseDTO
⚫ [AFTER] Completed service method
```

### Slow Query Warning

```
🔍 [QUERY-START] Starting query: PostService.searchPosts(..)
🐌 [SLOW-QUERY] Query took 1547 ms (threshold: 1000 ms)
🟡 [SLOW] PostService.searchPosts(..) took 1547 ms
```

### Exception

```
❌ [SERVICE-EXCEPTION] Exception in UserService.registerUser
   Exception Type: BadRequestException
   Exception Message: Username is taken
   ⚠️ Category: BAD_REQUEST - Invalid input data
```

## ⚙️ Configuration

### Log Levels (application.properties)

```properties
# INFO - Normal operations
logging.level.org.amalitech.bloggingplatformspring.aop=INFO
# DEBUG - Detailed logging
logging.level.org.amalitech.bloggingplatformspring.aop.LoggingAspect=DEBUG
# WARN/ERROR - Performance issues and exceptions
logging.level.org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect=INFO
logging.level.org.amalitech.bloggingplatformspring.aop.ExceptionMonitoringAspect=ERROR
```

### Performance Thresholds

Defined in `PerformanceMonitoringAspect.java`:

```java
SLOW_METHOD_THRESHOLD =1000
ms        // 1 second
        VERY_SLOW_METHOD_THRESHOLD = 5000
ms   // 5 seconds
```

## 🧪 Testing AOP

### With Real Beans

```java

@SpringBootTest
public class MyTest {
    @Autowired
    private PostService postService; // AOP works ✅

    @Test
    public void test() {
        postService.createPost(dto); // Logs will appear
    }
}
```

### With Mocks (AOP Disabled)

```java

@ExtendWith(MockitoExtension.class)
public class MyTest {
    @Mock
    private PostRepository repo;

    @InjectMocks
    private PostService postService; // AOP doesn't work ❌
}
```

## 🚀 Running Application with AOP

### Start Application

```bash
mvn spring-boot:run
```

### Check AOP Initialization

Look for startup logs:

```
🔧 AOP Configuration initialized
   ✅ LoggingAspect enabled
   ✅ PerformanceMonitoringAspect enabled
   ✅ ExceptionMonitoringAspect enabled
```

### Make API Call

```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{"title": "Test", "body": "Content", "authorId": "uuid", "tags": []}'
```

Watch console for AOP logs!

## 🎯 Use Cases

### 1. Debugging

- See method execution flow
- Check input arguments
- View return values
- Track exception sources

### 2. Performance Optimization

- Identify slow methods
- Find bottlenecks
- Monitor query performance
- Track CRUD operation timing

### 3. Production Monitoring

- Track API usage
- Monitor error rates
- Detect performance degradation
- Alert on critical issues

### 4. Analytics

- Method call frequency
- Average execution times
- Exception categories
- Database query patterns

## ⚡ Quick Actions

### View All Logs

```bash
mvn spring-boot:run | tee app.log
```

### Filter Performance Issues

```bash
mvn spring-boot:run | grep "SLOW\|PERFORMANCE"
```

### Monitor Exceptions

```bash
mvn spring-boot:run | grep "EXCEPTION"
```

### Watch CRUD Operations

```bash
mvn spring-boot:run | grep "CRUD"
```

## 🔧 Troubleshooting

### No AOP Logs?

1. **Check initialization**
    - Look for "AOP Configuration initialized" in startup logs

2. **Verify dependencies**
    - Confirm `spring-boot-starter-aop` in pom.xml

3. **Check log levels**
    - Set to DEBUG if logs are missing

4. **Using mocks?**
    - AOP doesn't work with `@Mock` objects

### False "Slow" Warnings?

Adjust thresholds in `PerformanceMonitoringAspect.java`:

```java
private static final long SLOW_METHOD_THRESHOLD = 2000; // Change to 2 seconds
```

## 📚 Documentation

| Document                                                   | Description              |
|------------------------------------------------------------|--------------------------|
| [AOP_IMPLEMENTATION_GUIDE.md](AOP_IMPLEMENTATION_GUIDE.md) | Complete guide           |
| [README.md](../../README.md)                               | Project overview         |
| Source Code                                                | `src/main/java/.../aop/` |

## 🎓 Advice Types Reference

| Advice Type     | Annotation        | Use For                    |
|-----------------|-------------------|----------------------------|
| Before          | `@Before`         | Pre-processing, validation |
| After           | `@After`          | Cleanup, final logging     |
| After Returning | `@AfterReturning` | Success logging            |
| After Throwing  | `@AfterThrowing`  | Exception handling         |
| Around          | `@Around`         | Performance, transactions  |

## ✅ Acceptance Criteria Met

- ✅ AOP aspects implemented using @Before, @After, and @Around
- ✅ Logging and monitoring applied to critical service methods
- ✅ Performance measurements integrated within AOP aspects
- ✅ Implementation documented within project files and README

---

**Quick Links:**

- Full Guide: [AOP_IMPLEMENTATION_GUIDE.md](AOP_IMPLEMENTATION_GUIDE.md)
- Main README: [README.md](../../README.md)
- Source: `src/main/java/org/amalitech/bloggingplatformspring/aop/`