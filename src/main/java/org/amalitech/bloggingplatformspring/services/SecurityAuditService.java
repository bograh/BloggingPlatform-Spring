package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.entity.SecurityAuditEvent;
import org.amalitech.bloggingplatformspring.entity.SecurityAuditEvent.EventType;
import org.amalitech.bloggingplatformspring.repository.SecurityAuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for managing security audit events, tracking failed attempts,
 * and detecting brute force patterns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAuditService {

  private final SecurityAuditEventRepository auditEventRepository;

  // In-memory cache for quick brute force detection
  private final ConcurrentHashMap<String, AtomicInteger> failedAttemptsByIp = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicInteger> failedAttemptsByEmail = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, LocalDateTime> lastFailedAttemptTime = new ConcurrentHashMap<>();

  // Thresholds for brute force detection
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final int BRUTE_FORCE_WINDOW_MINUTES = 15;
  private static final int RAPID_ATTEMPT_THRESHOLD_SECONDS = 2;

  /**
   * Log a successful sign-in attempt
   */
  @Async("applicationTaskExecutor")
  public CompletableFuture<Void> logSuccessfulSignIn(String email, String ipAddress, String userAgent) {
    SecurityAuditEvent event = SecurityAuditEvent.builder()
        .eventType(EventType.SIGN_IN_SUCCESS.name())
        .email(email)
        .ipAddress(ipAddress)
        .userAgent(userAgent)
        .success(true)
        .details("User successfully signed in")
        .timestamp(LocalDateTime.now())
        .build();

    auditEventRepository.save(event);
    log.info("[SECURITY] Successful sign-in: email={}, ip={}", maskEmail(email), maskIp(ipAddress));

    // Reset failed attempts on successful login
    resetFailedAttempts(email, ipAddress);
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Log a failed sign-in attempt
   */
  @Async("applicationTaskExecutor")
  public CompletableFuture<Void> logFailedSignIn(String email, String ipAddress, String userAgent, String reason) {
    SecurityAuditEvent event = SecurityAuditEvent.builder()
        .eventType(EventType.SIGN_IN_FAILURE.name())
        .email(email)
        .ipAddress(ipAddress)
        .userAgent(userAgent)
        .success(false)
        .details(reason)
        .timestamp(LocalDateTime.now())
        .build();

    auditEventRepository.save(event);
    log.warn("[SECURITY] Failed sign-in attempt: email={}, ip={}, reason={}",
        maskEmail(email), maskIp(ipAddress), reason);

    // Track failed attempts
    trackFailedAttempt(email, ipAddress, userAgent);
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Log a token validation failure
   */
  @Async("applicationTaskExecutor")
  public CompletableFuture<Void> logTokenValidationFailure(String ipAddress, String userAgent, String endpoint,
      String reason) {
    SecurityAuditEvent event = SecurityAuditEvent.builder()
        .eventType(EventType.TOKEN_VALIDATION_FAILURE.name())
        .ipAddress(ipAddress)
        .userAgent(userAgent)
        .endpoint(endpoint)
        .success(false)
        .details(reason)
        .timestamp(LocalDateTime.now())
        .build();

    auditEventRepository.save(event);
    log.warn("[SECURITY] Token validation failure: ip={}, endpoint={}, reason={}",
        maskIp(ipAddress), endpoint, reason);
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Log access to restricted endpoint
   */
  @Async("applicationTaskExecutor")
  public CompletableFuture<Void> logRestrictedEndpointAccess(String email, String ipAddress, String userAgent,
      String endpoint, String method, boolean success) {
    SecurityAuditEvent event = SecurityAuditEvent.builder()
        .eventType(EventType.RESTRICTED_ENDPOINT_ACCESS.name())
        .email(email)
        .ipAddress(ipAddress)
        .userAgent(userAgent)
        .endpoint(endpoint)
        .method(method)
        .success(success)
        .details(success ? "Access granted" : "Access denied")
        .timestamp(LocalDateTime.now())
        .build();

    auditEventRepository.save(event);
    if (success) {
      log.info("[SECURITY] Restricted endpoint access: email={}, endpoint={} {}, status=GRANTED",
          maskEmail(email), method, endpoint);
    } else {
      log.warn("[SECURITY] Restricted endpoint access: email={}, endpoint={} {}, status=DENIED",
          maskEmail(email), method, endpoint);
    }
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Log access denied events
   */
  @Async("applicationTaskExecutor")
  public CompletableFuture<Void> logAccessDenied(String email, String ipAddress, String userAgent,
      String endpoint, String method) {
    SecurityAuditEvent event = SecurityAuditEvent.builder()
        .eventType(EventType.ACCESS_DENIED.name())
        .email(email)
        .ipAddress(ipAddress)
        .userAgent(userAgent)
        .endpoint(endpoint)
        .method(method)
        .success(false)
        .details("Access denied - insufficient permissions")
        .timestamp(LocalDateTime.now())
        .build();

    auditEventRepository.save(event);
    log.warn("[SECURITY] Access denied: email={}, ip={}, endpoint={} {}",
        maskEmail(email), maskIp(ipAddress), method, endpoint);
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Track and detect brute force patterns
   */
  private void trackFailedAttempt(String email, String ipAddress, String userAgent) {
    LocalDateTime now = LocalDateTime.now();

    // Track by IP
    AtomicInteger ipAttempts = failedAttemptsByIp.computeIfAbsent(ipAddress, k -> new AtomicInteger(0));
    int ipCount = ipAttempts.incrementAndGet();

    // Track by email if provided
    int emailCount = 0;
    if (email != null && !email.isEmpty()) {
      AtomicInteger emailAttempts = failedAttemptsByEmail.computeIfAbsent(email, k -> new AtomicInteger(0));
      emailCount = emailAttempts.incrementAndGet();
    }

    // Check for rapid attempts (potential automated attack)
    String attemptKey = ipAddress + ":" + (email != null ? email : "unknown");
    AtomicBoolean rapidAttempt = new AtomicBoolean(false);
    lastFailedAttemptTime.compute(attemptKey, (key, lastAttempt) -> {
      if (lastAttempt != null &&
          java.time.Duration.between(lastAttempt, now).getSeconds() < RAPID_ATTEMPT_THRESHOLD_SECONDS) {
        rapidAttempt.set(true);
      }
      return now;
    });

    // Check database for historical failed attempts
    long dbFailedCount = checkDatabaseFailedAttempts(email, ipAddress);

    // Detect brute force
    if (ipCount >= MAX_FAILED_ATTEMPTS || emailCount >= MAX_FAILED_ATTEMPTS ||
        dbFailedCount >= MAX_FAILED_ATTEMPTS || rapidAttempt.get()) {
      logBruteForceWarning(email, ipAddress, userAgent, ipCount, emailCount, rapidAttempt.get());
    }
  }

  /**
   * Check database for recent failed attempts
   */
  private long checkDatabaseFailedAttempts(String email, String ipAddress) {
    LocalDateTime windowStart = LocalDateTime.now().minusMinutes(BRUTE_FORCE_WINDOW_MINUTES);

    long ipFailures = auditEventRepository.countByIpAddressAndEventTypeAndTimestampAfter(
        ipAddress, EventType.SIGN_IN_FAILURE.name(), windowStart);

    long emailFailures = 0;
    if (email != null && !email.isEmpty()) {
      emailFailures = auditEventRepository.countByEmailAndEventTypeAndTimestampAfter(
          email, EventType.SIGN_IN_FAILURE.name(), windowStart);
    }

    return Math.max(ipFailures, emailFailures);
  }

  /**
   * Log a brute force warning
   */
  private void logBruteForceWarning(String email, String ipAddress, String userAgent,
      int ipCount, int emailCount, boolean rapidAttempt) {
    String details = String.format(
        "Potential brute force attack detected - IP attempts: %d, Email attempts: %d, Rapid attempts: %s",
        ipCount, emailCount, rapidAttempt);

    SecurityAuditEvent event = SecurityAuditEvent.builder()
        .eventType(EventType.BRUTE_FORCE_SUSPECTED.name())
        .email(email)
        .ipAddress(ipAddress)
        .userAgent(userAgent)
        .success(false)
        .details(details)
        .timestamp(LocalDateTime.now())
        .build();

    auditEventRepository.save(event);
    log.error(
        "[SECURITY ALERT] Potential brute force attack: ip={}, email={}, ip_attempts={}, email_attempts={}, rapid={}",
        ipAddress, maskEmail(email), ipCount, emailCount, rapidAttempt);
  }

  /**
   * Reset failed attempts after successful login
   */
  private void resetFailedAttempts(String email, String ipAddress) {
    failedAttemptsByIp.remove(ipAddress);
    if (email != null) {
      failedAttemptsByEmail.remove(email);
    }
    String attemptKey = ipAddress + ":" + (email != null ? email : "unknown");
    lastFailedAttemptTime.remove(attemptKey);
  }

  /**
   * Check if an IP is currently blocked due to brute force attempts
   */
  public boolean isIpBlocked(String ipAddress) {
    AtomicInteger attempts = failedAttemptsByIp.get(ipAddress);
    if (attempts != null && attempts.get() >= MAX_FAILED_ATTEMPTS) {
      return true;
    }

    // Also check database
    LocalDateTime windowStart = LocalDateTime.now().minusMinutes(BRUTE_FORCE_WINDOW_MINUTES);
    long dbFailures = auditEventRepository.countByIpAddressAndEventTypeAndTimestampAfter(
        ipAddress, EventType.SIGN_IN_FAILURE.name(), windowStart);

    return dbFailures >= MAX_FAILED_ATTEMPTS;
  }

  /**
   * Get security event statistics
   */
  public Map<String, Object> getSecurityStats() {
    LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
    LocalDateTime lastHour = LocalDateTime.now().minusHours(1);

    Map<String, Object> stats = new ConcurrentHashMap<>();

    // Count events by type in last 24 hours
    for (EventType type : EventType.values()) {
      long count = auditEventRepository.countByIpAddressAndEventTypeAndTimestampAfter(
          null, type.name(), last24Hours);
      stats.put(type.name().toLowerCase() + "_24h", count);
    }

    // Recent brute force attempts
    List<SecurityAuditEvent> recentBruteForce = auditEventRepository
        .findByEventTypeAndTimestampAfterOrderByTimestampDesc(
            EventType.BRUTE_FORCE_SUSPECTED.name(), lastHour);
    stats.put("recentBruteForceAttempts", recentBruteForce.size());

    // Currently tracked IPs with failed attempts
    stats.put("trackedFailedIps", failedAttemptsByIp.size());
    stats.put("trackedFailedEmails", failedAttemptsByEmail.size());

    return stats;
  }

  /**
   * Get recent security events with pagination
   */
  public Page<SecurityAuditEvent> getRecentEvents(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return auditEventRepository.findAllByOrderByTimestampDesc(pageable);
  }

  /**
   * Get events by type with pagination
   */
  public Page<SecurityAuditEvent> getEventsByType(String eventType, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return auditEventRepository.findByEventType(eventType, pageable);
  }

  /**
   * Mask email for logging (privacy)
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
   * Mask IP for logging (partial privacy)
   */
  private String maskIp(String ip) {
    if (ip == null || ip.isEmpty()) {
      return "[unknown]";
    }
    if (ip.contains(".")) {
      // IPv4
      String[] parts = ip.split("\\.");
      if (parts.length == 4) {
        return parts[0] + "." + parts[1] + ".xxx.xxx";
      }
    }
    return ip.substring(0, Math.min(ip.length(), 8)) + "...";
  }

  /**
   * Clear in-memory tracking caches (for maintenance/testing)
   */
  public void clearTrackingCaches() {
    failedAttemptsByIp.clear();
    failedAttemptsByEmail.clear();
    lastFailedAttemptTime.clear();
    log.info("[SECURITY] Tracking caches cleared");
  }
}
