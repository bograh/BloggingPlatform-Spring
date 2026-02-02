# Entity Relationship Diagram

Visual representation of the database entities and their relationships.

## Crow's Foot Notation Diagram

```
                             BLOGGING PLATFORM DATABASE
                                    ER DIAGRAM

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              POSTGRESQL SCHEMA                                   │
└─────────────────────────────────────────────────────────────────────────────────┘


        ┌──────────────────────┐
        │       users          │
        ├──────────────────────┤
        │ ◆ id (UUID)          │
        │ • username           │
        │ • email              │
        │ • password           │
        │ • created_at         │
        └──────────┬───────────┘
                   │
                   │ 1
                   │
                   │ owns
                   │
                   │ N
                   ▼
        ┌──────────────────────┐
        │       posts          │
        ├──────────────────────┤
        │ ◆ id (BIGINT)        │
        │ • title              │
        │ • body               │
        │ ○ author_id          │──┐
        │ • posted_at          │  │
        │ • updated_at         │  │
        └──────────┬───────────┘  │
                   │               │
                   │               │
                   │ M             │
                   │               │
                   │ tagged_with   │
                   │               │
                   │ N             │
                   ▼               │
        ┌──────────────────────┐  │
        │     post_tags        │  │
        │   (Join Table)       │  │
        ├──────────────────────┤  │
        │ ○ post_id            │◄─┘
        │ ○ tag_id             │──┐
        └──────────────────────┘  │
                   │               │
                   │ N             │
                   │               │
                   │ references    │
                   │               │
                   │ 1             │
                   ▼               │
        ┌──────────────────────┐  │
        │       tags           │  │
        ├──────────────────────┤  │
        │ ◆ id (BIGINT)        │◄─┘
        │ • name               │
        └──────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              MONGODB SCHEMA                                      │
└─────────────────────────────────────────────────────────────────────────────────┘

        ┌──────────────────────┐
        │     comments         │
        ├──────────────────────┤
        │ ◆ _id (ObjectId)     │
        │ • post_id            │─ ─ ─ ─ ► posts.id
        │ • author_id          │─ ─ ─ ─ ► users.id
        │ • author             │
        │ • content            │
        │ • commented_at       │
        └──────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              LEGEND                                              │
└─────────────────────────────────────────────────────────────────────────────────┘

    ◆   Primary Key
    •   Required Field
    ○   Foreign Key

    ──►  Strong Relationship (Foreign Key)
    ─ ►  Weak Relationship (Application-level reference)

    1    One
    N    Many
    M:N  Many-to-Many
```

## Detailed Relationship Diagram

```
┌─────────────┐
│    User     │
│             │
│ - id        │
│ - username  │
│ - email     │
│ - password  │
│ - createdAt │
└──────┬──────┘
       │
       │ 1:N (One user has many posts)
       │ Cascade: DELETE posts when user deleted
       │ Fetch: LAZY
       │
       ▼
┌──────────────┐
│    Post      │──────────┐
│              │          │
│ - id         │          │
│ - title      │          │
│ - body       │          │
│ - authorId   │◄─────────┘
│ - postedAt   │
│ - updatedAt  │
└──────┬───────┘
       │
       │ M:N (Posts have many tags, tags belong to many posts)
       │ Join Table: post_tags
       │ Cascade: PERSIST, MERGE (not DELETE)
       │ Fetch: LAZY
       │
       ├───────────────────────────┐
       │                           │
       ▼                           ▼
┌──────────────┐            ┌─────────────┐
│  post_tags   │            │     Tag     │
│              │            │             │
│ - postId     │◄───────────│ - id        │
│ - tagId      │            │ - name      │
└──────────────┘            └─────────────┘


┌─────────────┐
│    Post     │
│             │
│ - id        │────┐
└─────────────┘    │
                   │ 1:N (One post has many comments)
                   │ Database: Cross-database reference
                   │ Enforced: Application layer
                   │ Storage: MongoDB
                   │
                   ▼
            ┌──────────────┐
            │   Comment    │
            │   (MongoDB)  │
            │              │
            │ - _id        │
            │ - postId     │◄─────┘
            │ - authorId   │◄─────┐
            │ - author     │      │
            │ - content    │      │
            │ - commentedAt│      │
            └──────────────┘      │
                   ▲              │
                   │              │
                   └──────────────┘
                   1:N (One user creates many comments)
                   Database: Cross-database reference
                   Enforced: Application layer
                   Author name: Denormalized
```

