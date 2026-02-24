package org.amalitech.bloggingplatformspring.dtos.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.amalitech.bloggingplatformspring.enums.ModerationAction;

import java.util.List;

/**
 * DTO for submitting a bulk comment moderation request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkModerationRequest {

  @NotEmpty(message = "Comment IDs list cannot be empty")
  private List<String> commentIds;

  @NotNull(message = "Moderation action is required")
  private ModerationAction action;

  private String reason;
}
