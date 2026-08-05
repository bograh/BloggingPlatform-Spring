package org.amalitech.bloggingplatformspring.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for notification processing statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsDTO {

  private long totalPending;
  private long totalProcessing;
  private long totalSent;
  private long totalFailed;
  private long totalRetrying;
  private double averageProcessingTimeMs;
  private long processedLast24Hours;
}
