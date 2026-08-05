# JWT Authentication Flow

This document details the JWT-based authentication flow implemented in the application, covering token generation, validation, refresh, and revocation mechanisms.

## Table of Contents

- [Login Flow](#login-flow)
- [Token Generation](#token-generation)
- [Access vs Refresh Token Lifecycle](#access-vs-refresh-token-lifecycle)
- [Token Validation Process](#token-validation-process)
- [Expiration and Rotation Strategy](#expiration-and-rotation-strategy)
- [Token Revocation](#token-revocation)
- [Key Takeaways](#key-takeaways)

---

## Login Flow

### Registration and Sign-In Sequence

```text
+----------+          +--------------+          +---------------+          +-------------+
|  Client  |          | AuthController|         |  AuthService  |          | JwtProvider |
+----+-----+          +------+-------+          +-------+-------+          +------+------+
     |                       |                          |                         |
     |  POST /api/auth/sign-in                          |                         |
     |  {email, password}    |                          |                         |
     |---------------------->|                          |                         |
     |                       |                          |                         |
     |                       |  signInUser(credentials) |                         |
     |                       |------------------------->|                         |
     |                       |                          |                         |
     |                       |                          |  Authenticate user      |
     |                       |                          |  (verify password)      |
     |                       |                          |--------+                |
     |                       |                          |        |                |
     |                       |                          |<-------+                |
     |                       |                          |                         |
     |                       |                          |  createAccessToken()    |
     |                       |                          |------------------------>|
     |                       |                          |                         |
     |                       |                          |     accessToken         |
     |                       |                          |<------------------------|
     |                       |                          |                         |
     |                       |                          |  createRefreshToken()   |
     |                       |                          |------------------------>|
     |                       |                          |                         |
     |                       |                          |     refreshToken        |
     |                       |                          |<------------------------|
     |                       |                          |                         |
     |                       |  AuthResponseDTO         |                         |
     |                       |<-------------------------|                         |
     |                       |                          |                         |
     |                       |  Create session in       |                         |
     |                       |  TokenSessionService     |                         |
     |                       |--------+                 |                         |
     |                       |<-------+                 |                         |
     |                       |                          |                         |
     |                       |  Set-Cookie: refreshToken|                         |
     |  HTTP 200             |  (HttpOnly, Secure)      |                         |
     |  {accessToken, user}  |                          |                         |
     |<----------------------|                          |                         |
     |                       |                          |                         |
```

---

## Token Generation

### JWT Structure

Both access and refresh tokens follow the same structure with different secrets and expiration times:

```text
+-----------------------------------------------------------------+
|                         JWT TOKEN                               |
+-----------------------------------------------------------------+
|  HEADER (Base64)                                                |
|  {                                                              |
|    "alg": "HS256",                                              |
|    "typ": "JWT"                                                 |
|  }                                                              |
+-----------------------------------------------------------------+
|  PAYLOAD (Base64)                                               |
|  {                                                              |
|    "sub": "user@example.com",    // User email (subject)        |
|    "iat": 1739635200,             // Issued at timestamp        |
|    "exp": 1739638800,             // Expiration timestamp       |
|    "jti": "550e8400-e29b..."      // Unique token ID (UUID)     |
|  }                                                              |
+-----------------------------------------------------------------+
|  SIGNATURE                                                      |
|  HMACSHA256(                                                    |
|    base64UrlEncode(header) + "." + base64UrlEncode(payload),    |
|    secret                                                       |
|  )                                                              |
+-----------------------------------------------------------------+
```

### Token Creation Implementation

```java
// JwtTokenProvider.java
private String createToken(Authentication authentication, String secret, long expirationMs) {
    String email = authentication.getName();
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationMs);

    return Jwts.builder()
            .issuedAt(now)
            .expiration(expiryDate)
            .subject(email)                    // User identifier
            .id(String.valueOf(UUID.randomUUID())) // Unique token ID
            .signWith(getSigningKey(secret))   // HMAC-SHA signing
            .compact();
}
```

---

## Access vs Refresh Token Lifecycle

### Token Comparison

| Attribute          | Access Token                  | Refresh Token                |
|--------------------|-------------------------------|------------------------------|
| **Purpose**        | Authorize API requests        | Obtain new access tokens     |
| **Lifetime**       | Short (minutes to hours)      | Long (7 days default)        |
| **Storage**        | Client memory / localStorage  | HttpOnly cookie              |
| **Transmission**   | Authorization header          | Cookie header (automatic)    |
| **Secret Key**     | `jwt.access-token-secret`     | `jwt.refresh-token-secret`   |
| **XSS Exposure**   | Yes (if in localStorage)      | No (HttpOnly)                |
| **CSRF Exposure**  | No (manual header)            | Yes (mitigated by SameSite)  |

### Token Lifecycle Diagram

```text
+-----------------------------------------------------------------+
|                    ACCESS TOKEN LIFECYCLE                       |
+-----------------------------------------------------------------+
|                                                                 |
|  [Creation]                                                     |
|      |                                                          |
|      v                                                          |
|  +-----------+                                                  |
|  |   VALID   |<------------------------------+                  |
|  +-----+-----+                               |                  |
|        |                                     |                  |
|        | Time passes / Revoked               | New token        |
|        |                                     | issued           |
|        v                                     |                  |
|  +-----------+     Token Refresh      +------+----------+       |
|  |  EXPIRED  |----------------------->| REFRESH PROCESS |       |
|  +-----------+                        +------------------+      |
|                                                                 |
+-----------------------------------------------------------------+

+-----------------------------------------------------------------+
|                   REFRESH TOKEN LIFECYCLE                       |
+-----------------------------------------------------------------+
|                                                                 |
|  [Creation]          Cookie stored: refreshToken                |
|      |               HttpOnly=true, Secure=true, SameSite=Lax   |
|      v                                                          |
|  +-----------+                                                  |
|  |   VALID   |                                                  |
|  +-----+-----+                                                  |
|        |                                                        |
|        | 7 days pass / User signs out / Revoked                 |
|        v                                                        |
|  +-----------+                                                  |
|  |  EXPIRED  |-----> User must re-authenticate                  |
|  +-----------+                                                  |
|                                                                 |
+-----------------------------------------------------------------+
```

---

## Token Validation Process

### Validation Sequence

```text
+----------+          +------------------+          +-------------+
|  Client  |          | JwtAuthFilter    |          | JwtProvider |
+----+-----+          +--------+---------+          +------+------+
     |                         |                           |
     |  GET /api/posts         |                           |
     |  Authorization: Bearer  |                           |
     |  eyJhbGciOi...          |                           |
     |------------------------>|                           |
     |                         |                           |
     |                         |  Extract token from       |
     |                         |  Authorization header     |
     |                         |----------+                |
     |                         |<---------+                |
     |                         |                           |
     |                         |  isTokenRevoked(token)    |
     |                         |------------------------+  |
     |                         |                        |  |
     |                         |<-----------------------+  |
     |                         |                           |
     |                         |  [If Revoked]             |
     |  HTTP 401               |                           |
     |  {error: "Invalid..."}  |                           |
     |<------------------------|                           |
     |                         |                           |
     |                         |  [If Not Revoked]         |
     |                         |  validAccessToken(token)  |
     |                         |-------------------------->|
     |                         |                           |
     |                         |  Verify signature --------|
     |                         |  Check expiration --------|
     |                         |  Parse claims ------------|
     |                         |                           |
     |                         |  true/false               |
     |                         |<--------------------------|
     |                         |                           |
     |                         |  [If Valid]               |
     |                         |  getEmailFromToken(token) |
     |                         |-------------------------->|
     |                         |                           |
     |                         |  email                    |
     |                         |<--------------------------|
     |                         |                           |
     |                         |  Load UserDetails         |
     |                         |  Set SecurityContext      |
     |                         |----------+                |
     |                         |<---------+                |
     |                         |                           |
     |  Continue to controller |                           |
     |                         |                           |
```

### Validation Decision Tree

```text
                    Token Received
                         |
                         v
               +-----------------+
               | Token in header?|
               +--------+--------+
                   Yes  |  No
          +-------------+--------------+
          v                            v
+-----------------+          Continue unauthenticated
| Token revoked?  |          (public endpoints OK)
+--------+--------+
    Yes  |  No
   +-----+-------------+
   |                   v
   v          +-----------------+
401 Error     | Signature valid?|
              +--------+--------+
                  Yes  |  No
          +------------+--------+
          v                     v
+-----------------+       Log failure
| Token expired?  |       Continue unauth
+--------+--------+
    Yes  |  No
   +-----+-------------+
   v                   v
Log failure     +--------------+
Continue unauth | Extract email|
                | Load user    |
                | Set context  |
                +--------------+
```

---

## Expiration and Rotation Strategy

### Token Refresh Flow

```text
+----------+          +--------------+          +---------------+          +-------------+
|  Client  |          |AuthController|          |  AuthService  |          | JwtProvider |
+----+-----+          +------+-------+          +-------+-------+          +------+------+
     |                       |                          |                         |
     |  POST /api/auth/refresh-token                    |                         |
     |  Cookie: refreshToken=eyJ...                     |                         |
     |---------------------->|                          |                         |
     |                       |                          |                         |
     |                       |  Check if token revoked  |                         |
     |                       |--------+                 |                         |
     |                       |<-------+                 |                         |
     |                       |                          |                         |
     |                       |  [If Revoked]            |                         |
     |  HTTP 401             |                          |                         |
     |  UnauthorizedException|                          |                         |
     |<----------------------|                          |                         |
     |                       |                          |                         |
     |                       |  [If Valid]              |                         |
     |                       |  refreshAccessToken()    |                         |
     |                       |------------------------->|                         |
     |                       |                          |                         |
     |                       |                          |  Validate refresh token |
     |                       |                          |  Extract email          |
     |                       |                          |  Load user              |
     |                       |                          |------------------------>|
     |                       |                          |                         |
     |                       |                          |  Create new tokens      |
     |                       |                          |<------------------------|
     |                       |                          |                         |
     |                       |  AuthResponseDTO         |                         |
     |                       |<-------------------------|                         |
     |                       |                          |                         |
     |                       |  Update session          |                         |
     |                       |  Set new refresh cookie  |                         |
     |                       |--------+                 |                         |
     |                       |<-------+                 |                         |
     |                       |                          |                         |
     |  HTTP 200             |                          |                         |
     |  {accessToken, user}  |                          |                         |
     |  Set-Cookie: refresh  |                          |                         |
     |<----------------------|                          |                         |
     |                       |                          |                         |
```

### Refresh Token Rotation

On each refresh, both tokens are rotated:

```
Before Refresh:
  Access Token A (expired)
  Refresh Token R1 (valid)

After Refresh:
  Access Token B (new, valid)
  Refresh Token R2 (new, valid)
  Session updated with B and R2
```

This rotation strategy:
- Limits the window of token reuse
- Invalidates the previous refresh token implicitly
- Maintains session continuity

---

## Token Revocation

### Revocation Triggers

```text
+-----------------------------------------------------------------+
|                    REVOCATION TRIGGERS                          |
+-----------------------------------------------------------------+
|                                                                 |
|  1. User Sign-Out                                               |
|     POST /api/auth/sign-out                                     |
|     +-- Revoke access token                                     |
|     +-- Revoke refresh token                                    |
|     +-- Remove session from activeSessions                      |
|     +-- Clear refresh cookie                                    |
|                                                                 |
|  2. Session Invalidation                                        |
|     TokenSessionService.removeSession(email)                    |
|     +-- Revoke both tokens                                      |
|     +-- Remove from activeSessions                              |
|                                                                 |
|  3. Scheduled Cleanup (Hourly)                                  |
|     cleanupExpiredSessionsAndTokens()                           |
|     +-- Remove expired sessions                                 |
|     +-- Remove revoked tokens past expiry                       |
|                                                                 |
+-----------------------------------------------------------------+
```

### Sign-Out Flow

```text
+----------+          +--------------+          +-------------------+
|  Client  |          |AuthController|          |TokenSessionService|
+----+-----+          +------+-------+          +--------+----------+
     |                       |                           |
     |  POST /api/auth/sign-out                          |
     |  Authorization: Bearer eyJ...                     |
     |  Cookie: refreshToken=eyJ...                      |
     |---------------------->|                           |
     |                       |                           |
     |                       |  Extract access token     |
     |                       |  Get email from token     |
     |                       |--------+                  |
     |                       |<-------+                  |
     |                       |                           |
     |                       |  removeSession(email)     |
     |                       |-------------------------->|
     |                       |                           |
     |                       |                           |  Add tokens to
     |                       |                           |  revokedTokens map
     |                       |                           |--------+
     |                       |                           |<-------+
     |                       |                           |
     |                       |  revokeToken(accessToken) |
     |                       |-------------------------->|
     |                       |                           |
     |                       |  revokeToken(refreshToken)|
     |                       |-------------------------->|
     |                       |                           |
     |                       |  Clear refresh cookie     |
     |                       |--------+                  |
     |                       |<-------+                  |
     |                       |                           |
     |  HTTP 200             |                           |
     |  Set-Cookie:          |                           |
     |  refreshToken=;Max-Age=0                          |
     |<----------------------|                           |
     |                       |                           |
```

---

## Key Takeaways

1. **Dual Token Design**: Access tokens for API auth, refresh tokens for session continuity
2. **Separate Secrets**: Access and refresh tokens use different signing keys
3. **Secure Cookie Storage**: Refresh tokens stored as HttpOnly cookies prevent XSS theft
4. **Stateless with Revocation**: In-memory revocation list enables immediate token invalidation
5. **Token Rotation**: Both tokens are refreshed together, limiting replay windows
6. **Automatic Cleanup**: Hourly scheduled task removes expired sessions and tokens
7. **Subject-Based Identity**: Email serves as the user identifier in token claims
8. **Session Tracking**: Active sessions tracked for monitoring and forced logout
