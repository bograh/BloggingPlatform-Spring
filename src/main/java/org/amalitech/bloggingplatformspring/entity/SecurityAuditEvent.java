package org.amalitech.bloggingplatformspring.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
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
@CompoundIndexes({
        @CompoundIndex(name = "idx_event_type_timestamp", def = "{'eventType': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_ip_event_type_timestamp", def = "{'ipAddress': 1, 'eventType': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_email_event_type_timestamp", def = "{'email': 1, 'eventType': 1, 'timestamp': -1}")
})
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
