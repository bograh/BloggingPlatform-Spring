# Unauthenticated GET Endpoints

This document lists endpoints that can be accessed without authentication using `GET`, based on the current Spring Security configuration in `SecurityConfig`.

## 1) Explicit unauthenticated GET patterns

These are explicitly allowed by method + path rule:

- `GET /api/posts/**`
- `GET /api/tags/**`

Additionally, feed routes are public for all methods:

- `/api/feed/**`

## 2) Implemented controller GET endpoints covered by those patterns

### Posts (`/api/posts`)

- `GET /api/posts`
- `GET /api/posts/{postId}`
- `GET /api/posts/popular`
- `GET /api/posts/trending`

### Tags (`/api/tags`)

- `GET /api/tags/popular`

### Feed (`/api/feed`)

- `GET /api/feed`
- `GET /api/feed/trending/live`

### Comments (`/api/comments`)

Comment GET routes exist, but they are **not unauthenticated** under current security rules:

- `GET /api/comments/post/{postId}` *(authentication required)*
- `GET /api/comments/{commentId}` *(authentication required)*

## 3) Additional public paths that also allow GET

The following are in `PUBLIC_ENDPOINTS` (`permitAll` for any HTTP method), so `GET` is also unauthenticated:

- `/api/auth/**`
- `/oauth2/**`
- `/login/oauth2/**`
- `/graphql`
- `/graphiql`
- `/favicon.ico`
- `/actuator/health`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/error`

## Notes

- Access here means **authentication is not required** by Spring Security.
- `GET /api/comments/**` is not included in unauthenticated `GET` matchers, so it falls under `.anyRequest().authenticated()`.
- Route existence still depends on controller mappings and enabled modules.
- This file reflects the current config and controllers at the time of writing.
