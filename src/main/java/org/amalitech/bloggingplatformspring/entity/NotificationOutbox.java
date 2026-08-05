package org.amalitech.bloggingplatformspring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.amalitech.bloggingplatformspring.enums.NotificationStatus;
import org.amalitech.bloggingplatformspring.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a notification in the outbox pattern.
 * Notifications are queued here and processed asynchronously.
 */
@Entity
@Table(name = "notification_outbox", indexes = {
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationOutbox {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "recipient_email", nullable = false)
  private String recipientEmail;

  @Column(name = "recipient_name")
  private String recipientName;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false)
  private NotificationType notificationType;

  @Column(nullable = false)
  private String subject;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationStatus status = NotificationStatus.PENDING;

  @Column(name = "retry_count")
  private int retryCount = 0;

  @Column(name = "max_retries")
  private int maxRetries = 3;

  @Column(name = "last_error", columnDefinition = "TEXT")
  private String lastError;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  @Column(name = "next_retry_at")
  private LocalDateTime nextRetryAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  public boolean canRetry() {
    return retryCount < maxRetries;
  }

  public void incrementRetryCount() {
    this.retryCount++;
  }
}
