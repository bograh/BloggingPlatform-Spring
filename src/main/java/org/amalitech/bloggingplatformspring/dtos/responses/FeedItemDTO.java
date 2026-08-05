package org.amalitech.bloggingplatformspring.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a single item in the aggregated feed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedItemDTO {

  private Long postId;
  private String title;
  private String bodyPreview;
  private String author;
  private String authorId;
  private List<String> tags;
  private String postedAt;
  private Long totalComments;
  private Double trendingScore;
  private Long viewCount;
  private String feedSource;
}
