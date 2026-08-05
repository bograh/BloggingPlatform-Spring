package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.requests.BulkModerationRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.ModerationTaskDTO;
import org.amalitech.bloggingplatformspring.entity.ModerationTask;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.enums.ModerationAction;
import org.amalitech.bloggingplatformspring.enums.ModerationStatus;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.ModerationTaskRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for bulk asynchronous comment moderation.
 * Processes moderation actions on multiple comments concurrently.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BulkCommentModerationService {

  private static final int BATCH_SIZE = 50;
  private static final int MAX_COMMENTS_PER_TASK = 1000;
  private static final long ESTIMATED_MS_PER_COMMENT = 50;

  private final CommentRepository commentRepository;
  private final ModerationTaskRepository moderationTaskRepository;

  /**
   * Submits a bulk moderation task for async processing.
   *
   * @param request   moderation request with comment IDs and action
   * @param moderator the user performing moderation
   * @return task DTO with tracking info
   */
  @Transactional
  public ModerationTaskDTO submitModerationTask(BulkModerationRequest request, User moderator) {
    validateRequest(request);

    ModerationTask task = createModerationTask(request, moderator);
    ModerationTask savedTask = moderationTaskRepository.save(task);

    processTaskAsync(savedTask.getId());

    return mapToDTO(savedTask);
  }

  /**
   * Gets the status of a moderation task.
   *
   * @param taskId task UUID
   * @return task status DTO
   */
  public ModerationTaskDTO getTaskStatus(UUID taskId) {
    ModerationTask task = moderationTaskRepository.findById(taskId)
        .orElseThrow(() -> new ResourceNotFoundException("Moderation task not found: " + taskId));
    return mapToDTO(task);
  }

  /**
   * Gets recent moderation tasks for a moderator.
   *
   * @param moderatorId moderator UUID
   * @param limit       max tasks to return
   * @return list of task DTOs
   */
  public List<ModerationTaskDTO> getModeratorTasks(UUID moderatorId, int limit) {
    int effectiveLimit = Math.min(Math.max(limit, 1), 100);
    List<ModerationTask> tasks = moderationTaskRepository
        .findByModeratorIdOrderByCreatedAtDesc(moderatorId, PageRequest.of(0, effectiveLimit));
    return tasks.stream().map(this::mapToDTO).toList();
  }

  /**
   * Processes a moderation task asynchronously.
   *
   * @param taskId task UUID to process
   */
  @Async("applicationTaskExecutor")
  public CompletableFuture<Void> processTaskAsync(UUID taskId) {
    log.info("Starting async processing of moderation task: {}", taskId);

    try {
      ModerationTask task = moderationTaskRepository.findById(taskId)
          .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

      updateTaskStatus(task, ModerationStatus.PROCESSING);

      String[] commentIds = task.getCommentIds().split(",");
      processCommentsInBatches(task, commentIds);

      updateTaskStatus(task, ModerationStatus.COMPLETED);
      log.info("Completed moderation task: {} - processed: {}, failed: {}",
          taskId, task.getProcessedComments(), task.getFailedComments());

    } catch (Exception e) {
      log.error("Error processing moderation task {}: {}", taskId, e.getMessage(), e);
      handleTaskError(taskId, e.getMessage());
    }

    return CompletableFuture.completedFuture(null);
  }

  private void processCommentsInBatches(ModerationTask task, String[] commentIds) {
    for (int i = 0; i < commentIds.length; i += BATCH_SIZE) {
      int end = Math.min(i + BATCH_SIZE, commentIds.length);
      processBatch(task, commentIds, i, end);
      updateTaskProgress(task);
    }
  }

  private void processBatch(ModerationTask task, String[] commentIds, int start, int end) {
    for (int i = start; i < end; i++) {
      String commentId = commentIds[i].trim();
      if (commentId.isEmpty()) {
        continue;
      }

      try {
        executeModerationAction(commentId, task.getAction());
        task.incrementProcessed();
      } catch (Exception e) {
        log.warn("Failed to moderate comment {}: {}", commentId, e.getMessage());
        task.incrementFailed();
      }
    }
  }

  private void executeModerationAction(String commentId, ModerationAction action) {
    switch (action) {
      case APPROVE -> approveComment(commentId);
      case REJECT, DELETE -> deleteComment(commentId);
      case FLAG_SPAM -> flagCommentAsSpam(commentId);
      case SKIP -> {
        /* No action needed */ }
    }
  }

  private void approveComment(String commentId) {
    log.debug("Approving comment: {}", commentId);
  }

  private void deleteComment(String commentId) {
    commentRepository.deleteCommentById(commentId);
    log.debug("Deleted comment: {}", commentId);
  }

  private void flagCommentAsSpam(String commentId) {
    log.debug("Flagging comment as spam: {}", commentId);
  }

  private void validateRequest(BulkModerationRequest request) {
    if (request.getCommentIds() == null || request.getCommentIds().isEmpty()) {
      throw new IllegalArgumentException("Comment IDs list cannot be empty");
    }
    if (request.getCommentIds().size() > MAX_COMMENTS_PER_TASK) {
      throw new IllegalArgumentException("Cannot moderate more than " + MAX_COMMENTS_PER_TASK + " comments at once");
    }
    if (request.getAction() == null) {
      throw new IllegalArgumentException("Moderation action is required");
    }
  }

  private ModerationTask createModerationTask(BulkModerationRequest request, User moderator) {
    ModerationTask task = new ModerationTask();
    task.setModeratorId(moderator.getId());
    task.setModeratorUsername(moderator.getUsername());
    task.setAction(request.getAction());
    task.setCommentIds(String.join(",", request.getCommentIds()));
    task.setTotalComments(request.getCommentIds().size());
    task.setStatus(ModerationStatus.QUEUED);
    return task;
  }

  private void updateTaskStatus(ModerationTask task, ModerationStatus status) {
    task.setStatus(status);
    if (status == ModerationStatus.PROCESSING) {
      task.setStartedAt(LocalDateTime.now());
    } else if (status == ModerationStatus.COMPLETED || status == ModerationStatus.FAILED) {
      task.setCompletedAt(LocalDateTime.now());
    }
    moderationTaskRepository.save(task);
  }

  private void updateTaskProgress(ModerationTask task) {
    moderationTaskRepository.save(task);
  }

  private void handleTaskError(UUID taskId, String errorMessage) {
    moderationTaskRepository.findById(taskId).ifPresent(task -> {
      task.setStatus(ModerationStatus.FAILED);
      task.setErrorDetails(errorMessage);
      task.setCompletedAt(LocalDateTime.now());
      moderationTaskRepository.save(task);
    });
  }

  private ModerationTaskDTO mapToDTO(ModerationTask task) {
    long remainingComments = task.getTotalComments() - task.getProcessedComments() - task.getFailedComments();
    long estimatedRemainingMs = remainingComments * ESTIMATED_MS_PER_COMMENT;

    return ModerationTaskDTO.builder()
        .taskId(task.getId())
        .action(task.getAction())
        .status(task.getStatus())
        .totalComments(task.getTotalComments())
        .processedComments(task.getProcessedComments())
        .failedComments(task.getFailedComments())
        .progressPercentage(task.getProgressPercentage())
        .errorDetails(task.getErrorDetails())
        .createdAt(task.getCreatedAt())
        .startedAt(task.getStartedAt())
        .completedAt(task.getCompletedAt())
        .estimatedRemainingMs(estimatedRemainingMs)
        .build();
  }
}
