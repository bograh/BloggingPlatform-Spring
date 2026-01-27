# OpenAPI/Swagger Documentation Guide

## Overview

This blogging platform now includes comprehensive OpenAPI 3.0 documentation with an interactive Swagger UI interface.
The API documentation is automatically generated from annotated controllers and provides a complete reference for all
available endpoints.

## Access Points

### Swagger UI (Interactive Documentation)

- **URL**: `http://localhost:8080/swagger-ui.html`
- **Description**: Interactive web interface to explore and test API endpoints
- **Features**:
    - Browse all API endpoints organized by tags
    - View detailed request/response schemas
    - Test endpoints directly from the browser
    - View example values for request bodies
    - See all possible response codes and descriptions

### OpenAPI Specification (JSON)

- **URL**: `http://localhost:8080/v3/api-docs`
- **Description**: Machine-readable OpenAPI 3.0 specification in JSON format
- **Use Cases**:
    - Import into API testing tools (Postman, Insomnia)
    - Generate client SDKs
    - API contract validation
    - CI/CD integration

### OpenAPI Specification (YAML)

- **URL**: `http://localhost:8080/v3/api-docs.yaml`
- **Description**: OpenAPI 3.0 specification in YAML format
- **Use Cases**: Same as JSON, preferred by some tools and easier to read

## API Structure

### Endpoints by Category

#### 1. User Management

**Tag**: User Management
**Base Path**: `/api/v1/users`

| Method | Endpoint    | Description                        |
|--------|-------------|------------------------------------|
| POST   | `/register` | Register a new user account        |
| POST   | `/sign-in`  | Authenticate user with credentials |

#### 2. Post Management

**Tag**: Post Management
**Base Path**: `/api/v1/posts`

| Method | Endpoint    | Description                                 |
|--------|-------------|---------------------------------------------|
| POST   | `/`         | Create a new blog post                      |
| GET    | `/`         | Get all posts with pagination and filtering |
| GET    | `/{postId}` | Get a specific post by ID                   |
| PUT    | `/{postId}` | Update an existing post                     |
| DELETE | `/{postId}` | Delete a post                               |

