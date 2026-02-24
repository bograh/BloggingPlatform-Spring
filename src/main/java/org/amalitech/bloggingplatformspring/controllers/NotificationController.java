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
import org.amalitech.bloggingplatformspring.dtos.requests.NotificationRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponseGeneric;
import org.amalitech.bloggingplatformspring.dtos.responses.NotificationDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.NotificationStatsDTO;
import org.amalitech.bloggingplatformspring.exceptions.ErrorResponse;
import org.amalitech.bloggingplatformspring.services.NotificationOutboxProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for notification management.
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "12. Notifications", description = "APIs for notification management and monitoring")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationOutboxProcessor notificationProcessor;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Queue a notification", description = "Queues a notification for async email delivery. Admin only.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "202", description = "Notification queued", content = @Content(schema = @Schema(implementation = NotificationDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<NotificationDTO>> queueNotification(
      @Valid @RequestBody NotificationRequest request) {

    NotificationDTO notification = notificationProcessor.queueNotification(request);
    return new ResponseEntity<>(
        ApiResponseGeneric.success("Notification queued", notification),
        HttpStatus.ACCEPTED);
  }

  @GetMapping("/{notificationId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get notification status", description = "Returns the current status of a notification")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Notification status retrieved", content = @Content(schema = @Schema(implementation = NotificationDTO.class))),
      @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<NotificationDTO>> getNotificationStatus(
      @Parameter(description = "Notification UUID") @PathVariable UUID notificationId) {

    NotificationDTO notification = notificationProcessor.getNotificationStatus(notificationId);
    return ResponseEntity.ok(ApiResponseGeneric.success("Notification status retrieved", notification));
  }

  @GetMapping("/stats")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get notification statistics", description = "Returns processing statistics for the notification outbox")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Stats retrieved", content = @Content(schema = @Schema(implementation = NotificationStatsDTO.class))),
      @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<NotificationStatsDTO>> getStats() {
    NotificationStatsDTO stats = notificationProcessor.getStats();
    return ResponseEntity.ok(ApiResponseGeneric.success("Stats retrieved", stats));
  }
}
