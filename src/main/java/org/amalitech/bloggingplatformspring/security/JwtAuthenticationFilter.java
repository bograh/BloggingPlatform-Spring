package org.amalitech.bloggingplatformspring.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_PREFIX = "/api/auth/";
    private static final String OAUTH2_PREFIX = "/oauth2/";
    private static final String LOGIN_OAUTH2_PREFIX = "/login/oauth2/";
    private static final String SWAGGER_PREFIX = "/swagger-ui/";
    private static final String API_DOCS_PREFIX = "/v3/api-docs/";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserUtils userUtils;
    private final TokenSessionService tokenSessionService;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        String path = request.getRequestURI();
        return path.startsWith(AUTH_PREFIX)
                || path.startsWith(OAUTH2_PREFIX)
                || path.startsWith(LOGIN_OAUTH2_PREFIX)
                || path.startsWith(SWAGGER_PREFIX)
                || path.startsWith(API_DOCS_PREFIX)
                || "/graphql".equals(path)
                || "/graphiql".equals(path)
                || "/favicon.ico".equals(path)
                || "/actuator/health".equals(path)
                || "/swagger-ui.html".equals(path)
                || "/error".equals(path);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = jwtTokenProvider.parseAccessToken(token);

            if (claims != null && !tokenSessionService.isTokenRevoked(token)) {
                String email = claims.getSubject();
                List<String> roles = userUtils.extractRoles(claims);
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        return null;
    }
}