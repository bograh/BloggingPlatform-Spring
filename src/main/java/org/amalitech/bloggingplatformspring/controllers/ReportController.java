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
import org.amalitech.bloggingplatformspring.dtos.requests.ReportExportRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponseGeneric;
import org.amalitech.bloggingplatformspring.dtos.responses.ReportExportDTO;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.ErrorResponse;
import org.amalitech.bloggingplatformspring.services.ParallelReportExportService;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for report export operations.
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "13. Report Export", description = "APIs for async report generation and download")
@RequiredArgsConstructor
public class ReportController {

  private final ParallelReportExportService reportExportService;
  private final UserUtils userUtils;

  @PostMapping("/export")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Initiate report export", description = "Starts async generation of a report. Admin only.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "202", description = "Report export initiated", content = @Content(schema = @Schema(implementation = ReportExportDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<ReportExportDTO>> initiateExport(
      @Valid @RequestBody ReportExportRequest request,
      HttpServletRequest httpRequest) {

    User user = userUtils.getUserFromRequest(httpRequest);
    ReportExportDTO report = reportExportService.initiateExport(request, user);
    return new ResponseEntity<>(
        ApiResponseGeneric.success("Report export initiated", report),
        HttpStatus.ACCEPTED);
  }

  @GetMapping("/download/{reportId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Download report file", description = "Downloads the generated report content as a text file. Report must be COMPLETED.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Report file returned"),
      @ApiResponse(responseCode = "404", description = "Report not found or content unavailable", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "Report not yet completed", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<byte[]> downloadReport(
      @Parameter(description = "Report UUID") @PathVariable UUID reportId) {

    String content = reportExportService.downloadReport(reportId);
    byte[] contentBytes = content.getBytes();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    headers.setContentDispositionFormData("attachment", "report-" + reportId + ".txt");
    headers.setContentLength(contentBytes.length);

    return ResponseEntity.ok().headers(headers).body(contentBytes);
  }

  @GetMapping("/{reportId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get report export status", description = "Returns the current status and download URL of a report")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Report status retrieved", content = @Content(schema = @Schema(implementation = ReportExportDTO.class))),
      @ApiResponse(responseCode = "404", description = "Report not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<ReportExportDTO>> getReportStatus(
      @Parameter(description = "Report UUID") @PathVariable UUID reportId) {

    ReportExportDTO report = reportExportService.getReportStatus(reportId);
    return ResponseEntity.ok(ApiResponseGeneric.success("Report status retrieved", report));
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get user's reports", description = "Returns recent report exports for the current user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Reports retrieved"),
      @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<List<ReportExportDTO>>> getUserReports(
      @Parameter(description = "Maximum number of reports to return", example = "20") @RequestParam(name = "limit", defaultValue = "20") int limit,
      HttpServletRequest request) {

    User user = userUtils.getUserFromRequest(request);
    List<ReportExportDTO> reports = reportExportService.getUserReports(user.getId(), limit);
    return ResponseEntity.ok(ApiResponseGeneric.success("Reports retrieved", reports));
  }
}
