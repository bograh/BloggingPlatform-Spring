package org.amalitech.bloggingplatformspring.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.amalitech.bloggingplatformspring.enums.NotificationStatus;
import org.amalitech.bloggingplatformspring.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for notification status response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

  private UUID id;
  private String recipientEmail;
  private NotificationType type;
  private String subject;
  private NotificationStatus status;
  private int retryCount;
  private String lastError;
  private LocalDateTime createdAt;
  private LocalDateTime processedAt;
}
