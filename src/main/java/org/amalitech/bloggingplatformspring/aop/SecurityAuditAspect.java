package org.amalitech.bloggingplatformspring.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.services.SecurityAuditService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect for security event auditing.
 * Logs authentication attempts, token validation failures, and access to
 * restricted endpoints.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAuditAspect {

    // List of restricted endpoint patterns
    private static final String[] RESTRICTED_PATTERNS = {
            "/api/admin",
            "/api/users",
            "/api/metrics"
    };
    private final SecurityAuditService securityAuditService;

    /**
     * Pointcut for sign-in method in AuthService
     */
    @Pointcut("execution(* org.amalitech.bloggingplatformspring.services.AuthService.signInUser(..))")
    public void signInMethod() {
    }

    /**
     * Pointcut for registration method in AuthService
     */
    @Pointcut("execution(* org.amalitech.bloggingplatformspring.services.AuthService.registerUser(..))")
    public void registerMethod() {
    }

    /**
     * Pointcut for JWT token validation
     */
    @Pointcut("execution(* org.amalitech.bloggingplatformspring.security.JwtTokenProvider.validAccessToken(..))")
    public void tokenValidationMethod() {
    }

    /**
     * Pointcut for all controller methods (to track restricted endpoint access)
     */
    @Pointcut("execution(* org.amalitech.bloggingplatformspring.controllers..*(..))")
    public void controllerMethods() {
    }

    /**
     * Pointcut for admin endpoints
     */
    @Pointcut("execution(* org.amalitech.bloggingplatformspring.controllers.UserController.*(..)) || " +
            "execution(* org.amalitech.bloggingplatformspring.controllers.PerformanceMetricsController.*(..))")
    public void restrictedEndpoints() {
    }

    /**
     * Around advice for sign-in attempts
     */
    @Around("signInMethod()")
    public Object auditSignIn(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String email = null;

        // Extract email from SignInUserDTO
        if (args.length > 0 && args[0] instanceof SignInUserDTO signInDTO) {
            email = signInDTO.getEmail();
        }

        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);

        try {
            Object result = joinPoint.proceed();

            // Log successful sign-in
            securityAuditService.logSuccessfulSignIn(email, ipAddress, userAgent);

            return result;
        } catch (Exception e) {
            // Log failed sign-in
            securityAuditService.logFailedSignIn(email, ipAddress, userAgent, e.getMessage());
            throw e;
        }
    }

    /**
     * After returning advice for successful registration
     */
    @AfterReturning("registerMethod()")
    public void auditSuccessfulRegistration(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String email = null;

        if (args.length > 0 && args[0] instanceof RegisterUserDTO registerDTO) {
            email = registerDTO.getEmail();
        }

        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIp(request);

        log.info("[SECURITY] Successful registration: email={}, ip={}",
                maskEmail(email), maskIp(ipAddress));
    }

    /**
     * After throwing advice for failed registration
     */
    @AfterThrowing(pointcut = "registerMethod()", throwing = "exception")
    public void auditFailedRegistration(JoinPoint joinPoint, Throwable exception) {
        Object[] args = joinPoint.getArgs();
        String email = null;

        if (args.length > 0 && args[0] instanceof RegisterUserDTO registerDTO) {
            email = registerDTO.getEmail();
        }

        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIp(request);

        log.warn("[SECURITY] Failed registration: email={}, ip={}, reason={}",
                maskEmail(email), maskIp(ipAddress), exception.getMessage());
    }

    /**
     * After returning advice for token validation - track failures
     */
    @AfterReturning(pointcut = "tokenValidationMethod()", returning = "isValid")
    public void auditTokenValidation(JoinPoint joinPoint, boolean isValid) {
        if (!isValid) {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                String ipAddress = getClientIp(request);
                String userAgent = getUserAgent(request);
                String endpoint = request.getRequestURI();

                securityAuditService.logTokenValidationFailure(
                        ipAddress, userAgent, endpoint, "Invalid or expired token");
            }
        }
    }

    /**
     * Before advice for restricted endpoint access
     */
    @Before("restrictedEndpoints()")
    public void auditRestrictedEndpointAccess(JoinPoint joinPoint) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return;
        }

        String endpoint = request.getRequestURI();
        String method = request.getMethod();
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);

        // Check if endpoint is restricted
        boolean isRestricted = isRestrictedEndpoint(endpoint);
        if (!isRestricted) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = null;
        boolean hasAccess = false;

        if (authentication != null && authentication.isAuthenticated()) {
            email = authentication.getName();
            hasAccess = true;
        }

        securityAuditService.logRestrictedEndpointAccess(
                email, ipAddress, userAgent, endpoint, method, hasAccess);
    }

    /**
     * Check if endpoint matches restricted patterns
     */
    private boolean isRestrictedEndpoint(String endpoint) {
        if (endpoint == null) {
            return false;
        }
        for (String pattern : RESTRICTED_PATTERNS) {
            if (endpoint.startsWith(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get current HTTP request
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.debug("Could not get current request: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

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

    /**
     * Get user agent
     */
    private String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }

    /**
     * Mask email for logging
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "[unknown]";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * Mask IP for logging
     */
    private String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "[unknown]";
        }
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".xxx.xxx";
            }
        }
        return ip.substring(0, Math.min(ip.length(), 8)) + "...";
    }
}