package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.SecurityAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB Repository for SecurityAuditEvent entities.
 */
@Repository
public interface SecurityAuditEventRepository extends MongoRepository<SecurityAuditEvent, String> {

  /**
   * Find events by type
   */
  List<SecurityAuditEvent> findByEventType(String eventType);

  /**
   * Find events by IP address
   */
  List<SecurityAuditEvent> findByIpAddress(String ipAddress);

  /**
   * Find events by email
   */
  List<SecurityAuditEvent> findByEmail(String email);

  /**
   * Find events by email and event type
   */
  List<SecurityAuditEvent> findByEmailAndEventType(String email, String eventType);

  /**
   * Find failed sign-in attempts for an IP address within a time window
   */
  List<SecurityAuditEvent> findByIpAddressAndEventTypeAndTimestampAfter(
      String ipAddress, String eventType, LocalDateTime timestamp);

  /**
   * Find failed sign-in attempts for an email within a time window
   */
  List<SecurityAuditEvent> findByEmailAndEventTypeAndTimestampAfter(
      String email, String eventType, LocalDateTime timestamp);

  /**
   * Count failed attempts by IP address within a time window
   */
  long countByIpAddressAndEventTypeAndTimestampAfter(
      String ipAddress, String eventType, LocalDateTime timestamp);

  /**
   * Count failed attempts by email within a time window
   */
  long countByEmailAndEventTypeAndTimestampAfter(
      String email, String eventType, LocalDateTime timestamp);

  /**
   * Find events within a time range
   */
  List<SecurityAuditEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

  /**
   * Find events by type with pagination
   */
  Page<SecurityAuditEvent> findByEventType(String eventType, Pageable pageable);

  /**
   * Find all events with pagination
   */
  Page<SecurityAuditEvent> findAllByOrderByTimestampDesc(Pageable pageable);

  /**
   * Find recent brute force suspected events
   */
  List<SecurityAuditEvent> findByEventTypeAndTimestampAfterOrderByTimestampDesc(
      String eventType, LocalDateTime timestamp);
}
