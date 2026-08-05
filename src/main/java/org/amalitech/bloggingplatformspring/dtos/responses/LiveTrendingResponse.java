package org.amalitech.bloggingplatformspring.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for live trending score engine response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveTrendingResponse {

  private List<TrendingScoreDTO> trendingPosts;
  private LocalDateTime snapshotTime;
  private long calculationTimeMs;
  private int totalTrackedPosts;
  private boolean isLiveUpdate;
}
