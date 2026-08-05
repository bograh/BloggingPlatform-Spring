package org.amalitech.bloggingplatformspring.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.amalitech.bloggingplatformspring.enums.ReportType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for report export status and download info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExportDTO {

  private UUID reportId;
  private ReportType reportType;
  private String status;
  private int progressPercentage;
  private String downloadUrl;
  private Long fileSize;
  private String errorMessage;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
  private LocalDateTime expiresAt;
}
