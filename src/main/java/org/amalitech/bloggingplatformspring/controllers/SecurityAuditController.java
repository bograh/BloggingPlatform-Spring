package org.amalitech.bloggingplatformspring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.responses.StatusResponse;
import org.amalitech.bloggingplatformspring.entity.SecurityAuditEvent;
import org.amalitech.bloggingplatformspring.services.SecurityAuditService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for security audit endpoints.
 * Provides endpoints to view security events and statistics.
 */
@RestController
@RequestMapping("/api/security/audit")
@RequiredArgsConstructor
@Tag(name = "6. Security Audit", description = "APIs for viewing security audit events and statistics")
public class SecurityAuditController {

  private final SecurityAuditService securityAuditService;

  /**
   * Get security statistics
   */
  @GetMapping("/stats")
  @Operation(summary = "Get security statistics", description = "Retrieves security event statistics including counts by event type")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
  })
  public ResponseEntity<Map<String, Object>> getSecurityStats() {
    return ResponseEntity.ok(securityAuditService.getSecurityStats());
  }

  /**
   * Get recent security events
   */
  @GetMapping("/events")
  @Operation(summary = "Get recent security events", description = "Retrieves recent security audit events with pagination")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
  })
  public ResponseEntity<Page<SecurityAuditEvent>> getRecentEvents(
      @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(securityAuditService.getRecentEvents(page, size));
  }

  /**
   * Get security events by type
   */
  @GetMapping("/events/{eventType}")
  @Operation(summary = "Get events by type", description = "Retrieves security audit events filtered by event type")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
  })
  public ResponseEntity<Page<SecurityAuditEvent>> getEventsByType(
      @Parameter(description = "Event type (SIGN_IN_SUCCESS, SIGN_IN_FAILURE, TOKEN_VALIDATION_FAILURE, ACCESS_DENIED, BRUTE_FORCE_SUSPECTED)") @PathVariable String eventType,
      @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(securityAuditService.getEventsByType(eventType, page, size));
  }

  /**
   * Check if an IP is blocked
   */
  @GetMapping("/blocked/{ipAddress}")
  @Operation(summary = "Check if IP is blocked", description = "Checks if an IP address is currently blocked due to brute force attempts")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Check completed successfully")
  })
  public ResponseEntity<Map<String, Object>> isIpBlocked(
      @Parameter(description = "IP address to check") @PathVariable String ipAddress) {
    boolean blocked = securityAuditService.isIpBlocked(ipAddress);
    return ResponseEntity.ok(Map.of(
        "ipAddress", ipAddress,
        "blocked", blocked));
  }

  /**
   * Clear tracking caches (admin only)
   */
  @DeleteMapping("/tracking/clear")
  @Operation(summary = "Clear tracking caches", description = "Clears in-memory tracking caches for failed attempts (admin only)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Caches cleared successfully")
  })
  public ResponseEntity<StatusResponse> clearTrackingCaches() {
    securityAuditService.clearTrackingCaches();
    return ResponseEntity.ok(StatusResponse.success("Tracking caches cleared"));
  }
}
