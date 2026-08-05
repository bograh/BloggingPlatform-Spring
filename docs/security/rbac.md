# Role-Based Access Control (RBAC)

This document describes the Role-Based Access Control implementation in the application, covering role hierarchy, permission mapping, and security enforcement strategies.

## Table of Contents

- [Role Hierarchy](#role-hierarchy)
- [Permission Mapping Strategy](#permission-mapping-strategy)
- [Method-Level vs URL-Level Security](#method-level-vs-url-level-security)
- [Access Matrix](#access-matrix)
- [Implementation Details](#implementation-details)
- [Key Takeaways](#key-takeaways)

---

## Role Hierarchy

### Defined Roles

The application defines three roles in the `UserRoles` enum:

```text
+-----------------------------------------------------------------+
|                        ROLE HIERARCHY                           |
+-----------------------------------------------------------------+
|                                                                 |
|                      +---------------+                          |
|                      |     ADMIN     |                          |
|                      |  (Full Access)|                          |
|                      +-------+-------+                          |
|                              |                                  |
|                              | Inherits                         |
|                              v                                  |
|                      +---------------+                          |
|                      |    AUTHOR     |                          |
|                      |(Content CRUD) |                          |
|                      +-------+-------+                          |
|                              |                                  |
|                              | Inherits                         |
|                              v                                  |
|                      +---------------+                          |
|                      |    READER     |                          |
|                      |  (Read Only)  |                          |
|                      +---------------+                          |
|                                                                 |
+-----------------------------------------------------------------+
```

### Role Capabilities

| Role     | Description                                | Primary Use Case           |
|----------|-------------------------------------------|----------------------------|
| `ADMIN`  | Full system access, user management       | Platform administrators    |
| `AUTHOR` | Create/edit/delete own content            | Content creators, bloggers |
| `READER` | Read-only access to public content        | General users, subscribers |

### Implicit Role Hierarchy

The application uses **flat role checking** (no implicit hierarchy). Each role must be explicitly granted:

```
ADMIN user:     Has ROLE_ADMIN
AUTHOR user:    Has ROLE_AUTHOR
READER user:    Has ROLE_READER
```

For endpoints requiring multiple role options, `hasAnyRole()` is used:
```java
.hasAnyRole("AUTHOR", "ADMIN")  // Either role can access
```

---

## Permission Mapping Strategy

### Domain-Based Permission Model

```text
+-----------------------------------------------------------------+
|                    PERMISSION DOMAINS                           |
+-----------------------------------------------------------------+
|                                                                 |
|  +---------------------------------------------------------+   |
|  |  AUTHENTICATION DOMAIN (Public)                         |   |
|  |  +-----------------------------------------------------+|   |
|  |  |  /api/auth/**                                       ||   |
|  |  |  - Register, Sign-in, Refresh, Sign-out             ||   |
|  |  |  - No authentication required                       ||   |
|  |  +-----------------------------------------------------+|   |
|  +---------------------------------------------------------+   |
|                                                                 |
|  +---------------------------------------------------------+   |
|  |  CONTENT DOMAIN                                         |   |
|  |  +-----------------------------------------------------+|   |
|  |  |  Posts & Tags                                       ||   |
|  |  |  - READ: Public (anonymous allowed)                 ||   |
|  |  |  - CREATE/UPDATE: AUTHOR                            ||   |
|  |  |  - DELETE: AUTHOR or ADMIN                          ||   |
|  |  +-----------------------------------------------------+|   |
|  |  +-----------------------------------------------------+|   |
|  |  |  Comments                                           ||   |
|  |  |  - READ: Public                                     ||   |
|  |  |  - CREATE: Authenticated users                      ||   |
|  |  |  - DELETE: Owner or ADMIN                           ||   |
|  |  +-----------------------------------------------------+|   |
|  +---------------------------------------------------------+   |
|                                                                 |
|  +---------------------------------------------------------+   |
|  |  USER DOMAIN                                            |   |
|  |  +-----------------------------------------------------+|   |
|  |  |  Profile (/api/users/profile)                       ||   |
|  |  |  - Any authenticated user can access own profile    ||   |
|  |  +-----------------------------------------------------+|   |
|  |  +-----------------------------------------------------+|   |
|  |  |  User Management (/api/users/**)                    ||   |
|  |  |  - ADMIN only                                       ||   |
|  |  +-----------------------------------------------------+|   |
|  +---------------------------------------------------------+   |
|                                                                 |
|  +---------------------------------------------------------+   |
|  |  ADMINISTRATION DOMAIN (ADMIN Only)                     |   |
|  |  +-----------------------------------------------------+|   |
|  |  |  /api/admin/**                                      ||   |
|  |  |  /api/metrics/performance/**                        ||   |
|  |  |  /api/security/audit/**                             ||   |
|  |  +-----------------------------------------------------+|   |
|  +---------------------------------------------------------+   |
|                                                                 |
+-----------------------------------------------------------------+
```

---

## Method-Level vs URL-Level Security

### URL-Level Security (SecurityConfig)

Configured in `SecurityFilterChain`:

```java
.authorizeHttpRequests(auth -> auth
    // Public endpoints
    .requestMatchers("/api/auth/**", "/oauth2/**", "/graphql").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()

    // Role-restricted endpoints
    .requestMatchers(HttpMethod.POST, "/api/posts").hasRole("AUTHOR")
    .requestMatchers(HttpMethod.PUT, "/api/posts/**").hasRole("AUTHOR")
    .requestMatchers(HttpMethod.DELETE, "/api/posts/**").hasAnyRole("AUTHOR", "ADMIN")

    // Admin-only endpoints
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/users/**").hasRole("ADMIN")
    .requestMatchers("/api/metrics/performance/**").hasRole("ADMIN")

    // Default
    .anyRequest().authenticated()
)
```

### Method-Level Security (@PreAuthorize)

Enabled via `@EnableMethodSecurity` and applied to individual methods:

```java
// GraphQL Mutations
@PreAuthorize("hasRole('AUTHOR')")
public Post createPost(CreatePostInput input) { ... }

@PreAuthorize("hasRole('AUTHOR')")
public Post updatePost(Long postId, UpdatePostInput input) { ... }

@PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
public Boolean deletePost(Long postId, UUID authorId) { ... }

// GraphQL Queries (Admin)
@PreAuthorize("hasRole('ADMIN')")
public List<User> getAllUsers() { ... }
```

### Comparison

| Aspect              | URL-Level                        | Method-Level                    |
|---------------------|----------------------------------|--------------------------------|
| **Location**        | `SecurityConfig.java`            | `@PreAuthorize` on methods     |
| **Granularity**     | HTTP method + path pattern       | Individual method              |
| **Use Case**        | REST API coarse-grained access   | Fine-grained business logic    |
| **Applies To**      | REST controllers only            | Any Spring-managed bean        |
| **GraphQL**         | Cannot differentiate operations  | Required for GraphQL security  |
| **Evaluation Time** | Before controller invocation     | At method invocation           |

### Why Both Are Used

```text
+-----------------------------------------------------------------+
|                    SECURITY LAYERS                              |
+-----------------------------------------------------------------+
|                                                                 |
|  HTTP Request                                                   |
|       |                                                         |
|       v                                                         |
|  +---------------------------------------------------------+   |
|  |  Layer 1: URL-Level Security                            |   |
|  |  - First line of defense                                |   |
|  |  - Blocks obvious unauthorized access early             |   |
|  |  - Efficient (no method invocation needed)              |   |
|  |  - Works for REST APIs with distinct URL patterns       |   |
|  +---------------------------------------------------------+   |
|       |                                                         |
|       v                                                         |
|  +---------------------------------------------------------+   |
|  |  Layer 2: Method-Level Security                         |   |
|  |  - Required for GraphQL (single /graphql endpoint)      |   |
|  |  - Enables per-operation authorization                  |   |
|  |  - Can access method parameters for decisions           |   |
|  |  - Defense in depth                                     |   |
|  +---------------------------------------------------------+   |
|       |                                                         |
|       v                                                         |
|  Business Logic Execution                                       |
|                                                                 |
+-----------------------------------------------------------------+
```

---

## Access Matrix

### REST API Endpoints

| Endpoint                         | Method   | Anonymous | READER | AUTHOR | ADMIN |
|----------------------------------|----------|:---------:|:------:|:------:|:-----:|
| `/api/auth/register`             | POST     | ✅        | ✅     | ✅     | ✅    |
| `/api/auth/sign-in`              | POST     | ✅        | ✅     | ✅     | ✅    |
| `/api/auth/refresh-token`        | POST     | ✅        | ✅     | ✅     | ✅    |
| `/api/auth/sign-out`             | POST     | ✅        | ✅     | ✅     | ✅    |
| `/api/posts`                     | GET      | ✅        | ✅     | ✅     | ✅    |
| `/api/posts/{id}`                | GET      | ✅        | ✅     | ✅     | ✅    |
| `/api/posts`                     | POST     | ❌        | ❌     | ✅     | ❌    |
| `/api/posts/{id}`                | PUT      | ❌        | ❌     | ✅     | ❌    |
| `/api/posts/{id}`                | DELETE   | ❌        | ❌     | ✅     | ✅    |
| `/api/tags`                      | GET      | ✅        | ✅     | ✅     | ✅    |
| `/api/tags/popular`              | GET      | ✅        | ✅     | ✅     | ✅    |
| `/api/tags`                      | POST     | ❌        | ❌     | ✅     | ✅    |
| `/api/tags/{id}`                 | PUT      | ❌        | ❌     | ✅     | ✅    |
| `/api/tags/{id}`                 | DELETE   | ❌        | ❌     | ✅     | ✅    |
| `/api/users/profile`             | GET      | ❌        | ✅     | ✅     | ✅    |
| `/api/users/**`                  | ALL      | ❌        | ❌     | ❌     | ✅    |
| `/api/admin/**`                  | ALL      | ❌        | ❌     | ❌     | ✅    |
| `/api/metrics/performance/**`    | ALL      | ❌        | ❌     | ❌     | ✅    |
| `/api/security/audit/**`         | ALL      | ❌        | ❌     | ❌     | ✅    |

### GraphQL Operations

| Operation                        | Type     | Anonymous | READER | AUTHOR | ADMIN |
|----------------------------------|----------|:---------:|:------:|:------:|:-----:|
| `getAllPosts`                    | Query    | ✅        | ✅     | ✅     | ✅    |
| `getPostById`                    | Query    | ✅        | ✅     | ✅     | ✅    |
| `getUserById`                    | Query    | ✅        | ✅     | ✅     | ✅    |
| `getCommentsByPostId`            | Query    | ✅        | ✅     | ✅     | ✅    |
| `getAllUsers`                    | Query    | ❌        | ❌     | ❌     | ✅    |
| `createPost`                     | Mutation | ❌        | ❌     | ✅     | ❌    |
| `updatePost`                     | Mutation | ❌        | ❌     | ✅     | ❌    |
| `deletePost`                     | Mutation | ❌        | ❌     | ✅     | ✅    |
| `createComment`                  | Mutation | ❌        | ❌     | ✅     | ❌    |
| `deleteComment`                  | Mutation | ❌        | ❌     | ✅     | ✅    |

---

## Implementation Details

### Role Storage

Roles are stored in the `User` entity:

```java
@Entity
public class User {
    // ...

    @Enumerated(EnumType.STRING)
    private UserRoles role;  // ADMIN, AUTHOR, or READER
}
```

### Authority Conversion

Roles are converted to Spring Security `GrantedAuthority` objects:

```java
// UserUtils.java
public List<GrantedAuthority> getUserAuthorities(User user) {
    return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
}
```

### UserDetails Loading

```java
// CustomUserDetailsService.java
@Override
public UserDetails loadUserByUsername(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    List<GrantedAuthority> authorities = userUtils.getUserAuthorities(user);

    return new org.springframework.security.core.userdetails.User(
        user.getEmail(),
        user.getPassword(),
        authorities
    );
}
```

### Authentication Object Flow

```text
+-----------------------------------------------------------------+
|               AUTHENTICATION OBJECT FLOW                        |
+-----------------------------------------------------------------+
|                                                                 |
|  JWT Token (contains email)                                     |
|       |                                                         |
|       v                                                         |
|  JwtAuthenticationFilter extracts email                         |
|       |                                                         |
|       v                                                         |
|  CustomUserDetailsService.loadUserByUsername(email)             |
|       |                                                         |
|       v                                                         |
|  UserRepository.findByEmail(email)                              |
|       |                                                         |
|       v                                                         |
|  User entity retrieved (includes role)                          |
|       |                                                         |
|       v                                                         |
|  Convert role to GrantedAuthority: "ROLE_AUTHOR"                |
|       |                                                         |
|       v                                                         |
|  Create UserDetails with authorities                            |
|       |                                                         |
|       v                                                         |
|  Create UsernamePasswordAuthenticationToken                     |
|       |                                                         |
|       v                                                         |
|  Set in SecurityContextHolder                                   |
|       |                                                         |
|       v                                                         |
|  Available for URL-level and method-level security checks       |
|                                                                 |
+-----------------------------------------------------------------+
```

---

## Key Takeaways

1. **Three-Tier Role Model**: ADMIN > AUTHOR > READER hierarchy maps to common blogging platform needs
2. **Flat Role Checking**: No implicit inheritance; use `hasAnyRole()` for multiple role acceptance
3. **Defense in Depth**: URL-level filtering combined with method-level `@PreAuthorize` annotations
4. **GraphQL Security**: Method-level security is mandatory since all operations share `/graphql` endpoint
5. **Role in JWT**: Role not stored in token; loaded fresh from database on each request
6. **Single Role Per User**: Current design supports one role per user; easily extendable to multi-role
7. **Ownership Not Enforced**: RBAC controls role-based access; ownership checks (author can only edit own posts) are implemented in service layer
8. **Default Role Assignment**: New users (including OAuth2) receive a default role configured in application
