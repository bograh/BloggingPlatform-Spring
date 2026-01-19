# GraphQL Test Queries
# Copy these queries into GraphiQL interface at http://localhost:8080/graphiql

## Query Examples

### 1. Get All Posts (Simple)
```graphql
query {
  getAllPosts {
    id
    title
    body
  }
}
```

### 2. Get All Posts with Tags
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
  getPaginatedPosts(
    pageRequest: {
      page: 0
      size: 5
      sortBy: "createdAt"
      sortDirection: "DESC"
    }
  ) {
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
    authorUsername
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
  posts: getAllPosts {
    id
    title
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
    id
    username
    email
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
    authorId: "YOUR_USER_ID_HERE"
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
      authorId: "YOUR_USER_ID_HERE"
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
    authorId: "YOUR_USER_ID_HERE"
    commentContent: "Great post!"
  }) {
    id
    authorUsername
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
    authorId: "YOUR_USER_ID_HERE"
  )
}
```

### 7. Delete Comment
```graphql
mutation {
  deleteComment(
    commentId: "COMMENT_ID_HERE"
    input: {
      authorId: "YOUR_USER_ID_HERE"
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

- Replace `YOUR_USER_ID_HERE` with an actual UUID from your database
- Replace `COMMENT_ID_HERE` with an actual comment ID (MongoDB ObjectId)
- All timestamps are in ISO 8601 format
- GraphQL queries are case-sensitive
- Use the GraphiQL autocomplete (Ctrl+Space) for field suggestions

## Common Errors and Solutions

### Error: "User not found"
- Ensure the UUID is valid and exists in the database
- Use the registerUser mutation to create a new user first

### Error: "Post not found"
- Verify the post ID exists
- Use getAllPosts query to see available posts

### Error: "Invalid authorId UUID"
- Check that the authorId is a valid UUID format
- Example valid UUID: `550e8400-e29b-41d4-a716-446655440000`

## REST API Still Works!

All existing REST endpoints continue to function normally:
- `POST /api/users/register`
- `POST /api/users/signin`
- `GET /api/posts`
- `POST /api/posts`
- etc.

Both REST and GraphQL can be used simultaneously!
