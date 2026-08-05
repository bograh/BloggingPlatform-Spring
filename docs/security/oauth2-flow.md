# OAuth2 Authentication Flow

This document describes the OAuth2 integration in the application, specifically the Authorization Code flow with Google as the identity provider, and how it integrates with the internal JWT-based authentication system.

## Table of Contents

- [Authorization Code Flow Overview](#authorization-code-flow-overview)
- [Redirection and Callback Handling](#redirection-and-callback-handling)
- [Token Exchange Process](#token-exchange-process)
- [Principal Extraction](#principal-extraction)
- [Integration with Internal JWT Issuing](#integration-with-internal-jwt-issuing)
- [Configuration](#configuration)
- [Key Takeaways](#key-takeaways)

---

## Authorization Code Flow Overview

The application implements OAuth2 Authorization Code flow with Google, then bridges to internal JWT issuance.

```text
+-----------------------------------------------------------------+
|                  OAuth2 + JWT Bridge Flow                       |
+-----------------------------------------------------------------+
|                                                                 |
|  +----------+     +--------------+     +--------------------+   |
|  |  Client  |     | Spring Boot  |     |   Google OAuth2    |   |
|  |  (SPA)   |     |  Application |     |     Provider       |   |
|  +----+-----+     +------+-------+     +---------+----------+   |
|       |                  |                       |              |
|       |  1. Login click  |                       |              |
|       |----------------->|                       |              |
|       |                  |                       |              |
|       |  2. Redirect to Google                   |              |
|       |<-----------------+---------------------->|              |
|       |                  |                       |              |
|       |  3. User authenticates with Google       |              |
|       |                  |                       |              |
|       |  4. Google redirects with auth code      |              |
|       |                  |<----------------------|              |
|       |                  |                       |              |
|       |  5. Exchange code for Google tokens      |              |
|       |                  |---------------------->|              |
|       |                  |<----------------------|              |
|       |                  |                       |              |
|       |  6. Fetch user info from Google          |              |
|       |                  |---------------------->|              |
|       |                  |<----------------------|              |
|       |                  |                       |              |
|       |  7. Create/update local user             |              |
|       |                  |                       |              |
|       |  8. Issue internal JWT tokens            |              |
|       |                  |                       |              |
|       |  9. Redirect to frontend with JWT        |              |
|       |<-----------------|                       |              |
|       |                  |                       |              |
|  +----------------------------------------------------------+   |
|                                                                 |
+-----------------------------------------------------------------+
```

---

## Redirection and Callback Handling

### OAuth2 Endpoint Structure

| Endpoint Pattern                        | Purpose                      |
|-----------------------------------------|------------------------------|
| `/oauth2/authorization/{provider}`      | Initiate OAuth2 login        |
| `/login/oauth2/code/{registrationId}`   | Handle OAuth2 callback       |

### Full Sequence Diagram

```text
+----------+     +-----------------+     +---------------+     +--------------+
|  Browser |     | Spring Security |     | Google OAuth2 |     |   Frontend   |
+----+-----+     +--------+--------+     +-------+-------+     +------+-------+
     |                    |                      |                    |
     |  GET /oauth2/authorization/google         |                    |
     |------------------->|                      |                    |
     |                    |                      |                    |
     |                    |  Build authorization URL:                 |
     |                    |  - client_id                              |
     |                    |  - redirect_uri                           |
     |                    |  - scope (openid, email, profile)         |
     |                    |  - state (CSRF protection)                |
     |                    |  - response_type=code                     |
     |                    |----------+           |                    |
     |                    |<---------+           |                    |
     |                    |                      |                    |
     |  HTTP 302 Redirect |                      |                    |
     |  Location: https://accounts.google.com/o/oauth2/v2/auth?...   |
     |<-------------------|                      |                    |
     |                    |                      |                    |
     |  Browser follows redirect                 |                    |
     |------------------------------------------>|                    |
     |                    |                      |                    |
     |  User sees Google login page              |                    |
     |  User enters credentials                  |                    |
     |  User grants consent                      |                    |
     |                    |                      |                    |
     |  HTTP 302 Redirect |                      |                    |
     |  Location: {baseUrl}/login/oauth2/code/google?code=xxx&state=yyy
     |<-----------------------------------------|                    |
     |                    |                      |                    |
     |  GET /login/oauth2/code/google?code=xxx&state=yyy             |
     |------------------->|                      |                    |
     |                    |                      |                    |
     |                    |  Validate state (CSRF)                   |
     |                    |  Exchange code for tokens                |
     |                    |--------------------->|                    |
     |                    |                      |                    |
     |                    |  {access_token, id_token, refresh_token} |
     |                    |<---------------------|                    |
     |                    |                      |                    |
     |                    |  Fetch user info     |                    |
     |                    |--------------------->|                    |
     |                    |                      |                    |
     |                    |  {email, name, picture, sub}             |
     |                    |<---------------------|                    |
     |                    |                      |                    |
     |                    |  CustomOAuth2UserService.loadUser()      |
     |                    |  Extract OAuth2UserInfo                  |
     |                    |  Save/update user in database            |
     |                    |----------+           |                    |
     |                    |<---------+           |                    |
     |                    |                      |                    |
     |                    |  OAuth2SuccessHandler.onAuthenticationSuccess()
     |                    |  Create internal JWT (access + refresh)  |
     |                    |  Set refresh token cookie                |
     |                    |  Build frontend redirect URL             |
     |                    |----------+           |                    |
     |                    |<---------+           |                    |
     |                    |                      |                    |
     |  HTTP 302 Redirect |                      |                    |
     |  Location: {frontendUrl}?token=eyJ...     |                    |
     |<-------------------|                      |                    |
     |                    |                      |                    |
     |  Browser follows redirect                 |                    |
     |---------------------------------------------------------------->|
     |                    |                      |                    |
     |                    |                      |       Extract token|
     |                    |                      |       from URL     |
     |                    |                      |       Store in app |
     |                    |                      |                    |
```

---

## Token Exchange Process

### OAuth2 Token Types

```text
+-----------------------------------------------------------------+
|                  Google OAuth2 Tokens                           |
+-----------------------------------------------------------------+
|                                                                 |
|  +---------------------------------------------------------+   |
|  |  Access Token (Google's)                                 |   |
|  |  - Used by Spring to call Google User Info endpoint      |   |
|  |  - NOT stored or exposed to frontend                     |   |
|  |  - Short-lived, single use for user info fetch           |   |
|  +---------------------------------------------------------+   |
|                                                                 |
|  +---------------------------------------------------------+   |
|  |  ID Token (Google's)                                     |   |
|  |  - Contains user identity claims                         |   |
|  |  - Parsed by Spring Security automatically               |   |
|  |  - NOT used directly by application                      |   |
|  +---------------------------------------------------------+   |
|                                                                 |
+-----------------------------------------------------------------+
                              |
                              v
+-----------------------------------------------------------------+
|                  Internal JWT Tokens                            |
+-----------------------------------------------------------------+
|                                                                 |
|  After successful OAuth2 authentication, the application       |
|  issues its own JWT tokens:                                     |
|                                                                 |
|  +---------------------------------------------------------+   |
|  |  Internal Access Token                                   |   |
|  |  - Signed with application's secret                      |   |
|  |  - Contains user email as subject                        |   |
|  |  - Returned to frontend via URL parameter                |   |
|  +---------------------------------------------------------+   |
|                                                                 |
|  +---------------------------------------------------------+   |
|  |  Internal Refresh Token                                  |   |
|  |  - Signed with application's refresh secret              |   |
|  |  - Stored in HttpOnly cookie                             |   |
|  |  - Same lifecycle as standard auth refresh token         |   |
|  +---------------------------------------------------------+   |
|                                                                 |
+-----------------------------------------------------------------+
```

### Code-to-Token Exchange

```text
+----------------------+                    +---------------------+
|   Spring Security    |                    |   Google Token API  |
|   OAuth2 Client      |                    |                     |
+----------+-----------+                    +----------+----------+
           |                                           |
           |  POST https://oauth2.googleapis.com/token |
           |  Content-Type: application/x-www-form-urlencoded
           |                                           |
           |  grant_type=authorization_code            |
           |  code=4/0AfJohXk...                       |
           |  redirect_uri=http://localhost:8080/login/oauth2/code/google
           |  client_id=xxx.apps.googleusercontent.com |
           |  client_secret=GOCSPX-xxx                 |
           |------------------------------------------>|
           |                                           |
           |  HTTP 200                                 |
           |  {                                        |
           |    "access_token": "ya29.xxx",            |
           |    "expires_in": 3599,                    |
           |    "scope": "openid email profile",       |
           |    "token_type": "Bearer",                |
           |    "id_token": "eyJhbGciOiJSUzI1NiIs..." |
           |  }                                        |
           |<------------------------------------------|
           |                                           |
```

---

## Principal Extraction

### OAuth2 User Info Flow

```text
+------------------------------------------------------------------+
|              CustomOAuth2UserService.loadUser()                  |
+------------------------------------------------------------------+
|                                                                  |
|  OAuth2UserRequest                                               |
|       |                                                          |
|       v                                                          |
|  +---------------------------------------------------------+    |
|  |  super.loadUser(userRequest)                            |    |
|  |  -> Calls Google User Info endpoint                     |    |
|  |  -> GET https://www.googleapis.com/oauth2/v3/userinfo   |    |
|  |  -> Returns OAuth2User with attributes                  |    |
|  +--------------------------+------------------------------+    |
|                             |                                    |
|                             v                                    |
|  +---------------------------------------------------------+    |
|  |  Extract registrationId ("google")                      |    |
|  |  Pass to OAuth2UserInfoFactory                          |    |
|  +--------------------------+------------------------------+    |
|                             |                                    |
|                             v                                    |
|  +---------------------------------------------------------+    |
|  |  OAuth2UserInfoFactory.getOAuth2UserInfo()              |    |
|  |  -> Creates provider-specific OAuth2UserInfo            |    |
|  |  -> GoogleOAuth2UserInfo extracts:                      |    |
|  |     - email                                             |    |
|  |     - name                                              |    |
|  |     - picture (avatar URL)                              |    |
|  |     - id (Google's sub claim)                           |    |
|  +--------------------------+------------------------------+    |
|                             |                                    |
|                             v                                    |
|  +---------------------------------------------------------+    |
|  |  userUtils.saveOrUpdateOAuthUser(userInfo, registrationId)  |
|  |  -> Find or create User entity                          |    |
|  |  -> Set authProvider = GOOGLE                           |    |
|  |  -> Update profile info if changed                      |    |
|  |  -> Save to PostgreSQL                                  |    |
|  +--------------------------+------------------------------+    |
|                             |                                    |
|                             v                                    |
|  +---------------------------------------------------------+    |
|  |  Return DefaultOAuth2User with:                         |    |
|  |  -> authorities (from local user roles)                 |    |
|  |  -> attributes (from Google)                            |    |
|  |  -> nameAttributeKey = "email"                          |    |
|  +---------------------------------------------------------+    |
|                                                                  |
+------------------------------------------------------------------+
```

### User Attribute Mapping

| Google Attribute | Internal Field     | Notes                           |
|------------------|--------------------|---------------------------------|
| `email`          | `User.email`       | Primary identifier              |
| `name`           | `User.username`    | Display name                    |
| `picture`        | `User.avatarUrl`   | Profile picture URL (if stored) |
| `sub`            | `User.oauthId`     | Google's unique user ID         |

---

## Integration with Internal JWT Issuing

### OAuth2AuthenticationSuccessHandler Flow

```text
+------------------------------------------------------------------+
|          OAuth2AuthenticationSuccessHandler                      |
+------------------------------------------------------------------+
|                                                                  |
|  onAuthenticationSuccess(request, response, authentication)      |
|       |                                                          |
|       v                                                          |
|  +---------------------------------------------------------+    |
|  |  Extract OAuth2User from authentication.getPrincipal()  |    |
|  +--------------------------+------------------------------+    |
|                             |                                    |
|                             v                                    |
|  +---------------------------------------------------------+    |
|  |  userService.processOAuth2User(principal)               |    |
|  |  -> Returns saved/updated User entity                   |    |
|  +--------------------------+------------------------------+    |
|                             |                                    |
|                             v                                    |
|  +---------------------------------------------------------+    |
|  |  userUtils.getUserAuthorities(savedUser)                |    |
|  |  -> Convert User.role to GrantedAuthority list          |    |
|  |  -> e.g., ROLE_AUTHOR, ROLE_READER                      |    |
|  +--------------------------+------------------------------+    |
|                             |                                    |
|                             v                                    |
|  +---------------------------------------------------------+    |
|  |  Create Spring Security UserDetails                     |    |
|  |  new User(email, "", authorities)                       |    |
|  +--------------------------+------------------------------+    |
|                             |                                    |
|                             v                                    |
|  +---------------------------------------------------------+    |
|  |  Create new Authentication object                       |    |
|  |  UsernamePasswordAuthenticationToken(userDetails,       |    |
|  |                                      null, authorities) |    |
|  |  Set in SecurityContextHolder                           |    |
|  +--------------------------+------------------------------+    |
|                             |                                    |
|                             v                                    |
|  +---------------------------------------------------------+    |
|  |  jwtTokenProvider.createAccessToken(newAuth)            |    |
|  |  jwtTokenProvider.createRefreshToken(newAuth)           |    |
|  +--------------------------+------------------------------+    |
|                             |                                    |
|                             v                                    |
|  +---------------------------------------------------------+    |
|  |  refreshCookieService.setRefreshTokenCookie(            |    |
|  |      refreshToken, response)                            |    |
|  |  -> Set-Cookie: refreshToken=eyJ...; HttpOnly; Secure   |    |
|  +--------------------------+------------------------------+    |
|                             |                                    |
|                             v                                    |
|  +---------------------------------------------------------+    |
|  |  response.sendRedirect(                                 |    |
|  |      "{frontendCallbackUrl}?token={accessToken}")       |    |
|  +---------------------------------------------------------+    |
|                                                                  |
+------------------------------------------------------------------+
```

### Auth Provider Unification

```text
+-----------------------------------------------------------------+
|                 Unified Authentication Flow                     |
+-----------------------------------------------------------------+
|                                                                 |
|  Standard Login                      OAuth2 Login               |
|       |                                   |                     |
|       v                                   v                     |
|  AuthService                    OAuth2SuccessHandler            |
|  .signInUser()                  .onAuthenticationSuccess()      |
|       |                                   |                     |
|       +----------------+------------------+                     |
|                        |                                        |
|                        v                                        |
|              +---------------------+                            |
|              |  JwtTokenProvider   |                            |
|              |  .createAccessToken |                            |
|              |  .createRefreshToken|                            |
|              +---------------------+                            |
|                        |                                        |
|                        v                                        |
|              Same JWT structure                                 |
|              Same validation logic                              |
|              Same refresh mechanism                             |
|                                                                 |
+-----------------------------------------------------------------+
```

---

## Configuration

### Application Properties

```properties
# OAuth2 Client Registration
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_AUTH_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_AUTH_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}

# OAuth2 Provider Configuration
spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/v2/auth
spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token
spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo

# Frontend Redirect
oauth.frontend.redirect.callback.url=${FRONTEND_REDIRECT_CALLBACK_URL}
```

### Security Config Integration

```java
.oauth2Login(oauth2 -> oauth2
    .userInfoEndpoint(userInfo -> userInfo
        .userService(customOAuth2UserService))    // Custom user processing
    .successHandler(oAuth2AuthenticationSuccessHandler)  // JWT issuance
    .failureHandler(oAuth2AuthenticationFailureHandler)) // Error handling
```

---

## Key Takeaways

1. **Bridge Pattern**: OAuth2 authenticates with Google, then issues internal JWTs
2. **No Google Token Storage**: Google's tokens are used once and discarded
3. **Unified Token Format**: OAuth2 and standard login produce identical JWTs
4. **User Provisioning**: OAuth2 users are auto-created or updated on each login
5. **Role Assignment**: OAuth2 users get default role (configured in UserUtils)
6. **Provider Tracking**: `authProvider` field distinguishes LOCAL vs GOOGLE users
7. **Frontend Integration**: Access token delivered via URL redirect parameter
8. **Cookie Consistency**: Refresh token handling identical across auth methods
