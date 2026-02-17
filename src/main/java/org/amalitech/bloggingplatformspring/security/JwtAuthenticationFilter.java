package org.amalitech.bloggingplatformspring.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.exceptions.ErrorResponse;
import org.amalitech.bloggingplatformspring.services.CustomUserDetailsService;
import org.amalitech.bloggingplatformspring.services.SecurityAuditService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;
    private final TokenSessionService tokenSessionService;
    private final SecurityAuditService securityAuditService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = getTokenFromAuthorizationHeader(request);

        if (StringUtils.hasText(token)) {
            String ipAddress = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String endpoint = request.getRequestURI();

            if (tokenSessionService.isTokenRevoked(token)) {
                // Log revoked token attempt
                securityAuditService.logTokenValidationFailure(
                        ipAddress, userAgent, endpoint, "Token has been revoked");

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");

                ErrorResponse errorResponse = new ErrorResponse(
                        "UNAUTHORIZED",
                        "Invalid or expired token",
                        HttpServletResponse.SC_UNAUTHORIZED);

                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }

            if (jwtTokenProvider.validAccessToken(token)) {
                String email = jwtTokenProvider.getEmailFromAccessToken(token);

                tokenSessionService.updateSessionActivity(email);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // Log invalid token attempt
                securityAuditService.logTokenValidationFailure(
                        ipAddress, userAgent, endpoint, "Invalid or expired access token");
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String getTokenFromAuthorizationHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}