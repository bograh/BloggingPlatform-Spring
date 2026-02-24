package org.amalitech.bloggingplatformspring.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.amalitech.bloggingplatformspring.enums.ModerationAction;
import org.amalitech.bloggingplatformspring.enums.ModerationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for moderation task status and results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationTaskDTO {

  private UUID taskId;
  private ModerationAction action;
  private ModerationStatus status;
  private int totalComments;
  private int processedComments;
  private int failedComments;
  private double progressPercentage;
  private String errorDetails;
  private LocalDateTime createdAt;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
  private long estimatedRemainingMs;
}
