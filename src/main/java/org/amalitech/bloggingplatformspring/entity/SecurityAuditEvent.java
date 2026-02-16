package org.amalitech.bloggingplatformspring.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entity for storing security audit events in MongoDB.
 * Tracks authentication attempts, token validations, and access control events.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "security_audit_events")
public class SecurityAuditEvent {

  @Id
  private String id;

  @Indexed
  private String eventType;

  @Indexed
  private String username;

  @Indexed
  private String email;

  @Indexed
  private String ipAddress;

  private String userAgent;

  private String endpoint;

  private String method;

  private String details;

  private boolean success;

  @Indexed
  private LocalDateTime timestamp;

  /**
   * Security event types
   */
  public enum EventType {
    SIGN_IN_SUCCESS,
    SIGN_IN_FAILURE,
    REGISTRATION_SUCCESS,
    REGISTRATION_FAILURE,
    TOKEN_VALIDATION_FAILURE,
    ACCESS_DENIED,
    RESTRICTED_ENDPOINT_ACCESS,
    BRUTE_FORCE_SUSPECTED,
    TOKEN_REFRESH,
    SIGN_OUT
  }
}
