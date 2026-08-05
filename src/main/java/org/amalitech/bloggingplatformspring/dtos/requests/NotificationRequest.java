package org.amalitech.bloggingplatformspring.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.amalitech.bloggingplatformspring.enums.NotificationType;

/**
 * DTO for creating a notification to be processed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

  @NotBlank(message = "Recipient email is required")
  @Email(message = "Invalid email format")
  private String recipientEmail;

  private String recipientName;

  @NotNull(message = "Notification type is required")
  private NotificationType type;

  @NotBlank(message = "Subject is required")
  private String subject;

  @NotBlank(message = "Body is required")
  private String body;
}
