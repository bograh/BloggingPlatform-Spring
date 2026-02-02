# Blogging Platform - Spring Boot

A production-ready, enterprise-grade blogging platform built with Spring Boot 3.5.9 featuring dual API support (REST + GraphQL), comprehensive performance monitoring, intelligent caching, and advanced cross-cutting concerns through Aspect-Oriented Programming.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Performance & Monitoring](#performance--monitoring)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Configuration](#configuration)
- [Contributing](#contributing)
- [License](#license)

## Overview

This blogging platform is a full-featured content management system designed with modern software engineering practices. It demonstrates enterprise-level patterns including:

- **Dual API Architecture** - REST and GraphQL for maximum flexibility
- **Hybrid Database Strategy** - PostgreSQL for relational data, MongoDB for flexible documents
- **Production-Ready Monitoring** - Performance metrics, cache statistics, and comprehensive logging
- **Security Best Practices** - Sensitive data masking, input validation, and secure password hashing
- **High Test Coverage** - 80%+ code coverage with unit, integration, and E2E tests

## Features

### Core Functionality
- ✅ **User Management** - Registration, authentication, profile management
- ✅ **Post Operations** - Create, read, update, delete with rich text support
- ✅ **Comments System** - Threaded comments stored in MongoDB for flexibility
- ✅ **Tagging System** - Many-to-many relationships with intelligent tag management
- ✅ **Advanced Search** - Filtering, pagination, sorting by multiple criteria

### API & Integration
- 🚀 **Dual API Support** - REST (OpenAPI 3.0) + GraphQL with schema introspection
- 📚 **Interactive Documentation** - Swagger UI for REST, GraphiQL for GraphQL
- 🔄 **Real-time Schema** - GraphQL schema with type-safe queries and mutations
- 🌐 **CORS Support** - Configured for cross-origin requests

### Performance & Monitoring
- 📊 **Performance Metrics** - Method-level execution time tracking
- 💾 **Intelligent Caching** - Multi-level caching with hit/miss rate monitoring
- 📈 **Cache Analytics** - Hit rates, miss rates, eviction tracking per cache
- 📁 **Metrics Export** - Combined performance and cache metrics to file and logs
- ⚡ **Query Optimization** - Indexed database queries with lazy loading

### Cross-Cutting Concerns (AOP)
- 🔍 **Comprehensive Logging** - Request/response logging with execution tracking
- 🎭 **Sensitive Data Masking** - Automatic PII protection in logs
- 🛡️ **Exception Handling** - Centralized error handling with detailed responses
- ⏱️ **Performance Monitoring** - Real-time method execution tracking
- 🔔 **Slow Query Detection** - Automatic alerts for methods exceeding thresholds

### Data & Storage
- 🗄️ **Hybrid Database** - PostgreSQL for structured data, MongoDB for flexible documents
- 🔐 **Secure Storage** - Bcrypt password hashing
- 📦 **Data Validation** - Jakarta Bean Validation throughout
- 🔄 **Transaction Management** - ACID compliance for critical operations

### Quality Assurance
- ✅ **80%+ Test Coverage** - Comprehensive test suite with JaCoCo reporting
- 🧪 **Multiple Test Types** - Unit, integration, and GraphQL tests
- 🎯 **Continuous Testing** - Automated test execution with Maven
- 📊 **Coverage Reports** - Detailed HTML coverage analysis

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Applications                      │
│            (Web, Mobile, Third-party Services)              │
└────────────┬──────────────────────────────┬─────────────────┘
             │                              │
             ▼                              ▼
      ┌─────────────┐              ┌──────────────┐
      │   REST API  │              │  GraphQL API │
      │  (OpenAPI)  │              │  (Schema)    │
      └──────┬──────┘              └──────┬───────┘
             │                            │
             └────────────┬───────────────┘
                          ▼
              ┌───────────────────────┐
              │   Controllers Layer   │
              │  (REST & GraphQL)     │
              └───────────┬───────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │    AOP Aspects        │◄──── Logging
              │  (Cross-cutting)      │◄──── Performance
              └───────────┬───────────┘◄──── Caching
                          │            ◄──── Masking
                          ▼
              ┌───────────────────────┐
              │   Services Layer      │
              │  (Business Logic)     │
              └───────────┬───────────┘
                          │
             ┌────────────┴────────────┐
             ▼                         ▼
      ┌─────────────┐          ┌─────────────┐
      │ Repository  │          │ Repository  │
      │ (JPA/SQL)   │          │ (MongoDB)   │
      └──────┬──────┘          └──────┬──────┘
             │                        │
             ▼                        ▼
      ┌─────────────┐          ┌─────────────┐
      │ PostgreSQL  │          │  MongoDB    │
      │ (Relational)│          │ (Document)  │
      └─────────────┘          └─────────────┘
```

### Design Patterns Used

- **Repository Pattern** - Data access abstraction
- **DTO Pattern** - Request/response data transfer
- **Aspect-Oriented Programming** - Cross-cutting concerns
- **Service Layer Pattern** - Business logic encapsulation
- **Builder Pattern** - Complex object construction (DTOs)
- **Factory Pattern** - GraphQL type creation
- **Singleton Pattern** - Cache statistics management

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.6+
- PostgreSQL 17+
- MongoDB 4.0+

### Installation & Running

```bash
# 1. Clone the repository
git clone <repository-url>
cd BloggingPlatform-Spring

# 2. Configure databases in src/main/resources/application-dev.properties
# PostgreSQL and MongoDB connection settings

# 3. Build the project
mvn clean install

# 4. Run the application
mvn spring-boot:run
```

### Access Points

Once running, access the application at:

| Interface              | URL                                        | Description                           |
|------------------------|--------------------------------------------|---------------------------------------|
| **Swagger UI**         | http://localhost:8080/swagger-ui.html      | REST API documentation & testing      |
| **GraphiQL**           | http://localhost:8080/graphiql             | GraphQL interactive query interface   |
| **OpenAPI Spec**       | http://localhost:8080/v3/api-docs          | OpenAPI 3.0 JSON specification        |
| **REST API**           | http://localhost:8080/api/v1/*             | RESTful endpoints base path           |
| **GraphQL API**        | http://localhost:8080/graphql              | GraphQL endpoint                      |
| **Performance Metrics**| http://localhost:8080/api/metrics/performance | Performance monitoring endpoints   |
| **Cache Metrics**      | http://localhost:8080/api/metrics/performance/cache | Cache statistics            |

## API Documentation

### REST API Endpoints

#### Users API (`/api/v1/users`)
- `POST /register` - Register a new user
- `GET /{userId}` - Get user by ID
- `PUT /{userId}` - Update user information
- `DELETE /{userId}` - Delete user account

#### Posts API (`/api/v1/posts`)
- `POST /` - Create a new post
- `GET /` - Get all posts (paginated, filterable, sortable)
- `GET /{postId}` - Get post by ID
- `PUT /{postId}` - Update existing post
- `DELETE /{postId}` - Delete post

#### Comments API (`/api/v1/comments`)
- `POST /` - Add comment to a post
- `GET /post/{postId}` - Get all comments for a post
- `GET /{commentId}` - Get specific comment
- `DELETE /{commentId}` - Delete comment

#### Tags API (`/api/v1/tags`)
- `GET /popular` - Get most used tags
- `POST /refresh` - Refresh tag cache

#### Performance Metrics API (`/api/metrics/performance`)
- `GET /` - Get all performance metrics
- `GET /summary` - Get metrics summary
- `GET /{layer}/{methodName}` - Get specific method metrics
- `DELETE /reset` - Reset all performance metrics
- `POST /export-log` - Export performance metrics to file
- `GET /cache` - Get all cache metrics
- `GET /cache/{cacheName}` - Get specific cache metrics
- `GET /cache/summary` - Get cache performance summary
- `DELETE /cache/reset` - Reset cache statistics
- `POST /cache/export-log` - Export cache metrics to file
- `POST /export-all` - Export combined metrics to file

### GraphQL API

**Query Operations:**
```graphql
# Get all posts with pagination
getAllPosts(page: Int, size: Int, sortBy: String, order: String): [Post!]!

# Get post by ID with relationships
getPostById(postId: ID!): Post

# Get user by ID
getUserById(userId: ID!): User

# Get comments for a post
getCommentsByPostId(postId: ID!): [Comment!]!
```

**Mutation Operations:**
```graphql
# Create a new post
createPost(input: CreatePostInput!): Post!

# Update existing post
updatePost(postId: ID!, input: UpdatePostInput!): Post!

# Delete post
deletePost(postId: ID!, authorId: ID!): Boolean!

# Create comment
createComment(input: CreateCommentInput!): Comment!
```

**For detailed examples:**
- **REST API**: Visit [Swagger UI](http://localhost:8080/swagger-ui.html) or see [ENDPOINTS.md](dev/ENDPOINTS.md)
- **GraphQL**: Visit [GraphiQL](http://localhost:8080/graphiql) or see [GraphQL Test Queries](docs/graphql/GRAPHQL_TEST_QUERIES.md)

## Database Schema

### Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         PostgreSQL Database                     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────┐                   ┌─────────────────┐
│      User       │                   │      Post       │
├─────────────────┤                   ├─────────────────┤
│ PK id (UUID)    │◄───────┐          │ PK id (BIGINT)  │
│    username     │        │          │    title        │
│    email        │        │          │    body (TEXT)  │
│    password     │        │          │ FK author_id    │
│    created_at   │        └──────────┤    posted_at    │
└─────────────────┘                   │    updated_at   │
         │                            └────────┬────────┘
         │ 1:N                                 │
         │ (posts)                             │ M:N
         │                                     │ (tags)
         │                             ┌───────┴────────┐
         │                             │                │
         │                      ┌──────▼───────┐  ┌─────▼──────┐
         │                      │  post_tags   │  │    Tag     │
         │                      │ (Join Table) │  ├────────────┤
         │                      ├──────────────┤  │ PK id      │
         │                      │ FK post_id   │  │    name    │
         │                      │ FK tag_id    │  └────────────┘
         │                      └──────────────┘
         │
         │ Reference (author_id)
         │
         ▼
┌──────────────────────────────────────────────────────────────────┐
│                         MongoDB Database                         │
└──────────────────────────────────────────────────────────────────┘

         ┌──────────────────┐
         │     Comment      │
         ├──────────────────┤
         │ PK _id (String)  │
         │    post_id       │◄──── References Post.id
         │    author_id     │◄──── References User.id
         │    author        │
         │    content       │
         │    commented_at  │
         └──────────────────┘
```

### Relationships

| Relationship | Type  | Description |
|-------------|-------|-------------|
| User → Post | 1:N   | A user can create multiple posts |
| Post → Tag  | M:N   | Posts can have multiple tags; tags can belong to multiple posts |
| Post → Comment | 1:N (virtual) | Comments reference posts via post_id (stored in MongoDB) |
| User → Comment | 1:N (virtual) | Comments reference users via author_id (stored in MongoDB) |

### Database Indexes

**PostgreSQL:**
- Users: `idx_username`, `idx_email`, `idx_created_at`
- Posts: `idx_author_id`, `idx_posted_at`, `idx_author_posted`
- Tags: `idx_name`

**MongoDB:**
- Comments: Auto-indexed on `_id`, indexed on `post_id`, `author_id`

**For detailed database documentation:**
- [Complete Database Schema](docs/DATABASE_SCHEMA.md) - Full schema with SQL, relationships, and data flow
- [Entity Relationship Diagram](docs/ER_DIAGRAM.md) - Visual ER diagrams with detailed cardinality
- [Database Quick Reference](docs/DATABASE_QUICK_REFERENCE.md) - Quick lookup for tables and queries

## Performance & Monitoring

### Performance Metrics

The application tracks detailed performance metrics for all service-layer methods:

- **Execution Time**: Min, max, and average execution times
- **Call Statistics**: Total calls, successful calls, failed calls
- **Failure Rate**: Percentage of failed operations
- **Slow Query Detection**: Automatic logging of methods exceeding 1000ms

**Access metrics:**
```bash
# Get all performance metrics
curl http://localhost:8080/api/metrics/performance

# Get metrics summary
curl http://localhost:8080/api/metrics/performance/summary

# Export to file (creates metrics/YYYYMMDD-HHmmss-performance-summary.log)
curl -X POST http://localhost:8080/api/metrics/performance/export-log
```

### Cache Monitoring

Intelligent caching with comprehensive statistics tracking:

**Caches:**
- `users` - User profile caching
- `posts` - Individual post caching
- `allPosts` - Post list caching
- `comments` - Comment caching
- `tags` - Popular tags caching

**Metrics tracked per cache:**
- Hit/Miss counts and rates
- Total requests
- Cache puts (additions)
- Evictions (removals)
- Cache clears

**Access cache metrics:**
```bash
# Get all cache metrics
curl http://localhost:8080/api/metrics/performance/cache

# Get cache summary with hit rates
curl http://localhost:8080/api/metrics/performance/cache/summary

# Get specific cache metrics
curl http://localhost:8080/api/metrics/performance/cache/users

# Export cache metrics to file
curl -X POST http://localhost:8080/api/metrics/performance/cache/export-log

# Export combined performance + cache metrics
curl -X POST http://localhost:8080/api/metrics/performance/export-all
```

**Example cache summary response:**
```json
{
  "totalCaches": 5,
  "totalHits": 1523,
  "totalMisses": 287,
  "totalRequests": 1810,
  "overallHitRate": "84.14%",
  "totalPuts": 342,
  "totalEvictions": 12,
  "bestPerformingCache": {
    "name": "users",
    "hitRate": "92.31%"
  },
  "worstPerformingCache": {
    "name": "allPosts",
    "hitRate": "67.45%"
  }
}
```

### Logging & Data Masking

All requests and responses are logged with automatic sensitive data masking:

- **Masked Fields**: Passwords, email addresses (partially), authentication tokens
- **Request/Response Logging**: Method name, execution time, parameters (masked)
- **Exception Tracking**: Detailed error logging with stack traces
- **Performance Alerts**: Automatic warnings for slow operations

## Documentation

Comprehensive documentation is available in the `docs/` directory:

### API Documentation

- **[OpenAPI Documentation Guide](docs/api/OPENAPI_DOCUMENTATION_GUIDE.md)** - Complete guide to REST API with Swagger
  UI, including all endpoints, request/response examples, and integration instructions

### GraphQL Documentation

- **[GraphQL Guide](docs/graphql/GRAPHQL_GUIDE.md)** - Implementation details and integration guide
- **[GraphQL Test Queries](docs/graphql/GRAPHQL_TEST_QUERIES.md)** - Example queries and mutations for all operations
- **[GraphQL Implementation Summary](docs/graphql/GRAPHQL_IMPLEMENTATION_SUMMARY.md)** - Technical architecture and
  resolver details
- **[GraphQL README](docs/graphql/README_GRAPHQL.md)** - Quick start guide

### AOP (Aspect-Oriented Programming) Documentation

- **[AOP Implementation Guide](docs/aop/AOP_IMPLEMENTATION_GUIDE.md)** - Complete guide to logging, performance
  monitoring, and exception tracking
- **[AOP Quick Reference](docs/aop/AOP_QUICK_REFERENCE.md)** - Quick reference for aspect usage
- **[Performance Metrics Guide](docs/aop/PERFORMANCE_METRICS_GUIDE.md)** - Performance monitoring setup and usage
- **[Performance Metrics Quick Reference](docs/aop/PERFORMANCE_METRICS_QUICK_REFERENCE.md)** - Quick reference for
  metrics endpoints
- **[Cache Monitoring Guide](docs/aop/CACHE_MONITORING_GUIDE.md)** - Cache statistics and monitoring
- **[Sensitive Data Masking](docs/aop/SENSITIVE_DATA_MASKING.md)** - Security and privacy features
- **[Request Masking Examples](docs/aop/REQUEST_MASKING_EXAMPLES.md)** - Examples of data masking in action

### Additional Documentation

- **[API Endpoints Reference](dev/ENDPOINTS.md)** - Complete REST API endpoint reference with request/response examples

## API Access

### REST API Quick Examples

```bash
# Register a user
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","email":"john@example.com","password":"SecurePass123!"}'

# Create a post
curl -X POST http://localhost:8080/api/v1/posts \
  -H "Content-Type: application/json" \
  -d '{"title":"My First Post","body":"This is the content of my post","authorId":"<uuid>","tags":["tech","spring"]}'

# Get all posts with pagination and sorting
curl "http://localhost:8080/api/v1/posts?page=0&size=10&sortBy=postedAt&order=desc"

# Add a comment
curl -X POST http://localhost:8080/api/v1/comments \
  -H "Content-Type: application/json" \
  -d '{"postId":1,"authorId":"<uuid>","author":"john_doe","content":"Great post!"}'

# Get performance metrics summary
curl http://localhost:8080/api/metrics/performance/summary

# Get cache statistics
curl http://localhost:8080/api/metrics/performance/cache/summary
```

**For complete endpoint documentation, visit [Swagger UI](http://localhost:8080/swagger-ui.html) or
see [API Endpoints Reference](dev/ENDPOINTS.md)**

### GraphQL Quick Example

```graphql
# Query: Get post with author and tags
query {
    getPostById(postId: 1) {
        id
        title
        body
        postedAt
        author {
            id
            username
            email
        }
        tags {
            id
            name
        }
    }
}

# Mutation: Create a new post
mutation {
    createPost(input: {
        title: "My GraphQL Post"
        body: "Content created via GraphQL"
        authorId: "<uuid>"
        tags: ["graphql", "api"]
    }) {
        id
        title
        postedAt
    }
}

# Query: Get paginated posts
query {
    getAllPosts(page: 0, size: 10, sortBy: "postedAt", order: "DESC") {
        id
        title
        author {
            username
        }
        tags {
            name
        }
    }
}
```

**For more examples, visit [GraphiQL](http://localhost:8080/graphiql) or
see [GraphQL Test Queries](docs/graphql/GRAPHQL_TEST_QUERIES.md)**

## Technology Stack

### Core Framework
- **Spring Boot** 3.5.9 (Latest stable release)
- **Java** 21 (LTS with modern features)
- **Maven** 3.6+ (Dependency management and build)

### Databases
- **PostgreSQL** 12+ (Relational data: Users, Posts, Tags)
- **MongoDB** 4.0+ (Document storage: Comments)
- **H2** 2.2.224 (In-memory database for testing)

### APIs & Documentation
- **Spring Web** - RESTful API implementation
- **Spring GraphQL** - GraphQL API implementation
- **SpringDoc OpenAPI** 2.8.15 - OpenAPI 3.0 specification + Swagger UI
- **GraphiQL** - Interactive GraphQL interface

### Data Access
- **Spring Data JPA** - PostgreSQL repository abstraction
- **Spring Data MongoDB** - MongoDB repository abstraction
- **Hibernate** - JPA implementation with optimizations

### Cross-Cutting Concerns
- **Spring AOP** - Aspect-Oriented Programming
- **Spring Boot Actuator** - Production monitoring
- **Spring Cache** - Caching abstraction with statistics

### Security & Validation
- **Jakarta Bean Validation** - Input validation
- **BCrypt** 0.10.2 - Secure password hashing

### Testing
- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking framework
- **Spring Test** - Integration testing support
- **Spring GraphQL Test** - GraphQL testing utilities
- **JaCoCo** 0.8.12 - Code coverage analysis

### Additional Libraries
- **Lombok** - Boilerplate code reduction
- **SLF4J/Logback** - Logging framework

## Project Structure

```
BloggingPlatform-Spring/
├── src/
│   ├── main/
│   │   ├── java/org/amalitech/bloggingplatformspring/
│   │   │   ├── aop/                         # Aspect-Oriented Programming
│   │   │   │   ├── LoggingAspect.java       # Request/response logging
│   │   │   │   ├── PerformanceMonitoringAspect.java  # Performance tracking
│   │   │   │   └── config/                  # AOP configuration
│   │   │   ├── config/                      # Application configuration
│   │   │   │   ├── CacheConfig.java         # Cache setup with monitoring
│   │   │   │   ├── OpenApiConfig.java       # Swagger/OpenAPI config
│   │   │   │   └── CorsConfig.java          # CORS configuration
│   │   │   ├── controllers/                 # REST controllers
│   │   │   │   ├── UserController.java
│   │   │   │   ├── PostController.java
│   │   │   │   ├── CommentController.java
│   │   │   │   ├── TagController.java
│   │   │   │   └── PerformanceMetricsController.java
│   │   │   ├── graphql/                     # GraphQL implementation
│   │   │   │   ├── resolvers/               # Query & Mutation resolvers
│   │   │   │   ├── types/                   # GraphQL types
│   │   │   │   ├── utils/                   # GraphQL utilities
│   │   │   │   └── config/                  # GraphQL configuration
│   │   │   ├── services/                    # Business logic layer
│   │   │   │   ├── UserService.java
│   │   │   │   ├── PostService.java
│   │   │   │   ├── CommentService.java
│   │   │   │   ├── TagService.java
│   │   │   │   └── PerformanceMetricsService.java
│   │   │   ├── repository/                  # Data access layer
│   │   │   │   ├── UserRepository.java      # JPA repository
│   │   │   │   ├── PostRepository.java      # JPA repository
│   │   │   │   ├── TagRepository.java       # JPA repository
│   │   │   │   └── CommentRepository.java   # MongoDB repository
│   │   │   ├── entity/                      # Domain entities
│   │   │   │   ├── User.java                # PostgreSQL entity
│   │   │   │   ├── Post.java                # PostgreSQL entity
│   │   │   │   ├── Tag.java                 # PostgreSQL entity
│   │   │   │   └── Comment.java             # MongoDB document
│   │   │   ├── dtos/                        # Data Transfer Objects
│   │   │   │   ├── requests/                # API request DTOs
│   │   │   │   └── responses/               # API response DTOs
│   │   │   ├── exceptions/                  # Exception handling
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── Custom exceptions
│   │   │   ├── enums/                       # Enumerations
│   │   │   └── utils/                       # Utility classes
│   │   └── resources/
│   │       ├── application.properties       # Main configuration
│   │       ├── application-dev.properties   # Development config
│   │       ├── graphql/
│   │       │   └── schema.graphqls          # GraphQL schema
│   │       ├── static/                      # Static resources
│   │       └── templates/                   # Template files
│   └── test/
│       ├── java/org/amalitech/bloggingplatformspring/
│       │   ├── aop/                         # AOP tests
│       │   ├── config/                      # Configuration tests
│       │   ├── controllers/                 # Controller tests
│       │   ├── services/                    # Service tests
│       │   ├── entity/                      # Entity tests
│       │   └── graphql/                     # GraphQL tests
│       └── resources/
│           └── application-test.properties  # Test configuration
├── docs/                                    # Documentation
│   ├── api/                                 # REST API documentation
│   ├── graphql/                             # GraphQL documentation
│   └── aop/                                 # AOP documentation
├── dev/                                     # Development resources
│   └── ENDPOINTS.md                         # API endpoint reference
├── metrics/                                 # Exported metrics (generated)
├── logs/                                    # Application logs (generated)
├── target/                                  # Build output
│   └── site/jacoco/                         # Coverage reports
├── pom.xml                                  # Maven configuration
└── README.md                                # This file
```

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage report
mvn clean test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run tests and skip compilation
mvn surefire:test

# Generate coverage report
mvn jacoco:report
```

### Viewing Coverage Reports

```bash
# After running tests, open the coverage report
open target/site/jacoco/index.html

# Or navigate to:
# target/site/jacoco/index.html
```

### Test Structure

- **Unit Tests**: Service layer business logic, entity validation
- **Integration Tests**: Controller endpoints, database operations
- **GraphQL Tests**: Query and mutation resolvers
- **AOP Tests**: Logging aspects, performance monitoring
- **Coverage Target**: 80%+ line coverage (currently achieved)

### Test Configuration

Tests use:
- **H2 in-memory database** for PostgreSQL tests
- **Embedded MongoDB** for MongoDB tests
- **MockMvc** for controller testing
- **Mockito** for mocking dependencies

## Configuration

### Database Configuration

**PostgreSQL** (`application-dev.properties`):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/blogging_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

**MongoDB** (`application-dev.properties`):
```properties
spring.data.mongodb.uri=mongodb://localhost:27017/blogging_platform
spring.data.mongodb.database=blogging_platform
```

### Cache Configuration

Caches are pre-configured in `CacheConfig.java`:
- `users` - User profile cache
- `posts` - Individual posts
- `allPosts` - Post listings
- `comments` - Comment data
- `tags` - Popular tags

### Performance Monitoring

Performance thresholds and settings in `PerformanceMonitoringAspect.java`:
```java
private static final long SLOW_THRESHOLD_MS = 1000; // Log slow queries
```

### CORS Configuration

CORS is configured in `CorsConfig.java` for cross-origin requests:
- Allowed origins: Configurable
- Allowed methods: GET, POST, PUT, DELETE, OPTIONS
- Allowed headers: All
- Credentials: Supported

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

Please ensure:

- Tests pass (`mvn test`)
- Code coverage remains above 80%
- Documentation is updated for new features

## License

This project is licensed under the MIT License.

## Learning Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring AOP Documentation](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [Spring GraphQL Documentation](https://docs.spring.io/spring-graphql/reference/)
- [SpringDoc OpenAPI Documentation](https://springdoc.org/)

---