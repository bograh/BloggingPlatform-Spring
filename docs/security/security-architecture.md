# Security Architecture

This document provides an architectural overview of the security implementation in the Blogging Platform Spring Boot application.

## Table of Contents

- [High-Level Architecture Overview](#high-level-architecture-overview)
- [Authentication vs Authorization Boundaries](#authentication-vs-authorization-boundaries)
- [Filter Chain Flow](#filter-chain-flow)
- [Token Handling Strategy](#token-handling-strategy)
- [Security Configuration Layers](#security-configuration-layers)
- [Key Takeaways](#key-takeaways)

---

## High-Level Architecture Overview

The security architecture implements a stateless, token-based authentication system using JWT with OAuth2 support for social login.

```text
+-----------------------------------------------------------------------------+
|                              CLIENT LAYER                                   |
|                    (Web App, Mobile App, API Consumer)                      |
+-------------------------------------+---------------------------------------+
                                      |
                                      v
+-----------------------------------------------------------------------------+
|                           SECURITY GATEWAY                                  |
|  +-----------------------------------------------------------------------+  |
|  |                     Spring Security Filter Chain                      |  |
|  |  +-----------+    +-----------------+    +----------------------+     |  |
|  |  |CORS Filter|--->| JWT Auth Filter |--->| Authorization Filter |     |  |
|  |  +-----------+    +-----------------+    +----------------------+     |  |
|  +-----------------------------------------------------------------------+  |
|                                    |                                        |
|        +--------------+------------+------------+--------------+            |
|        v              v                         v              v            |
|  +----------+   +------------+          +-----------+                       |
|  |  OAuth2  |   | JWT Token  |          | Entry/Deny|                       |
|  |  Handler |   | Provider   |          | Handlers  |                       |
|  +----------+   +------------+          +-----------+                       |
+-----------------------------------------------------------------------------+
                                      |
                                      v
+-----------------------------------------------------------------------------+
|                           APPLICATION LAYER                                 |
|  +---------------+  +---------------+  +-------------------------+          |
|  |  Controllers  |  |  GraphQL API  |  |  Method-Level Security  |          |
|  |   (REST API)  |  |   Resolvers   |  |    (@PreAuthorize)      |          |
|  +---------------+  +---------------+  +-------------------------+          |
+-----------------------------------------------------------------------------+
                                      |
                                      v
+-----------------------------------------------------------------------------+
|                             SERVICE LAYER                                   |
|  +-----------------------------------------------------------------------+  |
|  |  CustomUserDetailsService  |  AuthService  |  TokenSessionService     |  |
|  +-----------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------+
                                      |
                                      v
+-----------------------------------------------------------------------------+
|                              DATA LAYER                                     |
|         +-------------------+              +-------------------+            |
|         |    PostgreSQL     |              |     MongoDB       |            |
|         |  (Users, Posts)   |              |   (Comments)      |            |
|         +-------------------+              +-------------------+            |
+-----------------------------------------------------------------------------+
```

---

## Authentication vs Authorization Boundaries

| Concern            | Responsibility                                           | Implementation Component           |
|--------------------|----------------------------------------------------------|------------------------------------|
| **Authentication** | Verify identity (Who are you?)                           | `JwtAuthenticationFilter`          |
| **Authorization**  | Verify permissions (What can you do?)                    | `SecurityConfig` + `@PreAuthorize` |

### Authentication Boundary

```text
Request arrives
       |
       v
+--------------------------------------+
|        AUTHENTICATION BOUNDARY       |
|  +--------------------------------+  |
|  |  1. Extract JWT from header    |  |
|  |  2. Validate token signature   |  |
|  |  3. Check token revocation     |  |
|  |  4. Load UserDetails           |  |
|  |  5. Set SecurityContext        |  |
|  +--------------------------------+  |
+--------------------------------------+
       |
       v (Authenticated Principal)
+--------------------------------------+
|        AUTHORIZATION BOUNDARY        |
|  +--------------------------------+  |
|  |  1. Check URL-level rules      |  |
|  |  2. Check method-level rules   |  |
|  |  3. Evaluate role hierarchy    |  |
|  +--------------------------------+  |
+--------------------------------------+
       |
       v
  Controller/Resolver
```

---

## Filter Chain Flow

The Spring Security filter chain processes each request in sequence:

```text
HTTP Request
     |
     v
+-----------------------------------------------------------------+
|  1. CORS Filter                                                 |
|     - Validates allowed origins (localhost:3000, localhost:3001)|
|     - Handles preflight OPTIONS requests                        |
+----------------------------+------------------------------------+
                             |
                             v
+-----------------------------------------------------------------+
|  2. JWT Authentication Filter (Before UsernamePasswordAuth)     |
|     +-------------------------------------------------------+   |
|     | Extract Bearer token from Authorization header        |   |
|     |              |                                        |   |
|     |              v                                        |   |
|     | Token present? --- No ---> Continue filter chain      |   |
|     |     | Yes                                             |   |
|     |     v                                                 |   |
|     | Check if token is revoked (TokenSessionService)       |   |
|     |     |                                                 |   |
|     |     +-- Revoked --> Return 401 UNAUTHORIZED           |   |
|     |     |                                                 |   |
|     |     v Not Revoked                                     |   |
|     | Validate JWT signature & expiration                   |   |
|     |     |                                                 |   |
|     |     +-- Invalid --> Log failure, continue chain       |   |
|     |     |                                                 |   |
|     |     v Valid                                           |   |
|     | Extract email from token                              |   |
|     |     |                                                 |   |
|     |     v                                                 |   |
|     | Load UserDetails via CustomUserDetailsService         |   |
|     |     |                                                 |   |
|     |     v                                                 |   |
|     | Create Authentication object with authorities         |   |
|     |     |                                                 |   |
|     |     v                                                 |   |
|     | Set SecurityContextHolder                             |   |
|     +-------------------------------------------------------+   |
+----------------------------+------------------------------------+
                             |
                             v
+-----------------------------------------------------------------+
|  3. OAuth2 Login Filter                                        |
|     - Handles /oauth2/** and /login/oauth2/** paths            |
|     - Redirects to OAuth2 provider (Google)                    |
|     - Processes callback with authorization code               |
+----------------------------+------------------------------------+
                             |
                             v
+-----------------------------------------------------------------+
|  4. Authorization Filter                                        |
|     - Evaluates URL-based access rules                          |
|     - Checks role requirements (ADMIN, AUTHOR, READER)          |
+----------------------------+------------------------------------+
                             |
                             v
+-----------------------------------------------------------------+
|  5. Exception Translation Filter                                |
|     +-------------------------------------------------------+   |
|     | AuthenticationException --> JwtAuthenticationEntryPoint   |
|     |                             (Returns 401 JSON)            |
|     |                                                           |
|     | AccessDeniedException ----> JwtAccessDeniedHandler        |
|     |                             (Returns 403 JSON)            |
|     +-------------------------------------------------------+   |
+----------------------------+------------------------------------+
                             |
                             v
                     Request Handling
```

---

## Token Handling Strategy

### Dual Token Architecture

| Token Type       | Storage Location        | Lifetime          | Purpose                          |
|------------------|-------------------------|-------------------|----------------------------------|
| **Access Token** | Authorization header    | Short (configurable) | API request authentication    |
| **Refresh Token**| HttpOnly cookie         | Long (7 days)     | Obtain new access tokens         |

### Token Security Properties

```text
ACCESS TOKEN                          REFRESH TOKEN
+------------------------+            +------------------------+
| - Sent in header       |            | - Stored in HttpOnly   |
| - Exposed to JS (XSS)  |            |   cookie               |
| - Short-lived          |            | - Not accessible to JS |
| - Separate secret key  |            | - Long-lived           |
| - Contains: email,     |            | - Separate secret key  |
|   issued_at, exp, jti  |            | - SameSite=Lax         |
+------------------------+            | - Secure (in prod)     |
                                      +------------------------+
```

### Token Session Management

The `TokenSessionService` maintains in-memory state for:

1. **Active Sessions**: Tracks logged-in users with their tokens
2. **Revoked Tokens**: Blacklist of invalidated tokens
3. **Automatic Cleanup**: Hourly scheduled job removes expired entries

```text
+-------------------------------------------------------------+
|                   TokenSessionService                       |
|  +-------------------------------------------------------+  |
|  |  ConcurrentHashMap<String, SessionInfo>               |  |
|  |  activeSessions                                       |  |
|  |  +--------------------------------------------------+ |  |
|  |  | Key: email                                       | |  |
|  |  | Value: accessToken, refreshToken,                | |  |
|  |  |        loginTimestamp, lastActivity              | |  |
|  |  +--------------------------------------------------+ |  |
|  +-------------------------------------------------------+  |
|                                                             |
|  +-------------------------------------------------------+  |
|  |  ConcurrentHashMap<String, String>                    |  |
|  |  revokedTokens                                        |  |
|  |  +--------------------------------------------------+ |  |
|  |  | Key: token                                       | |  |
|  |  | Value: email                                     | |  |
|  |  +--------------------------------------------------+ |  |
|  +-------------------------------------------------------+  |
|                                                             |
|  @Scheduled(fixedRate = 3600000)  // Hourly cleanup         |
|  cleanupExpiredSessionsAndTokens()                          |
+-------------------------------------------------------------+
```

---

## Security Configuration Layers

### Layer 1: HTTP Security Configuration

Location: `SecurityConfig.java`

```text
+-------------------------------------------------------------+
|                  SecurityFilterChain                        |
|                                                             |
|  csrf(disable)           --- Stateless session, JWT-based   |
|  cors(defaults)          --- Uses CorsConfigurationSource   |
|  sessionManagement       --- STATELESS policy               |
|  oauth2Login             --- Google OAuth2 integration      |
|  exceptionHandling       --- Custom entry point + handler   |
|  addFilterBefore         --- JWT filter before UsernamePass |
|  authorizeHttpRequests   --- URL-based access rules         |
+-------------------------------------------------------------+
```

### Layer 2: URL-Based Security Rules

```text
PUBLIC ENDPOINTS (No authentication required)
|-- /api/auth/**           Authentication endpoints
|-- /oauth2/**             OAuth2 flow
|-- /login/oauth2/**       OAuth2 callbacks
|-- /graphql               GraphQL endpoint
|-- /graphiql              GraphQL IDE
|-- GET /api/posts/**      Public post viewing
+-- GET /api/tags/**       Public tag viewing

ROLE-RESTRICTED ENDPOINTS
|-- ROLE_AUTHOR
|   |-- POST /api/posts
|   +-- PUT /api/posts/**
|-- ROLE_AUTHOR + ROLE_ADMIN
|   |-- DELETE /api/posts/**
|   |-- POST/PUT/DELETE /api/tags/**
|   +--
+-- ROLE_ADMIN
    |-- /api/admin/**
    |-- /api/users/**
    |-- /api/metrics/performance/**
    +-- /api/security/audit/**

AUTHENTICATED ENDPOINTS
+-- /api/users/profile     Any authenticated user
```

### Layer 3: Method-Level Security

Enabled via `@EnableMethodSecurity`

```java
@PreAuthorize("hasRole('AUTHOR')")          // GraphQL mutations
@PreAuthorize("hasAnyRole('AUTHOR','ADMIN')") // Delete operations
@PreAuthorize("hasRole('ADMIN')")           // Admin queries
```

---

## Key Takeaways

1. **Stateless Architecture**: No server-side session storage; all state is in JWT tokens
2. **Defense in Depth**: Multiple security layers (filter chain → URL rules → method security)
3. **Token Isolation**: Access and refresh tokens use separate secrets and storage mechanisms
4. **Revocation Support**: In-memory token blacklist with automatic expiry cleanup
5. **Unified Auth Flow**: OAuth2 and standard login both result in JWT issuance
6. **Security Auditing**: Failed authentication and access denial events are logged
7. **CORS Configured**: Explicitly allows frontend origins with credentials
8. **CSRF Disabled**: Appropriate for stateless JWT authentication (no cookies for auth state)
