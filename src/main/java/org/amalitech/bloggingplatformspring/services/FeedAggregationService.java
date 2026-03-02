package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.responses.FeedAggregationResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.FeedItemDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for asynchronously aggregating multiple feed sources.
 * Uses parallel CompletableFuture execution for optimal performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedAggregationService {

    private static final int MAX_FEED_LIMIT = 50;
    private static final int BODY_PREVIEW_LENGTH = 200;

    private final PostRepository postRepository;
    private final PostRankingIndexService postRankingIndexService;

    /**
     * Aggregates multiple feed sources asynchronously in parallel.
     *
     * @param limit number of items per feed section
     * @return aggregated feed response
     */
    public FeedAggregationResponse aggregateFeed(int limit) {
        long startTime = System.currentTimeMillis();
        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_FEED_LIMIT);

        CompletableFuture<List<FeedItemDTO>> recentFuture = fetchRecentPostsAsync(effectiveLimit)
                .exceptionally(ex -> List.of());
        CompletableFuture<List<FeedItemDTO>> trendingFuture = fetchTrendingPostsAsync(effectiveLimit)
                .exceptionally(ex -> List.of());
        CompletableFuture<List<FeedItemDTO>> popularFuture = fetchPopularPostsAsync(effectiveLimit)
                .exceptionally(ex -> List.of());

        CompletableFuture.allOf(recentFuture, trendingFuture, popularFuture).join();

        List<FeedItemDTO> recentPosts = recentFuture.join();
        List<FeedItemDTO> trendingPosts = trendingFuture.join();
        List<FeedItemDTO> popularPosts = popularFuture.join();

        long generationTimeMs = System.currentTimeMillis() - startTime;
        int totalItems = recentPosts.size() + trendingPosts.size() + popularPosts.size();

        log.info("Feed aggregation completed in {}ms with {} total items", generationTimeMs, totalItems);

        return FeedAggregationResponse.builder()
                .recentPosts(recentPosts)
                .trendingPosts(trendingPosts)
                .popularPosts(popularPosts)
                .recommendedPosts(List.of())
                .generatedAt(LocalDateTime.now())
                .generationTimeMs(generationTimeMs)
                .totalItemsAggregated(totalItems)
                .build();
    }

    @Async("applicationTaskExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<List<FeedItemDTO>> fetchRecentPostsAsync(int limit) {
        log.debug("Fetching recent posts asynchronously, limit: {}", limit);
        try {
            PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "postedAt"));
            List<Post> posts = postRepository.findAll(pageRequest).getContent();
            List<FeedItemDTO> feedItems = posts.stream()
                    .map(post -> mapToFeedItem(post, "RECENT"))
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(feedItems);
        } catch (Exception e) {
            log.error("Error fetching recent posts: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("applicationTaskExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<List<FeedItemDTO>> fetchTrendingPostsAsync(int limit) {
        log.debug("Fetching trending posts asynchronously, limit: {}", limit);
        try {
            List<PostResponseDTO> trending = postRankingIndexService.getTrendingPosts(limit);
            List<FeedItemDTO> feedItems = trending.stream()
                    .map(dto -> mapResponseToFeedItem(dto, "TRENDING"))
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(feedItems);
        } catch (Exception e) {
            log.error("Error fetching trending posts: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("applicationTaskExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<List<FeedItemDTO>> fetchPopularPostsAsync(int limit) {
        log.debug("Fetching popular posts asynchronously, limit: {}", limit);
        try {
            List<PostResponseDTO> popular = postRankingIndexService.getPopularPosts(limit);
            List<FeedItemDTO> feedItems = popular.stream()
                    .map(dto -> mapResponseToFeedItem(dto, "POPULAR"))
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(feedItems);
        } catch (Exception e) {
            log.error("Error fetching popular posts: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private FeedItemDTO mapToFeedItem(Post post, String source) {
        String bodyPreview = truncateBody(post.getBody());
        List<String> tagNames = post.getTags().stream()
                .map(tag -> tag.getName())
                .collect(Collectors.toList());

        return FeedItemDTO.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .bodyPreview(bodyPreview)
                .author(post.getAuthor().getUsername())
                .authorId(post.getAuthor().getId().toString())
                .tags(tagNames)
                .postedAt(post.getPostedAt().toString())
                .totalComments(0L)
                .trendingScore(0.0)
                .viewCount(0L)
                .feedSource(source)
                .build();
    }

    private FeedItemDTO mapResponseToFeedItem(PostResponseDTO dto, String source) {
        String bodyPreview = truncateBody(dto.getBody());
        return FeedItemDTO.builder()
                .postId(dto.getId())
                .title(dto.getTitle())
                .bodyPreview(bodyPreview)
                .author(dto.getAuthor())
                .authorId(dto.getAuthorId())
                .tags(dto.getTags())
                .postedAt(dto.getPostedAt())
                .totalComments(dto.getTotalComments())
                .trendingScore(0.0)
                .viewCount(0L)
                .feedSource(source)
                .build();
    }

    private String truncateBody(String body) {
        if (body == null) {
            return "";
        }
        if (body.length() <= BODY_PREVIEW_LENGTH) {
            return body;
        }
        return body.substring(0, BODY_PREVIEW_LENGTH) + "...";
    }
}