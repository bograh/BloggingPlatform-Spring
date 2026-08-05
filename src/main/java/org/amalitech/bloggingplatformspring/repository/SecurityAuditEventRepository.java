package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.SecurityAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB Repository for SecurityAuditEvent entities.
 */
@Repository
public interface SecurityAuditEventRepository extends MongoRepository<SecurityAuditEvent, String> {

    List<SecurityAuditEvent> findByEventType(String eventType);

    List<SecurityAuditEvent> findByIpAddress(String ipAddress);

    List<SecurityAuditEvent> findByEmail(String email);

    List<SecurityAuditEvent> findByEmailAndEventType(String email, String eventType);

    List<SecurityAuditEvent> findByIpAddressAndEventTypeAndTimestampAfter(
            String ipAddress, String eventType, LocalDateTime timestamp);

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

    Page<SecurityAuditEvent> findByEventType(String eventType, Pageable pageable);

    Page<SecurityAuditEvent> findAllByOrderByTimestampDesc(Pageable pageable);

    /**
     * Find recent brute force suspected events
     */
    List<SecurityAuditEvent> findByEventTypeAndTimestampAfterOrderByTimestampDesc(
            String eventType, LocalDateTime timestamp);

    /**
     * Count events by event type since timestamp using a single aggregation query
     */
    @Aggregation(pipeline = {
            "{ '$match': { 'timestamp': { '$gte': ?0 } } }",
            "{ '$group': { '_id': '$eventType', 'count': { '$sum': 1 } } }",
            "{ '$project': { '_id': 0, 'eventType': '$_id', 'count': 1 } }"
    })
    List<SecurityEventTypeCountProjection> countEventsByTypeSince(LocalDateTime timestamp);

    /**
     * Count events by type in time window
     */
    long countByEventTypeAndTimestampAfter(String eventType, LocalDateTime timestamp);
}
