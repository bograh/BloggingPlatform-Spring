package org.amalitech.bloggingplatformspring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.amalitech.bloggingplatformspring.enums.ReportType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity tracking async report export jobs.
 */
@Entity
@Table(name = "report_exports", indexes = {
    @Index(name = "idx_report_user", columnList = "requested_by"),
    @Index(name = "idx_report_status", columnList = "export_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportExport {

  private static final String STATUS_PENDING = "PENDING";
  private static final String STATUS_PROCESSING = "PROCESSING";
  private static final String STATUS_COMPLETED = "COMPLETED";
  private static final String STATUS_FAILED = "FAILED";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "requested_by", nullable = false)
  private UUID requestedBy;

  @Enumerated(EnumType.STRING)
  @Column(name = "report_type", nullable = false)
  private ReportType reportType;

  @Column(name = "export_status", nullable = false)
  private String exportStatus = STATUS_PENDING;

  @Column(name = "file_path")
  private String filePath;

  @Column(name = "file_size")
  private Long fileSize;

  @Column(name = "download_url")
  private String downloadUrl;

  @Column(name = "progress_percentage")
  private int progressPercentage = 0;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "filter_params", columnDefinition = "TEXT")
  private String filterParams;

  @Column(name = "file_content", columnDefinition = "TEXT")
  private String fileContent;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  public boolean isComplete() {
    return STATUS_COMPLETED.equals(exportStatus) || STATUS_FAILED.equals(exportStatus);
  }

  public void markProcessing() {
    this.exportStatus = STATUS_PROCESSING;
    this.startedAt = LocalDateTime.now();
  }

  public void markCompleted(String filePath, String downloadUrl, String fileContent) {
    this.exportStatus = STATUS_COMPLETED;
    this.filePath = filePath;
    this.downloadUrl = downloadUrl;
    this.fileContent = fileContent;
    this.progressPercentage = 100;
    this.completedAt = LocalDateTime.now();
  }

  public void markFailed(String errorMessage) {
    this.exportStatus = STATUS_FAILED;
    this.errorMessage = errorMessage;
    this.completedAt = LocalDateTime.now();
  }
}
