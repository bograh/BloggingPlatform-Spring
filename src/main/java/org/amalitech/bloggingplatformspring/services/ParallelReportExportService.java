package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.requests.ReportExportRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.ReportExportDTO;
import org.amalitech.bloggingplatformspring.entity.ReportExport;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.enums.ReportType;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.ReportExportRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for asynchronously generating reports in parallel.
 * Supports multiple report types with progress tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParallelReportExportService {

  private static final int REPORT_EXPIRY_DAYS = 7;
  private static final String REPORTS_BASE_PATH = "/reports/";
  private static final String DOWNLOAD_URL_PREFIX = "/api/reports/download/";

  private final ReportExportRepository reportExportRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final CommentRepository commentRepository;

  /**
   * Initiates an async report export.
   *
   * @param request export request with type and filters
   * @param user    requesting user
   * @return report export DTO with tracking info
   */
  @Transactional
  public ReportExportDTO initiateExport(ReportExportRequest request, User user) {
    ReportExport report = new ReportExport();
    report.setRequestedBy(user.getId());
    report.setReportType(request.getReportType());
    report.setFilterParams(buildFilterParams(request));
    report.setExpiresAt(LocalDateTime.now().plusDays(REPORT_EXPIRY_DAYS));

    ReportExport saved = reportExportRepository.save(report);
    log.info("Initiated report export {} of type {} for user {}",
        saved.getId(), request.getReportType(), user.getUsername());

    generateReportAsync(saved.getId());

    return mapToDTO(saved);
  }

  /**
   * Gets the status of a report export.
   *
   * @param reportId report UUID
   * @return report export DTO
   */
  public ReportExportDTO getReportStatus(UUID reportId) {
    ReportExport report = reportExportRepository.findById(reportId)
        .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));
    return mapToDTO(report);
  }

  /**
   * Gets recent reports for a user.
   *
   * @param userId user UUID
   * @param limit  max reports to return
   * @return list of report DTOs
   */
  public List<ReportExportDTO> getUserReports(UUID userId, int limit) {
    int effectiveLimit = Math.min(Math.max(limit, 1), 50);
    List<ReportExport> reports = reportExportRepository
        .findByRequestedByOrderByCreatedAtDesc(userId, PageRequest.of(0, effectiveLimit));
    return reports.stream().map(this::mapToDTO).toList();
  }

  /**
   * Generates a report asynchronously using parallel data collection.
   *
   * @param reportId report UUID to generate
   */
  @Async("applicationTaskExecutor")
  public CompletableFuture<Void> generateReportAsync(UUID reportId) {
    log.info("Starting async report generation for: {}", reportId);

    try {
      ReportExport report = reportExportRepository.findById(reportId)
          .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));

      report.markProcessing();
      reportExportRepository.save(report);

      String reportContent = generateReportContent(report);
      String filePath = saveReportToFile(reportId, reportContent);
      String downloadUrl = DOWNLOAD_URL_PREFIX + reportId;

      report.markCompleted(filePath, downloadUrl);
      report.setFileSize((long) reportContent.length());
      reportExportRepository.save(report);

      log.info("Completed report generation: {}", reportId);

    } catch (Exception e) {
      log.error("Failed to generate report {}: {}", reportId, e.getMessage(), e);
      handleExportError(reportId, e.getMessage());
    }

    return CompletableFuture.completedFuture(null);
  }

  private String generateReportContent(ReportExport report) {
    return switch (report.getReportType()) {
      case USER_ACTIVITY -> generateUserActivityReport();
      case POST_ANALYTICS -> generatePostAnalyticsReport();
      case COMMENT_SUMMARY -> generateCommentSummaryReport();
      case MODERATION_LOG -> generateModerationLogReport();
      case FULL_PLATFORM -> generateFullPlatformReport();
    };
  }

  private String generateUserActivityReport() {
    log.debug("Generating user activity report");

    CompletableFuture<Long> userCountFuture = CompletableFuture.supplyAsync(userRepository::count);
    CompletableFuture<Long> postCountFuture = CompletableFuture.supplyAsync(postRepository::count);

    CompletableFuture.allOf(userCountFuture, postCountFuture).join();

    return String.format("""
        User Activity Report
        Generated: %s
        ======================
        Total Users: %d
        Total Posts: %d
        """, LocalDateTime.now(), userCountFuture.join(), postCountFuture.join());
  }

  private String generatePostAnalyticsReport() {
    log.debug("Generating post analytics report");
    long totalPosts = postRepository.count();

    return String.format("""
        Post Analytics Report
        Generated: %s
        ======================
        Total Posts: %d
        """, LocalDateTime.now(), totalPosts);
  }

  private String generateCommentSummaryReport() {
    log.debug("Generating comment summary report");
    long totalComments = commentRepository.count();

    return String.format("""
        Comment Summary Report
        Generated: %s
        ======================
        Total Comments: %d
        """, LocalDateTime.now(), totalComments);
  }

  private String generateModerationLogReport() {
    log.debug("Generating moderation log report");

    return String.format("""
        Moderation Log Report
        Generated: %s
        ======================
        Moderation actions logged here
        """, LocalDateTime.now());
  }

  private String generateFullPlatformReport() {
    log.debug("Generating full platform report");

    CompletableFuture<Long> usersFuture = CompletableFuture.supplyAsync(userRepository::count);
    CompletableFuture<Long> postsFuture = CompletableFuture.supplyAsync(postRepository::count);
    CompletableFuture<Long> commentsFuture = CompletableFuture.supplyAsync(commentRepository::count);

    CompletableFuture.allOf(usersFuture, postsFuture, commentsFuture).join();

    return String.format("""
        Full Platform Report
        Generated: %s
        ======================
        Total Users: %d
        Total Posts: %d
        Total Comments: %d
        """, LocalDateTime.now(), usersFuture.join(), postsFuture.join(), commentsFuture.join());
  }

  private String saveReportToFile(UUID reportId, String content) {
    String filePath = REPORTS_BASE_PATH + reportId + ".txt";
    log.debug("Simulating save to: {}", filePath);
    return filePath;
  }

  private String buildFilterParams(ReportExportRequest request) {
    StringBuilder params = new StringBuilder();
    if (request.getStartDate() != null) {
      params.append("startDate=").append(request.getStartDate()).append(";");
    }
    if (request.getEndDate() != null) {
      params.append("endDate=").append(request.getEndDate()).append(";");
    }
    if (request.getAdditionalFilters() != null) {
      params.append("filters=").append(request.getAdditionalFilters());
    }
    return params.toString();
  }

  private void handleExportError(UUID reportId, String errorMessage) {
    reportExportRepository.findById(reportId).ifPresent(report -> {
      report.markFailed(errorMessage);
      reportExportRepository.save(report);
    });
  }

  /**
   * Scheduled cleanup of expired reports.
   * Runs daily at 4 AM.
   */
  @Scheduled(cron = "0 0 4 * * *")
  @Transactional
  public void cleanupExpiredReports() {
    log.info("Starting cleanup of expired reports");
    int deleted = reportExportRepository.deleteExpiredReports(LocalDateTime.now());
    log.info("Cleaned up {} expired reports", deleted);
  }

  private ReportExportDTO mapToDTO(ReportExport report) {
    return ReportExportDTO.builder()
        .reportId(report.getId())
        .reportType(report.getReportType())
        .status(report.getExportStatus())
        .progressPercentage(report.getProgressPercentage())
        .downloadUrl(report.getDownloadUrl())
        .fileSize(report.getFileSize())
        .errorMessage(report.getErrorMessage())
        .createdAt(report.getCreatedAt())
        .completedAt(report.getCompletedAt())
        .expiresAt(report.getExpiresAt())
        .build();
  }
}
