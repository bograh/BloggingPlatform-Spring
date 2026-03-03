# GraphQL Test Queries
# Copy these queries into GraphiQL interface at http://localhost:8080/graphiql

## Query Examples

### 1. Get All Posts (Simple)
```graphql
query {
  getAllPosts(page: 0, size: 10, sortBy: "updatedAt", sortDirection: "desc") {
    content {
      id
      title
      body
    }
  }
}
```

### 2. Get All Posts with Tags
```graphql
query {
  getAllPosts(page: 0, size: 10, sortBy: "updatedAt", sortDirection: "desc") {
    content {
      id
      title
      body
      tags {
        name
      }
      updatedAt
    }
  }
}
```

### 3. Get Specific Post with Author
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
}
```

### 4. Get Paginated Posts
```graphql
query {
  getAllPosts(page: 0, size: 5, sortBy: "updatedAt", sortDirection: "desc") {
    content {
      id
      title
      body
    }
    pageNumber
    pageSize
    totalElements
    totalPages
  }
}
```

### 5. Get Comments for a Post
```graphql
query {
  getCommentsByPost(postId: 1) {
    id
    author
    content
    createdAt
  }
}
```

### 6. Get All Tags
```graphql
query {
  getAllTags {
    id
    name
  }
}
```

### 7. Combined Query (Multiple Operations)
```graphql
query {
  posts: getAllPosts(page: 0, size: 5) {
    content {
      id
      title
    }
  }
  tags: getAllTags {
    name
  }
}
```

## Mutation Examples

### 1. Register New User
```graphql
mutation {
  registerUser(input: {
    username: "testuser"
    email: "test@example.com"
    password: "Password123!"
  }) {
    token
    user {
      id
      username
      email
    }
  }
}
```

### 2. Sign In User
```graphql
mutation {
  signInUser(input: {
    email: "test@example.com"
    password: "Password123!"
  }) {
    id
    username
    email
  }
}
```

### 3. Create Post
```graphql
mutation {
  createPost(input: {
    title: "My First GraphQL Post"
    body: "This is a test post created via GraphQL"
    tags: ["graphql", "testing"]
  }) {
    id
    title
    body
    tags {
      name
    }
  }
}
```

### 4. Update Post
```graphql
mutation {
  updatePost(
    postId: 1
    input: {
      title: "Updated Title"
      body: "Updated content"
      tags: ["updated", "graphql"]
    }
  ) {
    id
    title
    body
    updatedAt
  }
}
```

### 5. Create Comment
```graphql
mutation {
  createComment(input: {
    postId: 1
    commentContent: "Great post!"
  }) {
    id
    author
    content
    createdAt
  }
}
```

### 6. Delete Post
```graphql
mutation {
  deletePost(
    postId: 1
  )
}
```

### 7. Delete Comment
```graphql
mutation {
  deleteComment(
    commentId: "COMMENT_ID_HERE"
    input: {
      postId: 1
    }
  )
}
```

## Testing Instructions

1. **Start the Application**:
   ```bash
   mvn spring-boot:run
   ```

2. **Open GraphiQL**:
   Navigate to: http://localhost:8080/graphiql

3. **Test Schema**:
   Click on "Docs" in GraphiQL to explore the full schema

4. **Run Introspection Query**:
   ```graphql
   {
     __schema {
       types {
         name
       }
     }
   }
   ```

5. **Test Query**:
   - Copy any query from above
   - Paste into GraphiQL editor
   - Click the Play button
   - View results in the right panel

## Notes

- Replace `COMMENT_ID_HERE` with an actual comment ID (MongoDB ObjectId)
- All timestamps are in ISO 8601 format
- GraphQL queries are case-sensitive
- Use the GraphiQL autocomplete (Ctrl+Space) for field suggestions

## Common Errors and Solutions

### Error: "User not found"
- Ensure the user exists in the application
- Use the registerUser mutation to create a new user first

### Error: "Post not found"
- Verify the post ID exists
- Use getAllPosts query to see available posts

### Error: "Unauthorized"
- Provide a valid JWT access token for protected mutations
- Sign in via REST (`POST /api/auth/sign-in`) and send `Authorization: Bearer <token>`

## REST API Still Works!

All existing REST endpoints continue to function normally:
- `POST /api/auth/register`
- `POST /api/auth/sign-in`
- `GET /api/posts`
- `POST /api/posts/old`
- etc.

Both REST and GraphQL can be used simultaneously!
