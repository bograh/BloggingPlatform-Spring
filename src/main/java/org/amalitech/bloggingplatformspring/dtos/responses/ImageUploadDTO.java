package org.amalitech.bloggingplatformspring.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.amalitech.bloggingplatformspring.enums.ImageUploadStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for image upload status and URLs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadDTO {

  private UUID imageId;
  private Long postId;
  private String originalFilename;
  private ImageUploadStatus status;
  private String cdnUrl;
  private String thumbnailUrl;
  private Long fileSize;
  private String contentType;
  private String errorMessage;
  private LocalDateTime uploadStartedAt;
  private LocalDateTime uploadCompletedAt;
}