## Cardinality Details

### User ↔ Post Relationship

```
┌──────┐       ┌──────┐
│ User │───────│ Post │
└──────┘       └──────┘
   1      :       N

Minimum: 0 (user can have no posts)
Maximum: ∞ (user can have unlimited posts)
```

### Post ↔ Tag Relationship

```
┌──────┐       ┌─────────┐       ┌─────┐
│ Post │───────│post_tags│───────│ Tag │
└──────┘       └─────────┘       └─────┘
   M       :       N

Minimum: 0 (post can have no tags)
Maximum: ∞ (post can have unlimited tags)

Minimum: 0 (tag can be on no posts)
Maximum: ∞ (tag can be on unlimited posts)
```

### Post ↔ Comment Relationship

```
┌──────┐       ┌─────────┐
│ Post │───────│ Comment │
└──────┘       └─────────┘
   1      :       N

Minimum: 0 (post can have no comments)
Maximum: ∞ (post can have unlimited comments)
```

### User ↔ Comment Relationship

```
┌──────┐       ┌─────────┐
│ User │───────│ Comment │
└──────┘       └─────────┘
   1      :       N

Minimum: 0 (user can have no comments)
Maximum: ∞ (user can create unlimited comments)
```

## Referential Integrity

### Strong Integrity (Database-enforced)

| Parent Table | Child Table | Constraint | On Delete |
|--------------|-------------|------------|-----------|
| users | posts | FK_author | CASCADE |
| posts | post_tags | FK_post | CASCADE |
| tags | post_tags | FK_tag | CASCADE |

### Weak Integrity (Application-enforced)

| Parent Table | Child Collection | Relationship | Enforcement |
|--------------|------------------|--------------|-------------|
| posts | comments | post_id reference | Application validates |
| users | comments | author_id reference | Application validates |

## Normalized vs Denormalized Data

### Normalized (PostgreSQL)

✅ Users stored once
✅ Posts reference users via FK
✅ Tags stored once, referenced via join table
✅ No data duplication

### Denormalized (MongoDB)

✅ Comment stores author_id (reference)
⚠️ Comment stores author (username copy)
💡 Trade-off: Faster reads, eventual consistency

**Justification**: Comments display author name frequently, avoiding cross-database joins

## Entity Details

### User Entity

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;  // Bcrypt hashed

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "author")
    private Set<Post> posts = new HashSet<>();
}
```

### Post Entity

```java
@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "post_tags",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();
}
```

### Tag Entity

```java
@Entity
@Table(name = "tags")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<Post> posts = new HashSet<>();
}
```

### Comment Document

```java
@Document(collection = "comments")
public class Comment {
    @Id
    private String id;

    @Field("post_id")
    private Long postId;

    @Field("author_id")
    private String authorId;  // UUID as String

    @Field("author")
    private String author;  // Denormalized username

    @Field("content")
    private String content;

    @Field("commented_at")
    private LocalDateTime commentedAt;
}
```

## Database Choice Rationale

### Why PostgreSQL for Users, Posts, Tags?

✅ Complex relationships (user-post-tag)
✅ ACID transactions required
✅ Referential integrity needed
✅ Structured, stable schema
✅ Excellent JOIN performance

### Why MongoDB for Comments?

✅ Flexible schema (may add ratings, reactions)
✅ High write throughput
✅ Denormalization acceptable
✅ Independent scalability
✅ Document model matches use case

## For More Information

- [Database Schema Documentation](DATABASE_SCHEMA.md)
- [Database Quick Reference](DATABASE_QUICK_REFERENCE.md)
- [Performance Optimization Guide](aop/PERFORMANCE_METRICS_GUIDE.md)
