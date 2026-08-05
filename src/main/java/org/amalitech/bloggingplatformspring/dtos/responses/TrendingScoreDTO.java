package org.amalitech.bloggingplatformspring.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for real-time trending score updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingScoreDTO {

  private Long postId;
  private String title;
  private Double currentScore;
  private Double previousScore;
  private Double scoreChange;
  private Long commentCount;
  private Long viewCount;
  private Double velocityFactor;
  private LocalDateTime lastUpdated;
  private Integer rank;
  private Integer previousRank;
  private String trend;
}
