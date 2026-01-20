# AOP Quick Reference Card

## 🎯 Quick Overview

**What is AOP?** Aspect-Oriented Programming - a way to add cross-cutting concerns (logging, monitoring) without
modifying business code.

**Status:** ✅ Fully implemented and active

## 📦 Components

### Aspects Created

| Aspect                      | File                                   | Purpose                  |
|-----------------------------|----------------------------------------|--------------------------|
| LoggingAspect               | `aop/LoggingAspect.java`               | Method execution logging with **sensitive data masking** |
| PerformanceMonitoringAspect | `aop/PerformanceMonitoringAspect.java` | Performance measurement and metrics |
| AopConfig                   | `aop/config/AopConfig.java`            | Configuration            |

## 🔍 What Gets Monitored

### Automatically Monitored Layers

- ✅ **Services** - All methods in `services..*` package
- ✅ **Repositories** - All methods in `repository..*` package

### Special Monitoring

- 📝 **CRUD Operations** - create*, update*, delete* (enhanced logging with timing)
- 📊 **Analytics** - *Analytics*, *Report*, *Statistics* (detailed parameter and timing logging)

## 🔒 Sensitive Data Masking

### Automatically Masked Fields

The following fields are automatically masked in logs (case-insensitive):

- **Passwords:** password, passwd, pwd
- **Tokens:** token, accessToken, refreshToken
- **Secrets:** secret, secretKey, apiKey
- **Payment:** creditCard, cvv, cvc, cardNumber
- **Personal:** ssn, pin, socialSecurityNumber
- **Security:** authorization, privateKey

### Example
```
==> Entering method: UserService.registerUser(..) with arguments:
[RegisterUserDTO{username="john", email="john@example.com", password=***MASKED***}]
```

**See:** [SENSITIVE_DATA_MASKING.md](SENSITIVE_DATA_MASKING.md) for complete documentation.

##  Log Symbol Guide

### Execution Flow

| Symbol | Meaning         | When                    |
|--------|-----------------|-------------------------|
| ==>    | BEFORE          | Before method execution |
| <==    | AFTER-RETURNING | Successful execution    |
| <!>    | AFTER-THROWING  | Exception thrown        |
| [AUDIT]| AFTER           | After method completion |

### Performance

| Level    | Threshold      | Description |
|----------|----------------|-------------|
| FAST     | < 100ms        | Optimal     |
| NORMAL   | 100-500ms      | Acceptable  |
| SLOW     | 500-1000ms     | Getting slow|
| CRITICAL | > 1000ms       | Warning     |

### Operations

| Prefix       | Type                |
|--------------|---------------------|
| [CRUD]       | CRUD operation      |
| [ANALYTICS]  | Analytics operation |
| [PERFORMANCE]| Performance metrics |
| [AUDIT]      | Audit logging       |

## 📝 Example Logs

### Normal Execution

```
==> Entering method: PostService.createPost(..) with arguments: [CreatePostDTO{title="My Post", body="Content here...", authorId="123"}]
[CRUD] Starting operation: PostService.createPost(..)
[PERFORMANCE] 2026-01-20 10:15:30 | FAST | Method: SERVICE::PostService.createPost(..) | Execution Time: 87 ms | Memory: 256 KB | Status: SUCCESS
[CRUD] Successfully completed operation: PostService.createPost(..) in 87 ms
<== Successfully completed method: PostService.createPost(..) with result: PostResponseDTO
[AUDIT] Method execution completed - Class: PostService, Method: PostService.createPost(..)
```

### Execution with Sensitive Data

```
==> Entering method: UserService.registerUser(..) with arguments: [RegisterUserDTO{username="john_doe", email="john@example.com", password=***MASKED***}]
[CRUD] Starting operation: UserService.registerUser(..)
[CRUD] Successfully completed operation: UserService.registerUser(..) in 120 ms
<== Successfully completed method: UserService.registerUser(..) with result: UserResponseDTO
```

### Slow Operation Warning

```
==> Entering method: PostService.searchPosts(..) with arguments: [SearchDTO(...)]
[PERFORMANCE] SLOW SERVICE OPERATION DETECTED: PostService.searchPosts(..) took 1547 ms
[PERFORMANCE] 2026-01-20 10:15:35 | CRITICAL | Method: SERVICE::PostService.searchPosts(..) | Execution Time: 1547 ms | Memory: 1024 KB | Status: SUCCESS
<== Successfully completed method: PostService.searchPosts(..) with result: PageResponse
```

### Exception

```
==> Entering method: UserService.registerUser(..) with arguments: [RegisterUserDTO(...)]
<!> Exception in method: UserService.registerUser(..) - Exception type: BadRequestException - Message: Username is taken
[AUDIT] Method execution completed - Class: UserService, Method: UserService.registerUser(..)
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
SLOW_THRESHOLD_MS = 1000ms  // 1 second
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
AOP Configuration initialized
LoggingAspect enabled
PerformanceMonitoringAspect enabled
Monitoring service layer, controllers, and repositories
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

### Adjust Performance Thresholds

Modify thresholds in `PerformanceMonitoringAspect.java`:

```java
private static final long SLOW_THRESHOLD_MS = 2000; // Change to 2 seconds
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