**Query Parameters for GET /**:

- `page`: Page number (0-indexed, default: 0)
- `size`: Page size (max 50, default: 10)
- `sort`: Sort field (id, createdAt, lastUpdated, title)
- `order`: Sort direction (ASC or DESC, default: DESC)
- `author`: Filter by author name
- `tags`: Filter by tag names (comma-separated)
- `search`: Search in title and content

#### 3. Comment Management

**Tag**: Comment Management
**Base Path**: `/api/v1/comments`

| Method | Endpoint         | Description                  |
|--------|------------------|------------------------------|
| POST   | `/`              | Add a comment to a post      |
| GET    | `/post/{postId}` | Get all comments for a post  |
| GET    | `/{commentId}`   | Get a specific comment by ID |
| DELETE | `/{commentId}`   | Delete a comment             |

**Note**: Comments are stored in MongoDB and use ObjectId format for IDs.

#### 4. Performance Metrics

**Tag**: Performance Metrics
**Base Path**: `/api/metrics/performance`

| Method | Endpoint                | Description                                       |
|--------|-------------------------|---------------------------------------------------|
| GET    | `/`                     | Get all performance metrics                       |
| GET    | `/{layer}/{methodName}` | Get metrics for a specific method                 |
| GET    | `/summary`              | Get aggregated metrics summary                    |
| GET    | `/slow`                 | Get methods exceeding threshold (default: 1000ms) |
| GET    | `/top`                  | Get top N slowest methods (default: 10)           |
| GET    | `/layer/{layer}`        | Get metrics by layer (SERVICE/REPOSITORY)         |
| GET    | `/failures`             | Get failure statistics                            |
| DELETE | `/reset`                | Reset all metrics                                 |
| POST   | `/export-log`           | Export metrics to log file                        |

## Using Swagger UI

### 1. Accessing Swagger UI

1. Start your application
2. Navigate to `http://localhost:8080/swagger-ui.html`
3. You'll see all API endpoints grouped by tags

### 2. Testing Endpoints

#### Example: Creating a User

1. Expand the **User Management** section
2. Click on `POST /api/v1/users/register`
3. Click the **Try it out** button
4. Edit the request body JSON:

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "SecurePass123!"
}
```

5. Click **Execute**
6. View the response below, including status code and response body

#### Example: Getting Posts with Filters

1. Expand the **Post Management** section
2. Click on `GET /api/v1/posts`
3. Click **Try it out**
4. Fill in optional parameters:
    - `page`: 0
    - `size`: 10
    - `sort`: lastUpdated
    - `order`: DESC
    - `author`: John Doe
    - `tags`: technology, programming
5. Click **Execute**
6. Review the paginated response

### 3. Understanding Responses

Each endpoint documentation shows:

- **Request Body**: Expected JSON structure with field descriptions
- **Parameters**: Query, path, and header parameters
- **Responses**: All possible HTTP status codes
    - 2xx: Success responses
    - 4xx: Client errors (bad request, not found, unauthorized, etc.)
    - 5xx: Server errors

Example response codes:

- `200 OK`: Successful GET/PUT operation
- `201 Created`: Successful POST operation
- `204 No Content`: Successful DELETE operation
- `400 Bad Request`: Invalid input data
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Not authorized for this operation
- `404 Not Found`: Resource doesn't exist

## API Features

### 1. Pagination

Most list endpoints support pagination:

```
GET /api/v1/posts?page=0&size=10&sort=lastUpdated&order=DESC
```

Response includes:

```json
{
  "status": "success",
  "message": "Posts retrieved successfully",
  "data": {
    "content": [...],
    "page": 0,
    "size": 10,
    "totalElements": 45,
    "totalPages": 5,
    "isFirst": true,
    "isLast": false
  }
}
```

### 2. Filtering and Search

Posts can be filtered by:

- **Author**: `?author=John Doe`
- **Tags**: `?tags=java,spring,backend`
- **Search**: `?search=spring boot tutorial`

Combine filters:

```
GET /api/v1/posts?author=John&tags=java,spring&search=tutorial&page=0&size=10
```

### 3. Error Handling

All endpoints return consistent error responses:

```json
{
  "status": "error",
  "message": "Detailed error message",
  "timestamp": "2026-01-20T10:30:00Z"
}
```

## Integration with API Clients

### Postman

1. Open Postman
2. Click **Import**
3. Enter URL: `http://localhost:8080/v3/api-docs`
4. Postman will create a collection with all endpoints

### Insomnia

1. Open Insomnia
2. Create new **Design Document**
3. Import from URL: `http://localhost:8080/v3/api-docs`
4. All endpoints will be available for testing

### cURL Examples

#### Register User

```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'
```

#### Get Posts

```bash
curl -X GET "http://localhost:8080/api/v1/posts?page=0&size=10&sort=lastUpdated&order=DESC"
```

#### Create Post

```bash
curl -X POST http://localhost:8080/api/v1/posts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Getting Started with Spring Boot",
    "content": "This is a comprehensive guide...",
    "authorId": "user123",
    "tags": ["spring", "java", "backend"]
  }'
```

#### Add Comment

```bash
curl -X POST http://localhost:8080/api/v1/comments \
  -H "Content-Type: application/json" \
  -d '{
    "postId": 1,
    "userId": "user123",
    "content": "Great article!"
  }'
```

## Configuration

OpenAPI/Swagger is configured in:

### application.properties

```properties
# OpenAPI/Swagger Configuration
springdoc.api-docs.path=/v3/api-docs
springdoc.api-docs.enabled=true
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.try-it-out-enabled=true
springdoc.swagger-ui.filter=true
springdoc.swagger-ui.syntaxHighlight.activated=true
springdoc.show-actuator=false
springdoc.swagger-ui.displayRequestDuration=true
springdoc.swagger-ui.defaultModelsExpandDepth=1
springdoc.swagger-ui.defaultModelExpandDepth=1
```

### OpenApiConfig.java

The configuration class defines:

- API title, version, and description
- Contact information
- License information
- Server URLs
- Global API documentation

## Best Practices

### 1. Keep Documentation Updated

- Update `@Operation` descriptions when modifying endpoints
- Add new `@ApiResponse` entries for new error cases
- Update example values in `@Parameter` annotations

### 2. Use Descriptive Summaries

- Summaries should be concise (5-10 words)
- Descriptions should explain the purpose and behavior
- Include important details about authorization and validation

### 3. Document All Response Codes

- Include all possible HTTP status codes
- Provide clear descriptions for each code
- Reference the error response schema

### 4. Provide Examples

- Use `@Parameter` examples for path and query parameters
- Provide realistic example values
- Show both success and error response examples

### 5. Organize with Tags

- Group related endpoints together
- Use clear, descriptive tag names
- Keep the number of tags manageable

## Troubleshooting

### Swagger UI Not Loading

**Issue**: Cannot access `http://localhost:8080/swagger-ui.html`

**Solutions**:

1. Verify application is running: `curl http://localhost:8080/actuator/health`
2. Check if springdoc dependency is in pom.xml
3. Verify `springdoc.swagger-ui.enabled=true` in properties
4. Check application logs for errors
5. Try alternative URL: `http://localhost:8080/swagger-ui/index.html`

### API Docs Not Generating

**Issue**: Endpoints missing from Swagger UI

**Solutions**:

1. Ensure controllers have `@RestController` annotation
2. Verify `@RequestMapping` is present on controller class
3. Check that methods have HTTP method annotations (`@GetMapping`, etc.)
4. Verify `@Tag` annotation is present on controller
5. Check that controller package is scanned by Spring Boot

### Invalid OpenAPI Spec

**Issue**: OpenAPI validation errors

**Solutions**:

1. Ensure all `@Operation` annotations have summary
2. Verify `@Parameter` descriptions are present
3. Check that response schemas reference existing DTOs
4. Validate JSON structure in `@Schema` annotations

## Additional Resources

### GraphQL Alternative

This API also supports GraphQL:

- **GraphiQL UI**: `http://localhost:8080/graphiql`
- **GraphQL Endpoint**: `http://localhost:8080/graphql`
- **Documentation**: See [docs/graphql/README_GRAPHQL.md](../graphql/README_GRAPHQL.md)

### Performance Monitoring

Monitor API performance using:

- **Metrics Endpoint**: `/api/metrics/performance`
- **Actuator**: `/actuator/metrics`
- **Documentation**: See [docs/aop/PERFORMANCE_METRICS_GUIDE.md](../aop/PERFORMANCE_METRICS_GUIDE.md)

### AOP Features

The platform includes:

- Automatic request/response logging
- Performance monitoring
- Sensitive data masking
- **Documentation**: See [docs/aop/AOP_IMPLEMENTATION_GUIDE.md](../aop/AOP_IMPLEMENTATION_GUIDE.md)

## Support

For issues or questions:

- Check application logs in `logs/blogging-platform.log`
- Review error responses for detailed messages
- Consult Spring Boot documentation
- Review Springdoc OpenAPI documentation: https://springdoc.org/

---

**Last Updated**: January 20, 2026
**Version**: 1.0.0
**Springdoc Version**: 2.3.0
**OpenAPI Version**: 3.0