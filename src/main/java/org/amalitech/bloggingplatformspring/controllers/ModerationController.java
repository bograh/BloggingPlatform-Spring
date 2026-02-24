package org.amalitech.bloggingplatformspring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.requests.BulkModerationRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponseGeneric;
import org.amalitech.bloggingplatformspring.dtos.responses.ModerationTaskDTO;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.ErrorResponse;
import org.amalitech.bloggingplatformspring.services.BulkCommentModerationService;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for bulk comment moderation operations.
 */
@RestController
@RequestMapping("/api/moderation")
@Tag(name = "11. Comment Moderation", description = "APIs for bulk comment moderation tasks")
@RequiredArgsConstructor
public class ModerationController {

  private final BulkCommentModerationService moderationService;
  private final UserUtils userUtils;

  @PostMapping("/bulk")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Submit bulk moderation task", description = "Queues a bulk moderation action for async processing. Admin only.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "202", description = "Moderation task queued", content = @Content(schema = @Schema(implementation = ModerationTaskDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<ModerationTaskDTO>> submitModerationTask(
      @Valid @RequestBody BulkModerationRequest request,
      HttpServletRequest httpRequest) {

    User moderator = userUtils.getUserFromRequest(httpRequest);
    ModerationTaskDTO task = moderationService.submitModerationTask(request, moderator);
    return new ResponseEntity<>(
        ApiResponseGeneric.success("Moderation task queued", task),
        HttpStatus.ACCEPTED);
  }

  @GetMapping("/tasks/{taskId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get moderation task status", description = "Returns the current status and progress of a moderation task")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Task status retrieved", content = @Content(schema = @Schema(implementation = ModerationTaskDTO.class))),
      @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<ModerationTaskDTO>> getTaskStatus(
      @Parameter(description = "Task UUID") @PathVariable UUID taskId) {

    ModerationTaskDTO task = moderationService.getTaskStatus(taskId);
    return ResponseEntity.ok(ApiResponseGeneric.success("Task status retrieved", task));
  }

  @GetMapping("/tasks")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get moderator's tasks", description = "Returns recent moderation tasks for the current moderator")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Tasks retrieved"),
      @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<List<ModerationTaskDTO>>> getModeratorTasks(
      @Parameter(description = "Maximum number of tasks to return", example = "20") @RequestParam(name = "limit", defaultValue = "20") int limit,
      HttpServletRequest request) {

    User moderator = userUtils.getUserFromRequest(request);
    List<ModerationTaskDTO> tasks = moderationService.getModeratorTasks(moderator.getId(), limit);
    return ResponseEntity.ok(ApiResponseGeneric.success("Tasks retrieved", tasks));
  }
}
