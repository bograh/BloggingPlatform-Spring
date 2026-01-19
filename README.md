# Blogging Platform - Spring Boot

A comprehensive blogging platform built with Spring Boot featuring REST API, GraphQL support, and Aspect-Oriented
Programming for monitoring and logging.

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [AOP Implementation](#aop-implementation)
- [GraphQL](#graphql)
- [Testing](#testing)
- [Project Structure](#project-structure)

## ✨ Features

### Core Functionality

- ✅ User registration and authentication
- ✅ Create, read, update, delete posts
- ✅ Comment on posts
- ✅ Tag management
- ✅ Post search and filtering
- ✅ Pagination support

### API Types

- 🌐 **REST API** - Traditional RESTful endpoints
- 📊 **GraphQL API** - Flexible query interface with GraphiQL UI
- 🔄 Dual database support (PostgreSQL + MongoDB)

### Cross-Cutting Concerns (AOP)

- 📝 **Comprehensive Logging** - Method execution logging across all layers
- ⚡ **Performance Monitoring** - Automatic execution time measurement
- ❌ **Exception Tracking** - Centralized exception logging and categorization
- 🎯 **CRUD Analytics** - Special monitoring for data operations

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Controllers Layer                      │
│  (REST Endpoints, GraphQL Resolvers)                        │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────▼───────────────┐
        │     AOP Aspects Layer       │
        │  (Logging, Performance,     │
        │   Exception Monitoring)     │
        └─────────────┬───────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                     Services Layer                          │
│  (Business Logic, Validation)                               │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                  Repository Layer                           │
│  (Data Access, PostgreSQL + MongoDB)                        │
└─────────────────────────────────────────────────────────────┘
```

## 🛠️ Technology Stack

- **Framework:** Spring Boot 3.5.9
- **Language:** Java 21
- **Build Tool:** Maven
- **Databases:**
    - PostgreSQL (Primary - Users, Posts, Tags)
    - MongoDB (Comments)
- **APIs:**
    - Spring Web (REST)
    - Spring GraphQL
- **AOP:** Spring AOP / AspectJ
- **Testing:**
    - JUnit 5
    - Mockito
    - Spring Test
    - H2 Database (test)
- **Code Quality:** JaCoCo (code coverage)

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven 3.6+
- PostgreSQL 12+
- MongoDB 4.0+

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd BloggingPlatform-Spring
   ```

2. **Configure databases**

   Update `src/main/resources/application-dev.properties`:

   ```properties
   # PostgreSQL
   spring.datasource.url=jdbc:postgresql://localhost:5432/blogging_platform
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   
   # MongoDB
   mongodb.database=blogging_platform
   mongodb.uri=mongodb://localhost:27017
   ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

5. **Access the APIs**
    - REST API: `http://localhost:8080/api/*`
    - GraphQL: `http://localhost:8080/graphql`
    - GraphiQL UI: `http://localhost:8080/graphiql` 👈 Interactive GraphQL explorer

## 📚 API Documentation

### REST Endpoints

#### Users

- `POST /api/users/register` - Register new user
- `POST /api/users/sign-in` - User sign in

#### Posts

- `POST /api/posts` - Create post
- `GET /api/posts/{postId}` - Get post by ID
- `GET /api/posts` - Get all posts (with pagination)
- `PUT /api/posts/{postId}` - Update post
- `DELETE /api/posts/{postId}` - Delete post
- `POST /api/posts/search` - Search posts

#### Comments

- `POST /api/comments` - Add comment to post
- `GET /api/comments/post/{postId}` - Get comments for post
- `DELETE /api/comments` - Delete comment

### GraphQL Queries & Mutations

See [GRAPHQL_GUIDE.md](docs/graphql/GRAPHQL_GUIDE.md)
and [GRAPHQL_TEST_QUERIES.md](docs/graphql/GRAPHQL_TEST_QUERIES.md) for complete
GraphQL documentation.

## 🎯 AOP Implementation

This project implements Aspect-Oriented Programming for handling cross-cutting concerns.

### Aspects

| Aspect                          | Purpose                             | Advice Types                                     |
|---------------------------------|-------------------------------------|--------------------------------------------------|
| **LoggingAspect**               | Method execution logging            | @Before, @After, @AfterReturning, @AfterThrowing |
| **PerformanceMonitoringAspect** | Execution time measurement          | @Around                                          |
| **ExceptionMonitoringAspect**   | Exception tracking & categorization | @AfterThrowing                                   |

### Features

✅ **Automatic Logging**

- Logs all service, controller, and repository method calls
- Captures input arguments and return values
- Minimal code intrusion

✅ **Performance Monitoring**

- Measures execution time of all service methods
- Identifies slow operations (> 1 second)
- Alerts on critical performance issues (> 5 seconds)
- Special monitoring for CRUD and query operations

✅ **Exception Tracking**

- Categorizes exceptions (400, 401, 403, 404, 500)
- Detects database errors
- Provides detailed error context
- Logs stack traces in debug mode

### Example Log Output

```
🔵 [BEFORE] Executing service method: PostService.createPost(..) with arguments: [CreatePostDTO(...)]
⚡ [PERFORMANCE] PostService.createPost(..) executed in 87 ms
📝 [CRUD-END] CRUD operation PostService.createPost(..) completed in 87 ms
✅ [AFTER-RETURNING] Method PostService.createPost(..) returned: PostResponseDTO
⚫ [AFTER] Completed service method: PostService.createPost(..)
```

### Performance Thresholds

- ⚡ **Fast:** < 100ms
- ⚡ **Normal:** 100-500ms
- ⚡ **Acceptable:** 500-1000ms
- 🟡 **Slow:** 1000-5000ms (WARNING)
- 🔴 **Very Slow:** > 5000ms (CRITICAL)

### Documentation

For complete AOP documentation, see [AOP_IMPLEMENTATION_GUIDE.md](docs/aop/AOP_IMPLEMENTATION_GUIDE.md)

## 📊 GraphQL

The application provides a full GraphQL API alongside REST.

### Quick Start

1. Start the application
2. Open GraphiQL: `http://localhost:8080/graphiql`
3. Try example queries from [GRAPHQL_TEST_QUERIES.md](docs/graphql/GRAPHQL_TEST_QUERIES.md)

### Example Query

```graphql
query {
  getPostById(postId: 1) {
    id
    title
    body
    author {
      username
    }
    tags {
      name
    }
    createdAt
  }
}
```

For complete documentation:

- [GRAPHQL_GUIDE.md](docs/graphql/GRAPHQL_GUIDE.md) - Integration guide
- [GRAPHQL_TEST_QUERIES.md](docs/graphql/GRAPHQL_TEST_QUERIES.md) - Example queries
- [README_GRAPHQL.md](docs/graphql/README_GRAPHQL.md) - Quick start

## 🧪 Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Suite

```bash
# Service tests
mvn test -Dtest=*ServiceTest

# DAO tests
mvn test -Dtest=*DAOTest

# GraphQL tests
mvn test -Dtest=GraphQLIntegrationTest
```

### Code Coverage

```bash
mvn clean test
```

Coverage report: `target/site/jacoco/index.html`

### Test Coverage

- ✅ Unit tests for all service methods
- ✅ DAO layer tests
- ✅ GraphQL integration tests
- ✅ Controller layer tests

## 📁 Project Structure

```
BloggingPlatform-Spring/
├── src/
│   ├── main/
│   │   ├── java/org/amalitech/bloggingplatformspring/
│   │   │   ├── aop/                    # AOP Aspects
│   │   │   │   ├── LoggingAspect.java
│   │   │   │   ├── PerformanceMonitoringAspect.java
│   │   │   │   ├── ExceptionMonitoringAspect.java
│   │   │   │   └── config/
│   │   │   │       └── AopConfig.java
│   │   │   ├── config/                 # Configuration
│   │   │   ├── controllers/            # REST Controllers
│   │   │   ├── dao/                    # Data Access Objects
│   │   │   ├── dtos/                   # Data Transfer Objects
│   │   │   ├── entity/                 # Entity classes
│   │   │   ├── enums/                  # Enumerations
│   │   │   ├── exceptions/             # Custom exceptions
│   │   │   ├── graphql/                # GraphQL layer
│   │   │   │   ├── config/
│   │   │   │   ├── resolvers/
│   │   │   │   └── types/
│   │   │   ├── repository/             # Repository layer
│   │   │   ├── services/               # Business logic
│   │   │   └── utils/                  # Utility classes
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       └── graphql/
│   │           └── schema.graphqls
│   └── test/                           # Test classes
│       └── java/org/amalitech/bloggingplatformspring/
│           ├── dao/
│           ├── graphql/
│           └── services/
├── docs/                               # Documentation
│   ├── AOP_IMPLEMENTATION_GUIDE.md    # AOP documentation
│   ├── GRAPHQL_GUIDE.md               # GraphQL guide
│   ├── GRAPHQL_TEST_QUERIES.md        # GraphQL examples
│   └── README_GRAPHQL.md              # GraphQL quick start
├── pom.xml                            # Maven configuration
└── README.md                          # This file
```

## 🔧 Configuration

### Application Profiles

- **dev** - Development profile (default)
- **prod** - Production profile

Switch profiles:

```bash
mvn spring-boot:run -Dspring.profiles.active=prod
```

### AOP Configuration

Control AOP logging levels in `application.properties`:

```properties
# AOP logging levels
logging.level.org.amalitech.bloggingplatformspring.aop=INFO
logging.level.org.amalitech.bloggingplatformspring.aop.LoggingAspect=DEBUG
logging.level.org.amalitech.bloggingplatformspring.aop.PerformanceMonitoringAspect=INFO
logging.level.org.amalitech.bloggingplatformspring.aop.ExceptionMonitoringAspect=ERROR
```

### GraphQL Configuration

```properties
# GraphQL
spring.graphql.graphiql.enabled=true
spring.graphql.graphiql.path=/graphiql
spring.graphql.path=/graphql
```

## 📈 Monitoring & Performance

### Performance Features

1. **Automatic Performance Tracking**
    - All service methods are monitored
    - Slow operations are logged with warnings
    - Critical performance issues trigger errors

2. **Database Query Monitoring**
    - Special tracking for query operations
    - Slow query detection and logging
    - Repository layer monitoring

3. **CRUD Operation Tracking**
    - Dedicated monitoring for create/update/delete operations
    - Operation timing and success tracking

### Best Practices

- Monitor logs for 🟡 SLOW and 🔴 VERY_SLOW indicators
- Investigate methods consistently exceeding thresholds
- Review exception categories regularly
- Use performance data to optimize critical paths

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License.

## 📞 Support

For issues, questions, or contributions:

- Create an issue in the repository
- Refer to documentation files in the docs/ directory
- Check the comprehensive guides:
    - [AOP_IMPLEMENTATION_GUIDE.md](docs/aop/AOP_IMPLEMENTATION_GUIDE.md)
    - [GRAPHQL_GUIDE.md](docs/graphql/GRAPHQL_GUIDE.md)

## 🎓 Learning Resources

- [Spring AOP Documentation](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [Spring GraphQL Documentation](https://docs.spring.io/spring-graphql/reference/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)

---