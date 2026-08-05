package org.amalitech.bloggingplatformspring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.amalitech.bloggingplatformspring.enums.ModerationAction;
import org.amalitech.bloggingplatformspring.enums.ModerationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a bulk moderation task.
 * Tracks async processing of comment moderation jobs.
 */
@Entity
@Table(name = "moderation_tasks", indexes = {
    @Index(name = "idx_moderation_status", columnList = "status"),
    @Index(name = "idx_moderation_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModerationTask {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "moderator_id", nullable = false)
  private UUID moderatorId;

  @Column(name = "moderator_username", nullable = false)
  private String moderatorUsername;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ModerationAction action;

  @Column(name = "comment_ids", columnDefinition = "TEXT", nullable = false)
  private String commentIds;

  @Column(name = "total_comments")
  private int totalComments;

  @Column(name = "processed_comments")
  private int processedComments = 0;

  @Column(name = "failed_comments")
  private int failedComments = 0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ModerationStatus status = ModerationStatus.QUEUED;

  @Column(name = "error_details", columnDefinition = "TEXT")
  private String errorDetails;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  public boolean isComplete() {
    return status == ModerationStatus.COMPLETED || status == ModerationStatus.FAILED;
  }

  public double getProgressPercentage() {
    if (totalComments == 0) {
      return 0.0;
    }
    return (processedComments * 100.0) / totalComments;
  }

  public void incrementProcessed() {
    this.processedComments++;
  }

  public void incrementFailed() {
    this.failedComments++;
  }
}
