package org.amalitech.bloggingplatformspring.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing the aggregated feed response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedAggregationResponse {

  private List<FeedItemDTO> recentPosts;
  private List<FeedItemDTO> trendingPosts;
  private List<FeedItemDTO> popularPosts;
  private List<FeedItemDTO> recommendedPosts;
  private LocalDateTime generatedAt;
  private long generationTimeMs;
  private int totalItemsAggregated;
}
