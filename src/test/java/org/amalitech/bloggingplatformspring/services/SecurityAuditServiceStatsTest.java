package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.entity.SecurityAuditEvent.EventType;
import org.amalitech.bloggingplatformspring.repository.SecurityAuditEventRepository;
import org.amalitech.bloggingplatformspring.repository.SecurityEventTypeCountProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAuditServiceStatsTest {

    @Mock
    private SecurityAuditEventRepository auditEventRepository;

    @InjectMocks
    private SecurityAuditService securityAuditService;

    @Test
    void getSecurityStats_shouldUseAggregatedCountsAndAvoidLoadingEventLists() {
        when(auditEventRepository.countEventsByTypeSince(any()))
                .thenReturn(List.of(
                        projection(EventType.SIGN_IN_FAILURE.name(), 7L),
                        projection(EventType.ACCESS_DENIED.name(), 2L)));
        when(auditEventRepository.countByEventTypeAndTimestampAfter(anyString(), any()))
                .thenReturn(3L);

        Map<String, Object> stats = securityAuditService.getSecurityStats();

        assertEquals(7L, stats.get("sign_in_failure_24h"));
        assertEquals(2L, stats.get("access_denied_24h"));
        assertEquals(0L, stats.get("sign_in_success_24h"));
        assertEquals(3L, stats.get("recentBruteForceAttempts"));

        verify(auditEventRepository, never())
                .findByEventTypeAndTimestampAfterOrderByTimestampDesc(anyString(), any());
    }

    private SecurityEventTypeCountProjection projection(String eventType, long count) {
        return new SecurityEventTypeCountProjection() {
            @Override
            public String getEventType() {
                return eventType;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }
}
