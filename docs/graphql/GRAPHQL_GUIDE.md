# GraphQL Integration Guide

## Overview
This project now supports GraphQL alongside the existing REST API endpoints. Both interfaces can be used simultaneously without conflicts.

## Endpoints

- **GraphQL Endpoint**: `http://localhost:8080/graphql`
- **GraphiQL Interface**: `http://localhost:8080/graphiql` (Interactive GraphQL IDE)
- **REST API**: Continues to work on existing endpoints

## Getting Started

### 1. Build and Run the Application

```bash
mvn clean install
mvn spring-boot:run
```

### 2. Access GraphiQL

Open your browser and navigate to: `http://localhost:8080/graphiql`

## GraphQL Schema

The GraphQL API supports the following entities:
- **User**: User accounts and authentication
- **Post**: Blog posts with tags
- **Comment**: Comments on posts
- **Tag**: Post categorization tags

## Example Queries

### Query: Get All Posts
```graphql
query {
  getAllPosts {
    id
    title
    body
    tags {
      name
    }
    updatedAt
  }
}
```

### Query: Get Post by ID with Author
```graphql
query {
  getPost(postId: 1) {
    id
    title
    body
    author {
      id
      username
      email
    }
    tags {
      name
    }
    createdAt
    updatedAt
  }
}
```

### Query: Get Paginated Posts with Filters
```graphql
query {
  getPaginatedPosts(
    pageRequest: {
      page: 0
      size: 10
      sortBy: "createdAt"
      sortDirection: "DESC"
    }
    filter: {
      tag: "technology"
      keyword: "spring"
    }
  ) {
    content {
      id
      title
      body
      tags {
        name
      }
    }
    pageNumber
    pageSize
    totalElements
    totalPages
  }
}
```

### Query: Get Comments for a Post
```graphql
query {
  getCommentsByPost(postId: 1) {
    id
    authorUsername
    content
    createdAt
  }
}
```

### Query: Get User by ID
```graphql
query {
  getUser(userId: "550e8400-e29b-41d4-a716-446655440000") {
    id
    username
    email
    createdAt
  }
}
```

### Query: Get All Tags
```graphql
query {
  getAllTags {
    id
    name
  }
}
```

## Example Mutations

### Mutation: Register New User
```graphql
mutation {
  registerUser(input: {
    username: "johndoe"
    email: "john@example.com"
    password: "SecurePass123!"
  }) {
    id
    username
    email
    createdAt
  }
}
```

### Mutation: Sign In User
```graphql
mutation {
  signInUser(input: {
    email: "john@example.com"
    password: "SecurePass123!"
  }) {
    id
    username
    email
  }
}
```

### Mutation: Create Post
```graphql
mutation {
  createPost(input: {
    title: "Introduction to GraphQL"
    body: "GraphQL is a query language for APIs..."
    authorId: "550e8400-e29b-41d4-a716-446655440000"
    tags: ["graphql", "api", "technology"]
  }) {
    id
    title
    body
    tags {
      name
    }
    updatedAt
  }
}
```

### Mutation: Update Post
```graphql
mutation {
  updatePost(
    postId: 1
    input: {
      title: "Introduction to GraphQL - Updated"
      body: "Updated content..."
      authorId: "550e8400-e29b-41d4-a716-446655440000"
      tags: ["graphql", "api"]
    }
  ) {
    id
    title
    body
    updatedAt
  }
}
```

### Mutation: Delete Post
```graphql
mutation {
  deletePost(
    postId: 1
    authorId: "550e8400-e29b-41d4-a716-446655440000"
  )
}
```

### Mutation: Create Comment
```graphql
mutation {
  createComment(input: {
    postId: 1
    authorId: "550e8400-e29b-41d4-a716-446655440000"
    commentContent: "Great article!"
  }) {
    id
    authorUsername
    content
    createdAt
  }
}
```

### Mutation: Delete Comment
```graphql
mutation {
  deleteComment(
    commentId: "507f1f77bcf86cd799439011"
    input: {
      authorId: "550e8400-e29b-41d4-a716-446655440000"
    }
  )
}
```

## Advanced Queries

