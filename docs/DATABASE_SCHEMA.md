# Database Schema & Entity Relationships

Comprehensive database schema documentation for the Blogging Platform including entity relationships, indexes, and constraints.

## Table of Contents

- [Overview](#overview)
- [Entity Relationship Diagram](#entity-relationship-diagram)
- [Database Architecture](#database-architecture)
- [PostgreSQL Schema](#postgresql-schema)
- [MongoDB Schema](#mongodb-schema)
- [Relationships](#relationships)
- [Indexes & Performance](#indexes--performance)
- [Data Flow](#data-flow)

## Overview

The Blogging Platform uses a **hybrid database architecture**:

- **PostgreSQL**: Stores structured relational data (Users, Posts, Tags)
- **MongoDB**: Stores flexible document data (Comments)

This approach provides:
- ACID compliance for critical data
- Flexibility for schema-less comment structures
- Optimal performance for different data access patterns

## Entity Relationship Diagram

### Complete ER Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              PostgreSQL Database                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘

     ┌──────────────────────┐
     │        User          │
     ├──────────────────────┤
     │ PK  id (UUID)        │◄──────────────┐
     │     username (UK)    │               │
     │     email (UK)       │               │  1:N Relationship
     │     password         │               │  (author → posts)
     │     created_at       │               │
     └──────────────────────┘               │
               │                            │
               │ References                 │
               │ (virtual)                  │
               ▼                            │
                                    ┌───────┴──────────┐
                                    │      Post        │
     ┌──────────────────────┐       ├──────────────────┤
     │      Comment         │       │ PK  id (BIGINT)  │
     │    (MongoDB)         │       │     title        │
     ├──────────────────────┤       │     body (TEXT)  │◄─────┐
     │ PK  _id (String)     │       │ FK  author_id    │      │
     │     post_id          │──────►│     posted_at    │      │
     │     author_id        │─ ─ ─ ▶│     updated_at   │      │
     │     author           │       └──────────┬───────┘      │
     │     content          │                  │              │
     │     commented_at     │                  │              │
     └──────────────────────┘                  │              │
               ▲                               │              │
               │                               │ M:N          │
               │                               │ (via         │
               │                               │ post_tags)   │
               │                               │              │
               │                               ▼              │
               │                      ┌────────────────┐      │
               │                      │   post_tags    │      │
               │                      │  (Join Table)  │      │
               │                      ├────────────────┤      │
               │                      │ FK  post_id    │──────┘
               │                      │ FK  tag_id     │────┐
               │                      └────────────────┘    │
               │                                            │
               │ Ref (comments.author_id → users.id)       │
               │ [Enforced in application logic]           │
               └───────────────────────────────────────┐    │
                                                       │    │
                                                       │    │
                                                       │    ▼
                                                ┌──────┴────────┐
                                                │      Tag      │
                                                ├───────────────┤
                                                │ PK  id        │
                                                │     name (UK) │
                                                └───────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                MongoDB Database                                      │
└─────────────────────────────────────────────────────────────────────────────────────┘

                           ┌──────────────────────┐
                           │      comments        │
                           │    (Collection)      │
                           ├──────────────────────┤
                           │ _id                  │
                           │ post_id              │
                           │ author_id            │
                           │ author               │
                           │ content              │
                           │ commented_at         │
                           └──────────────────────┘
```

### Relationship Cardinality

```
User ────┬──── 1:N ────┬──── Post
         │              │
         │              └──── M:N (via post_tags) ──── Tag
         │
         └──── 1:N (virtual) ──── Comment
```

## Database Architecture

### Design Decisions

| Entity | Database | Reason |
|--------|----------|--------|
| User | PostgreSQL | Requires ACID, relationships with Posts |
| Post | PostgreSQL | Complex relationships (User, Tags), transactions |
| Tag | PostgreSQL | Many-to-many with Posts, referential integrity |
| Comment | MongoDB | Flexible schema, high write volume, denormalized |

### Hybrid Benefits

1. **PostgreSQL Strengths**:
   - Complex joins (User ↔ Post ↔ Tag)
   - Referential integrity
   - ACID transactions
   - Structured query optimization

2. **MongoDB Strengths**:
   - Flexible comment schema
   - High write throughput
   - Denormalized data (author name cached)
   - Horizontal scaling potential

## PostgreSQL Schema

### Users Table

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_username UNIQUE (username),
    CONSTRAINT uk_email UNIQUE (email)
);

CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_created_at ON users(created_at);
```

**Columns**:
- `id`: Primary key, UUID for global uniqueness
- `username`: Unique username, indexed for fast lookups
- `email`: Unique email, indexed for authentication
- `password`: Bcrypt-hashed password
- `created_at`: Account creation timestamp

**Indexes**:
- `idx_username`: Fast username lookups
- `idx_email`: Authentication queries
- `idx_created_at`: User timeline queries

### Posts Table

```sql
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    author_id UUID NOT NULL,
    posted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_author
        FOREIGN KEY (author_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_author_id ON posts(author_id);
CREATE INDEX idx_posted_at ON posts(posted_at);
CREATE INDEX idx_author_posted ON posts(author_id, posted_at);
```

**Columns**:
- `id`: Primary key, auto-incrementing
- `title`: Post title
- `body`: Post content (TEXT for long content)
- `author_id`: Foreign key to users table
- `posted_at`: Creation timestamp
- `updated_at`: Last modification timestamp

**Indexes**:
- `idx_author_id`: Find posts by author
- `idx_posted_at`: Chronological sorting
- `idx_author_posted`: Composite index for author's posts by date

**Triggers**:
```java
@PrePersist
protected void onCreate() {
    postedAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
}

@PreUpdate
protected void onUpdate() {
    updatedAt = LocalDateTime.now();
}
```

### Tags Table

```sql
CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,

    CONSTRAINT uk_name UNIQUE (name)
);

CREATE INDEX idx_name ON tags(name);
```

**Columns**:
- `id`: Primary key, auto-incrementing
- `name`: Tag name, unique across system

**Indexes**:
- `idx_name`: Fast tag lookups by name

### Post_Tags Join Table

```sql
CREATE TABLE post_tags (
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,

    PRIMARY KEY (post_id, tag_id),

    CONSTRAINT fk_post
        FOREIGN KEY (post_id)
        REFERENCES posts(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_tag
        FOREIGN KEY (tag_id)
        REFERENCES tags(id)
        ON DELETE CASCADE
);
```

**Columns**:
- `post_id`: Foreign key to posts
- `tag_id`: Foreign key to tags

**Composite Primary Key**: Prevents duplicate tag assignments

**Cascade Behavior**:
- Deleting a post removes all its tag associations
- Deleting a tag removes all post associations

## MongoDB Schema

### Comments Collection

```javascript
{
  _id: ObjectId("507f1f77bcf86cd799439011"),
  post_id: 123,                          // References posts.id (PostgreSQL)
  author_id: "550e8400-e29b-41d4-a716-446655440000", // References users.id (PostgreSQL)
  author: "john_doe",                     // Denormalized username
  content: "This is a great post!",
  commented_at: ISODate("2026-02-02T10:30:00Z")
}
```

**Fields**:
- `_id`: MongoDB ObjectId (auto-generated)
- `post_id`: Reference to PostgreSQL posts.id
- `author_id`: Reference to PostgreSQL users.id (UUID string)
- `author`: Denormalized username (cached for performance)
- `content`: Comment text
- `commented_at`: Timestamp

**Indexes**:
```javascript
db.comments.createIndex({ "post_id": 1 })
db.comments.createIndex({ "author_id": 1 })
db.comments.createIndex({ "commented_at": -1 })
```

**Denormalization Rationale**:
- `author` field duplicates username for performance
- Avoids cross-database joins
- Acceptable trade-off for read-heavy workload

## Relationships

### 1. User → Post (One-to-Many)

**Type**: One-to-Many
**Implementation**: Foreign Key
**Cascade**: ON DELETE CASCADE

```java
// User entity
@OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<Post> posts = new HashSet<>();

// Post entity
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "fk_author"))
private User author;
```

**Behavior**:
- One user can create many posts
- Deleting a user deletes all their posts
- Lazy loading to avoid N+1 queries

### 2. Post → Tag (Many-to-Many)

**Type**: Many-to-Many
**Implementation**: Join Table (post_tags)
**Cascade**: PERSIST, MERGE

```java
// Post entity
@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
@JoinTable(
    name = "post_tags",
    joinColumns = @JoinColumn(name = "post_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id"),
    foreignKey = @ForeignKey(name = "fk_post"),
    inverseForeignKey = @ForeignKey(name = "fk_tag")
)
private Set<Tag> tags = new HashSet<>();

// Tag entity
@ManyToMany(mappedBy = "tags")
private Set<Post> posts = new HashSet<>();
```

**Behavior**:
- A post can have multiple tags
- A tag can belong to multiple posts
- No cascade delete (tags persist when posts are deleted)

### 3. Post → Comment (One-to-Many, Virtual)

**Type**: One-to-Many (Virtual)
**Implementation**: Application-level reference
**Database**: Cross-database (PostgreSQL → MongoDB)

```java
// CommentService
public List<Comment> getCommentsByPostId(Long postId) {
    return commentRepository.findByPostId(postId);
}
```

**Behavior**:
- Relationship maintained in application code
- No database-level foreign key
- MongoDB query filters by post_id

### 4. User → Comment (One-to-Many, Virtual)

**Type**: One-to-Many (Virtual)
**Implementation**: Application-level reference
**Database**: Cross-database (PostgreSQL → MongoDB)

```java
// Comment entity (MongoDB)
@Field("author_id")
private String authorId;  // Stores user UUID as string

@Field("author")
private String author;     // Denormalized username
```

**Behavior**:
- User ID stored as string in MongoDB
- Username denormalized for performance
- Application enforces referential integrity

## Indexes & Performance

### PostgreSQL Indexes

| Table | Index | Columns | Purpose |
|-------|-------|---------|---------|
| users | PRIMARY KEY | id | Primary key lookups |
| users | idx_username | username | Username searches |
| users | idx_email | email | Authentication queries |
| users | idx_created_at | created_at | User timeline |
| posts | PRIMARY KEY | id | Primary key lookups |
| posts | idx_author_id | author_id | Posts by author |
| posts | idx_posted_at | posted_at | Chronological sorting |
| posts | idx_author_posted | author_id, posted_at | Composite: author's posts by date |
| tags | PRIMARY KEY | id | Primary key lookups |
| tags | idx_name | name | Tag name searches |
| post_tags | PRIMARY KEY | post_id, tag_id | Composite primary key |

### MongoDB Indexes

```javascript
// Comments collection indexes
db.comments.createIndex({ "post_id": 1 })      // Find comments by post
db.comments.createIndex({ "author_id": 1 })    // Find comments by author
db.comments.createIndex({ "commented_at": -1 }) // Sort by date (descending)
```

### Query Optimization

**Efficient Queries**:

```sql
-- Get user's posts with pagination (uses idx_author_posted)
SELECT * FROM posts
WHERE author_id = ?
ORDER BY posted_at DESC
LIMIT 10 OFFSET 0;

-- Get posts with specific tag (uses indexes + join table)
SELECT p.* FROM posts p
JOIN post_tags pt ON p.id = pt.post_id
JOIN tags t ON pt.tag_id = t.id
WHERE t.name = 'spring-boot';

-- Get comments for a post (MongoDB)
db.comments.find({ post_id: 123 }).sort({ commented_at: -1 })
```

## Data Flow

### Create Post Flow

```
1. User authenticated
2. PostService.createPost(CreatePostDTO)
3. ├─► Validate user exists (PostgreSQL)
4. ├─► Process tags
5. │   ├─► Find existing tags
6. │   └─► Create new tags if needed
7. ├─► Create Post entity
8. ├─► Associate tags (post_tags)
9. └─► Save to PostgreSQL
10. Cache eviction (allPosts cache)
11. Return GetPostDTO
```

### Add Comment Flow

```
1. User provides post_id, author_id, content
2. CommentService.createComment(CreateCommentDTO)
3. ├─► Validate post exists (PostgreSQL)
4. ├─► Validate user exists (PostgreSQL)
5. ├─► Get username for denormalization
6. ├─► Create Comment document
7. └─► Save to MongoDB
8. Cache eviction (comments cache, user cache)
9. Return Comment
```

### Get Post with Details Flow

```
1. Request post ID
2. Check cache (posts)
3. If miss:
4.   ├─► Query posts table (PostgreSQL)
5.   ├─► LEFT JOIN post_tags
6.   ├─► LEFT JOIN tags
7.   ├─► LEFT JOIN users (author)
8.   ├─► Query comments (MongoDB)
9.   └─► Assemble GetPostDTO
10. Store in cache
11. Return result
```

## Best Practices

### 1. Use Appropriate Fetch Types

```java
// Lazy loading for collections
@OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
private Set<Post> posts;

// Eager only when always needed
@ManyToOne(fetch = FetchType.LAZY)
private User author;
```

### 2. Leverage Indexes

```java
// Define indexes in entities
@Table(name = "posts", indexes = {
    @Index(name = "idx_author_id", columnList = "author_id"),
    @Index(name = "idx_posted_at", columnList = "posted_at")
})
```

### 3. Use Pagination

```java
PageRequest pageRequest = PageRequest.of(page, size,
    Sort.by(Sort.Direction.fromString(order), sortBy));
Page<Post> posts = postRepository.findAll(pageRequest);
```

### 4. Implement Caching Strategically

- Cache frequently accessed entities (users, posts)
- Evict caches on updates
- Use cache keys that match query patterns

### 5. Monitor Query Performance

```java
// Enable query logging
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.data.mongodb.logging.level=DEBUG
```

## Migration & Schema Changes

### Adding a Column

```sql
ALTER TABLE posts ADD COLUMN view_count BIGINT DEFAULT 0;
CREATE INDEX idx_view_count ON posts(view_count);
```

### Adding a Relationship

```sql
CREATE TABLE likes (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    post_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_post FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT uk_user_post UNIQUE (user_id, post_id)
);
```

## Summary

The hybrid database architecture provides:

✅ **Strong consistency** for critical data (Users, Posts, Tags)
✅ **Flexibility** for evolving schemas (Comments)
✅ **Performance** through strategic indexing and caching
✅ **Scalability** with MongoDB for high-write workloads
✅ **Data integrity** through foreign keys and application logic

For more information:
- [Cache Monitoring Guide](CACHE_MONITORING_GUIDE.md)
- [Performance Metrics Guide](PERFORMANCE_METRICS_GUIDE.md)
- [API Documentation](../api/OPENAPI_DOCUMENTATION_GUIDE.md)
