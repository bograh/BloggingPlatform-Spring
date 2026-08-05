package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.responses.LiveTrendingResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.TrendingScoreDTO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Live trending score engine that provides real-time score calculations.
 * Tracks score changes and provides velocity-based trending analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveTrendingScoreService {

  private static final double COMMENTS_WEIGHT = 10.0;
  private static final double RECENCY_WEIGHT = 2.0;
  private static final double VELOCITY_WEIGHT = 5.0;
  private static final int RECENCY_WINDOW_HOURS = 72;
  private static final int MAX_TRENDING_POSTS = 50;

  private final PostRankingIndexService postRankingIndexService;

  private final ConcurrentHashMap<Long, ScoreSnapshot> previousScores = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Long, Integer> previousRanks = new ConcurrentHashMap<>();

  /**
   * Gets live trending data with score changes and velocity metrics.
   *
   * @param limit number of trending posts to return
   * @return live trending response with detailed scores
   */
  public LiveTrendingResponse getLiveTrendingScores(int limit) {
    long startTime = System.currentTimeMillis();
    int effectiveLimit = Math.min(Math.max(limit, 1), MAX_TRENDING_POSTS);

    List<PostResponseDTO> trendingPosts = postRankingIndexService.getTrendingPosts(effectiveLimit);
    List<TrendingScoreDTO> scoredPosts = calculateDetailedScores(trendingPosts);

    long calculationTimeMs = System.currentTimeMillis() - startTime;

    return LiveTrendingResponse.builder()
        .trendingPosts(scoredPosts)
        .snapshotTime(LocalDateTime.now())
        .calculationTimeMs(calculationTimeMs)
        .totalTrackedPosts(previousScores.size())
        .isLiveUpdate(true)
        .build();
  }

  /**
   * Asynchronously calculates trending scores for batch processing.
   *
   * @param limit number of posts to process
   * @return future containing live trending response
   */
  @Async("applicationTaskExecutor")
  public CompletableFuture<LiveTrendingResponse> getLiveTrendingScoresAsync(int limit) {
    LiveTrendingResponse response = getLiveTrendingScores(limit);
    return CompletableFuture.completedFuture(response);
  }

  /**
   * Scheduled task to refresh score snapshots periodically.
   * Runs every 5 minutes to update velocity calculations.
   */
  @Scheduled(fixedRate = 300_000)
  public void refreshScoreSnapshots() {
    log.debug("Refreshing trending score snapshots");
    try {
      List<PostResponseDTO> allTrending = postRankingIndexService.getTrendingPosts(MAX_TRENDING_POSTS);
      updateScoreSnapshots(allTrending);
    } catch (Exception e) {
      log.error("Error refreshing score snapshots: {}", e.getMessage(), e);
    }
  }

  private List<TrendingScoreDTO> calculateDetailedScores(List<PostResponseDTO> posts) {
    List<TrendingScoreDTO> scoredPosts = new ArrayList<>(posts.size());
    AtomicInteger currentRank = new AtomicInteger(1);

    for (PostResponseDTO post : posts) {
      TrendingScoreDTO scoredPost = buildTrendingScoreDTO(post, currentRank.getAndIncrement());
      scoredPosts.add(scoredPost);
    }

    updateCurrentRanks(scoredPosts);
    return scoredPosts;
  }

  private TrendingScoreDTO buildTrendingScoreDTO(PostResponseDTO post, int currentRank) {
    Long postId = post.getId();
    long commentCount = post.getTotalComments();

    double currentScore = calculateTrendingScore(commentCount, post.getPostedAt());

    ScoreSnapshot previousSnapshot = previousScores.get(postId);
    double previousScore = previousSnapshot != null ? previousSnapshot.score() : currentScore;
    double scoreChange = currentScore - previousScore;

    Integer previousRank = previousRanks.getOrDefault(postId, currentRank);
    String trend = determineTrend(currentRank, previousRank, scoreChange);

    double velocityFactor = calculateVelocityFactor(postId, currentScore, previousSnapshot);

    return TrendingScoreDTO.builder()
        .postId(postId)
        .title(post.getTitle())
        .currentScore(currentScore)
        .previousScore(previousScore)
        .scoreChange(scoreChange)
        .commentCount(commentCount)
        .viewCount(0L)
        .velocityFactor(velocityFactor)
        .lastUpdated(LocalDateTime.now())
        .rank(currentRank)
        .previousRank(previousRank)
        .trend(trend)
        .build();
  }

  private double calculateTrendingScore(long commentCount, String postedAt) {
    LocalDateTime postedAtDateTime = parsePostedAt(postedAt);
    if (postedAtDateTime == null) {
      return commentCount * COMMENTS_WEIGHT;
    }

    long hoursSincePosted = Duration.between(postedAtDateTime, LocalDateTime.now()).toHours();
    long recencyBonus = Math.max(0, RECENCY_WINDOW_HOURS - hoursSincePosted);

    return (commentCount * COMMENTS_WEIGHT) + (recencyBonus * RECENCY_WEIGHT);
  }

  private LocalDateTime parsePostedAt(String postedAt) {
    if (postedAt == null || postedAt.isBlank()) {
      return null;
    }

    try {
      return LocalDateTime.parse(postedAt);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private double calculateVelocityFactor(Long postId, double currentScore, ScoreSnapshot previousSnapshot) {
    if (previousSnapshot == null) {
      return 0.0;
    }

    long hoursSinceSnapshot = Duration.between(previousSnapshot.timestamp(), LocalDateTime.now()).toHours();
    if (hoursSinceSnapshot == 0) {
      return 0.0;
    }

    double scoreChange = currentScore - previousSnapshot.score();
    return (scoreChange / hoursSinceSnapshot) * VELOCITY_WEIGHT;
  }

  private String determineTrend(int currentRank, int previousRank, double scoreChange) {
    if (currentRank < previousRank) {
      return "UP";
    } else if (currentRank > previousRank) {
      return "DOWN";
    } else if (scoreChange > 0) {
      return "RISING";
    } else if (scoreChange < 0) {
      return "FALLING";
    }
    return "STABLE";
  }

  private void updateScoreSnapshots(List<PostResponseDTO> posts) {
    for (PostResponseDTO post : posts) {
      long commentCount = post.getTotalComments();
      double score = calculateTrendingScore(commentCount, post.getPostedAt());
      previousScores.put(post.getId(), new ScoreSnapshot(score, LocalDateTime.now()));
    }
  }

  private void updateCurrentRanks(List<TrendingScoreDTO> scoredPosts) {
    for (TrendingScoreDTO scored : scoredPosts) {
      previousRanks.put(scored.getPostId(), scored.getRank());
    }
  }

  /**
   * Gets the current score for a specific post.
   *
   * @param postId post ID
   * @return optional score snapshot
   */
  public ScoreSnapshot getScoreSnapshot(Long postId) {
    return previousScores.get(postId);
  }

  /**
   * Forces immediate refresh of all trending scores.
   */
  public void forceRefresh() {
    postRankingIndexService.rebuildIndexes();
    refreshScoreSnapshots();
  }

  /**
   * Immutable record for score snapshots.
   */
  public record ScoreSnapshot(double score, LocalDateTime timestamp) {
  }
}