### Nested Query: Post with Author and Comments
```graphql
query {
  getPost(postId: 1) {
    id
    title
    body
    author {
      username
      email
    }
    tags {
      name
    }
  }
  getCommentsByPost(postId: 1) {
    authorUsername
    content
    createdAt
  }
}
```

### Multiple Queries in One Request
```graphql
query {
  allPosts: getAllPosts {
    id
    title
  }
  allTags: getAllTags {
    id
    name
  }
}
```

## Schema Introspection

You can explore the schema using introspection queries:

```graphql
{
  __schema {
    types {
      name
      description
    }
  }
}
```

Or query specific types:

```graphql
{
  __type(name: "Post") {
    name
    fields {
      name
      type {
        name
      }
    }
  }
}
```

## Error Handling

GraphQL returns structured error responses:

```json
{
  "errors": [
    {
      "message": "User not found",
      "locations": [{"line": 2, "column": 3}],
      "path": ["getUser"]
    }
  ],
  "data": {
    "getUser": null
  }
}
```

## Custom Scalars

The API uses custom scalar types:

- **UUID**: For user IDs (e.g., `550e8400-e29b-41d4-a716-446655440000`)
- **DateTime**: ISO 8601 formatted timestamps (e.g., `2026-01-19T10:30:00`)

## REST vs GraphQL Comparison

| Feature | REST API | GraphQL API |
|---------|----------|-------------|
| Endpoint | Multiple endpoints | Single endpoint (/graphql) |
| Data Fetching | Fixed response structure | Request only needed fields |
| Over-fetching | May return unnecessary data | No over-fetching |
| Under-fetching | May need multiple requests | Single request for nested data |
| Versioning | URL or header versioning | Schema evolution |

## Testing with cURL

### GraphQL Query Example
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ getAllPosts { id title body } }"
  }'
```

### GraphQL Mutation Example
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { registerUser(input: { username: \"testuser\", email: \"test@example.com\", password: \"Password123!\" }) { id username } }"
  }'
```

## Configuration

GraphQL configuration is defined in `application.properties`:

```properties
# GraphQL Configuration
spring.graphql.graphiql.enabled=true
spring.graphql.graphiql.path=/graphiql
spring.graphql.path=/graphql
spring.graphql.schema.printer.enabled=true
spring.graphql.schema.locations=classpath:graphql/
```

## Coexistence with REST API

Both APIs work simultaneously:

- **REST**: All existing REST endpoints remain functional
- **GraphQL**: New `/graphql` endpoint added
- **No Conflicts**: Both share the same service layer
- **Same Business Logic**: Both interfaces use identical services and repositories

## Development Tools

### GraphiQL Features
- Auto-completion
- Schema documentation
- Query history
- Syntax highlighting
- Error validation

### Alternative Clients
- **Altair GraphQL Client**: Browser extension or desktop app
- **Postman**: Supports GraphQL requests
- **Apollo Studio**: Cloud-based GraphQL IDE
- **Insomnia**: REST and GraphQL client

## Troubleshooting

### GraphiQL not loading
- Ensure `spring.graphql.graphiql.enabled=true`
- Check application is running on correct port
- Verify no CORS issues

### Schema not found
- Ensure schema file is in `src/main/resources/graphql/schema.graphqls`
- Check `spring.graphql.schema.locations` property

### Query returns null
- Check if data exists in database
- Verify UUIDs and IDs are correct format
- Check service layer for exceptions

## Performance Considerations

- **Batching**: Consider implementing DataLoader for N+1 query optimization
- **Query Complexity**: Monitor and limit query depth and complexity
- **Caching**: Implement caching strategies for frequently accessed data
- **Pagination**: Use paginated queries for large datasets

## Security

- GraphQL inherits existing security configurations
- Consider adding query complexity limits
- Implement proper authorization checks in resolvers
- Monitor for introspection queries in production

## Next Steps

1. Test all queries and mutations in GraphiQL
2. Implement DataLoader for batch loading (optional optimization)
3. Add authentication/authorization to GraphQL endpoints
4. Set up query complexity analysis
5. Add GraphQL-specific integration tests

## Resources

- [Spring for GraphQL Documentation](https://docs.spring.io/spring-graphql/docs/current/reference/html/)
- [GraphQL Official Documentation](https://graphql.org/learn/)
- [GraphQL Best Practices](https://graphql.org/learn/best-practices/)
