package org.amalitech.bloggingplatformspring.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.amalitech.bloggingplatformspring.enums.ReportType;

import java.time.LocalDateTime;

/**
 * DTO for requesting a report export.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExportRequest {

  @NotNull(message = "Report type is required")
  private ReportType reportType;

  private LocalDateTime startDate;
  private LocalDateTime endDate;
  private String additionalFilters;
}
