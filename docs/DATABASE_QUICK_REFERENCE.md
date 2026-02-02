# Database Quick Reference

Quick reference card for the Blogging Platform database schema.

## Tables Overview

### PostgreSQL Tables

| Table | Purpose | Primary Key | Foreign Keys |
|-------|---------|-------------|--------------|
| **users** | User accounts | id (UUID) | - |
| **posts** | Blog posts | id (BIGINT) | author_id → users(id) |
| **tags** | Post tags | id (BIGINT) | - |
| **post_tags** | Post-Tag mapping | (post_id, tag_id) | post_id → posts(id)<br>tag_id → tags(id) |

### MongoDB Collections

| Collection | Purpose | Primary Key | References |
|------------|---------|-------------|------------|
| **comments** | Post comments | _id (ObjectId) | post_id → posts(id)<br>author_id → users(id) |

## Entity Relationships

```
User (1) ──────< (N) Post (M) ──────< (N) Tag
  │                    │
  │                    │
  └──< (N) Comment     └──< (N) Comment
    (virtual)           (virtual)
```

## Column Reference

### users

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | UUID | NO | gen_random_uuid() | Primary key |
| username | VARCHAR(255) | NO | - | Unique username |
| email | VARCHAR(255) | NO | - | Unique email |
| password | VARCHAR(255) | NO | - | Bcrypt hash |
| created_at | TIMESTAMP | NO | CURRENT_TIMESTAMP | Creation time |

**Indexes**: `idx_username`, `idx_email`, `idx_created_at`

### posts

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | BIGSERIAL | NO | auto | Primary key |
| title | VARCHAR(255) | NO | - | Post title |
| body | TEXT | NO | - | Post content |
| author_id | UUID | NO | - | FK to users(id) |
| posted_at | TIMESTAMP | NO | CURRENT_TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | NO | CURRENT_TIMESTAMP | Last update |

**Indexes**: `idx_author_id`, `idx_posted_at`, `idx_author_posted`

### tags

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | BIGSERIAL | NO | auto | Primary key |
| name | VARCHAR(100) | NO | - | Unique tag name |

**Indexes**: `idx_name`

### post_tags

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| post_id | BIGINT | NO | - | FK to posts(id) |
| tag_id | BIGINT | NO | - | FK to tags(id) |

**Composite PK**: (post_id, tag_id)

### comments (MongoDB)

| Field | Type | Description |
|-------|------|-------------|
| _id | ObjectId | MongoDB auto-generated |
| post_id | Long | References posts.id |
| author_id | String | References users.id (UUID) |
| author | String | Denormalized username |
| content | String | Comment text |
| commented_at | ISODate | Creation timestamp |

**Indexes**: `post_id`, `author_id`, `commented_at`

## Common Queries

### Get User's Posts
```sql
SELECT * FROM posts
WHERE author_id = ?
ORDER BY posted_at DESC;
```

### Get Post with Tags
```sql
SELECT p.*, t.name as tag_name
FROM posts p
LEFT JOIN post_tags pt ON p.id = pt.post_id
LEFT JOIN tags t ON pt.tag_id = t.id
WHERE p.id = ?;
```

### Get Comments for Post
```javascript
db.comments.find({ post_id: 123 }).sort({ commented_at: -1 })
```

### Get Popular Tags
```sql
SELECT t.name, COUNT(pt.post_id) as usage_count
FROM tags t
JOIN post_tags pt ON t.id = pt.tag_id
GROUP BY t.id, t.name
ORDER BY usage_count DESC
LIMIT 10;
```

## Cascade Behaviors

| Parent | Child | On Delete |
|--------|-------|-----------|
| users | posts | CASCADE (posts deleted) |
| posts | post_tags | CASCADE (associations deleted) |
| posts | comments | Application-level cleanup |
| tags | post_tags | CASCADE (associations deleted) |

## Connection Strings

### PostgreSQL
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/blogging_db
spring.datasource.username=postgres
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
```

### MongoDB
```properties
spring.data.mongodb.uri=mongodb://localhost:27017/blogging_platform
spring.data.mongodb.database=blogging_platform
```

## Data Types Mapping

### Java → PostgreSQL

| Java Type | PostgreSQL Type | Example |
|-----------|----------------|---------|
| UUID | UUID | 550e8400-e29b-41d4-a716-446655440000 |
| String | VARCHAR/TEXT | "Hello World" |
| Long | BIGINT | 123456789 |
| LocalDateTime | TIMESTAMP | 2026-02-02 10:30:00 |
| Set<Tag> | M:N via join table | post_tags |

### Java → MongoDB

| Java Type | MongoDB Type | Example |
|-----------|-------------|---------|
| String (id) | ObjectId | 507f1f77bcf86cd799439011 |
| Long | NumberLong | 123 |
| String | String | "john_doe" |
| LocalDateTime | ISODate | ISODate("2026-02-02T10:30:00Z") |

## Performance Tips

✅ **Use indexes** for frequently queried columns
✅ **Lazy load** relationships to avoid N+1 queries
✅ **Paginate** large result sets
✅ **Cache** frequently accessed data
✅ **Denormalize** when cross-database joins needed

## Full Documentation

See [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) for complete documentation.
