# Blogging Platform - Spring Boot

A production-ready blogging platform built with Spring Boot featuring REST API, GraphQL support, OpenAPI documentation,
and Aspect-Oriented Programming for monitoring and logging.

## Table of Contents

- [Features](#-features)
- [Quick Start](#-quick-start)
- [Documentation](#-documentation)
- [API Access](#-api-access)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)

## Features

- **Dual API Support** - REST API with OpenAPI 3.0 + GraphQL with schema introspection
- **Interactive Documentation** - Swagger UI for REST, GraphiQL for GraphQL
- **Dual Database** - PostgreSQL (relational) + MongoDB (document storage)
- **AOP Cross-Cutting Concerns** - Logging, performance monitoring, sensitive data masking
- **Full CRUD Operations** - Users, posts, comments, and tags
- **Advanced Search** - Post search with filtering, pagination, and sorting
- **Comprehensive Testing** - 80%+ code coverage with unit and integration tests

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.6+
- PostgreSQL 12+
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

| Interface        | URL                                   | Description                      |
|------------------|---------------------------------------|----------------------------------|
| **Swagger UI**   | http://localhost:8080/swagger-ui.html | REST API documentation & testing |
| **GraphiQL**     | http://localhost:8080/graphiql        | GraphQL query interface          |
| **OpenAPI Spec** | http://localhost:8080/v3/api-docs     | OpenAPI 3.0 specification        |
| **REST API**     | http://localhost:8080/api/v1/*        | RESTful endpoints                |
| **GraphQL API**  | http://localhost:8080/graphql         | GraphQL endpoint                 |

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
- **[Sensitive Data Masking](docs/aop/SENSITIVE_DATA_MASKING.md)** - Security and privacy features
- **[Request Masking Examples](docs/aop/REQUEST_MASKING_EXAMPLES.md)** - Examples of data masking in action

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
  -d '{"title":"My Post","body":"Content...","userId":1,"tags":["tech"]}'

# Get all posts
curl http://localhost:8080/api/v1/posts?page=0&size=10
```

**For complete endpoint documentation, visit [Swagger UI](http://localhost:8080/swagger-ui.html) or
see [OpenAPI Documentation Guide](docs/api/OPENAPI_DOCUMENTATION_GUIDE.md)**

### GraphQL Quick Example

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
    }
}
```

**For more examples, visit [GraphiQL](http://localhost:8080/graphiql) or
see [GraphQL Test Queries](docs/graphql/GRAPHQL_TEST_QUERIES.md)**

## Technology Stack

- **Framework:** Spring Boot 3.5.9, Java 21
- **Databases:** PostgreSQL (relational), MongoDB (document storage)
- **APIs:** Spring Web (REST), Spring GraphQL, SpringDoc OpenAPI (Swagger)
- **AOP:** Spring AOP for logging, performance monitoring, data masking
- **Testing:** JUnit 5, Mockito, Spring Test, H2, JaCoCo (80%+ coverage)
- **Build:** Maven

## Testing

```bash
# Run all tests
mvn test

# Run with coverage report
mvn clean test

# View coverage report
open target/site/jacoco/index.html
```

**Test Coverage:** 80%+ with unit tests, integration tests, and GraphQL tests. See test results in the coverage report.

## Project Structure

```
BloggingPlatform-Spring/
├── src/main/java/org/amalitech/bloggingplatformspring/
│   ├── aop/                    # AOP aspects (logging, performance, masking)
│   ├── config/                 # Configuration (OpenAPI, databases, CORS)
│   ├── controllers/            # REST controllers
│   ├── graphql/                # GraphQL resolvers and types
│   ├── services/               # Business logic
│   ├── dao/                    # Data access layer
│   ├── repository/             # Spring Data repositories
│   ├── entity/                 # JPA entities & MongoDB documents
│   ├── dtos/                   # Request/Response DTOs
│   ├── exceptions/             # Exception handling
│   └── utils/                  # Utility classes
├── src/main/resources/
│   ├── application.properties  # Main configuration
│   ├── application-dev.properties
│   └── graphql/schema.graphqls # GraphQL schema
├── src/test/                   # Comprehensive test suite
├── docs/                       # Documentation
│   ├── api/                    # OpenAPI/REST documentation
│   ├── graphql/                # GraphQL guides and examples
│   └── aop/                    # AOP implementation guides
└── pom.xml                     # Maven configuration
```

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