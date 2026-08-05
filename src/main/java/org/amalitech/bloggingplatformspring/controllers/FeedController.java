package org.amalitech.bloggingplatformspring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponseGeneric;
import org.amalitech.bloggingplatformspring.dtos.responses.FeedAggregationResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.LiveTrendingResponse;
import org.amalitech.bloggingplatformspring.exceptions.ErrorResponse;
import org.amalitech.bloggingplatformspring.services.FeedAggregationService;
import org.amalitech.bloggingplatformspring.services.LiveTrendingScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for aggregated feed and live trending endpoints.
 */
@RestController
@RequestMapping("/api/feed")
@Tag(name = "10. Feed Aggregation", description = "APIs for aggregated feed and live trending data")
@RequiredArgsConstructor
public class FeedController {

  private final FeedAggregationService feedAggregationService;
  private final LiveTrendingScoreService liveTrendingScoreService;

  @GetMapping
  @Operation(summary = "Get aggregated feed", description = "Returns an aggregated feed containing recent, trending, and popular posts fetched in parallel")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Feed successfully aggregated", content = @Content(schema = @Schema(implementation = FeedAggregationResponse.class))),
      @ApiResponse(responseCode = "500", description = "Server error during aggregation", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<FeedAggregationResponse>> getAggregatedFeed(
      @Parameter(description = "Number of items per feed section", example = "10") @RequestParam(name = "limit", defaultValue = "10") int limit) {

    FeedAggregationResponse response = feedAggregationService.aggregateFeed(limit);
    return ResponseEntity.ok(ApiResponseGeneric.success("Feed aggregated successfully", response));
  }

  @GetMapping("/trending/live")
  @Operation(summary = "Get live trending scores", description = "Returns real-time trending post scores with velocity metrics and rank changes")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Live trending data retrieved", content = @Content(schema = @Schema(implementation = LiveTrendingResponse.class))),
      @ApiResponse(responseCode = "500", description = "Server error during calculation", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<LiveTrendingResponse>> getLiveTrending(
      @Parameter(description = "Number of trending posts to return", example = "10") @RequestParam(name = "limit", defaultValue = "10") int limit) {

    LiveTrendingResponse response = liveTrendingScoreService.getLiveTrendingScores(limit);
    return ResponseEntity.ok(ApiResponseGeneric.success("Live trending data retrieved", response));
  }

  @PostMapping("/trending/refresh")
  @Operation(summary = "Force refresh trending scores", description = "Forces an immediate refresh of all trending score calculations")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Trending scores refreshed"),
      @ApiResponse(responseCode = "500", description = "Server error during refresh", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<String>> refreshTrending() {
    liveTrendingScoreService.forceRefresh();
    return ResponseEntity.ok(ApiResponseGeneric.success("Trending scores refreshed", "OK"));
  }
}
