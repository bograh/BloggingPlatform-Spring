package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.NotificationOutbox;
import org.amalitech.bloggingplatformspring.enums.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {

  List<NotificationOutbox> findByStatusOrderByCreatedAtAsc(NotificationStatus status, Pageable pageable);

  @Query("SELECT n FROM NotificationOutbox n WHERE n.status = :status OR " +
      "(n.status = :retryStatus AND n.nextRetryAt <= :now) ORDER BY n.createdAt ASC")
  List<NotificationOutbox> findPendingOrReadyToRetry(
      @Param("status") NotificationStatus status,
      @Param("retryStatus") NotificationStatus retryStatus,
      @Param("now") LocalDateTime now,
      Pageable pageable);

  long countByStatus(NotificationStatus status);

  @Query("SELECT COUNT(n) FROM NotificationOutbox n WHERE n.status = :status AND n.processedAt >= :since")
  long countByStatusAndProcessedAtAfter(
      @Param("status") NotificationStatus status,
      @Param("since") LocalDateTime since);

  @Modifying
  @Query("DELETE FROM NotificationOutbox n WHERE n.status = :status AND n.processedAt < :before")
  int deleteOldProcessedNotifications(
      @Param("status") NotificationStatus status,
      @Param("before") LocalDateTime before);
}
