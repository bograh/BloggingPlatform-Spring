package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostCommentCountProjection;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.amalitech.bloggingplatformspring.utils.PostUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostRankingIndexService {

  private static final long INDEX_TTL_MILLIS = 60_000;
  private static final int DEFAULT_LIMIT = 10;
  private static final double TRENDING_COMMENTS_WEIGHT = 10.0;
  private static final double TRENDING_RECENCY_WEIGHT = 2.0;

  private final PostRepository postRepository;
  private final CommentRepository commentRepository;
  private final PostUtils postUtils;

  private final ConcurrentHashMap<Long, PostSnapshot> snapshotsById = new ConcurrentHashMap<>();
  private final NavigableSet<RankedPost> popularIndex = new ConcurrentSkipListSet<>(rankComparator());
  private final NavigableSet<RankedPost> trendingIndex = new ConcurrentSkipListSet<>(rankComparator());
  private final AtomicLong lastRefreshEpochMillis = new AtomicLong(0);

  @Cacheable(cacheNames = Constants.POPULAR_POSTS_CACHE_NAME, key = "#limit")
  @Transactional(readOnly = true)
  public List<PostResponseDTO> getPopularPosts(int limit) {
    int normalizedLimit = normalizeLimit(limit);
    ensureFreshIndex();
    return buildTopPosts(popularIndex, normalizedLimit);
  }

  @Cacheable(cacheNames = Constants.TRENDING_POSTS_CACHE_NAME, key = "#limit")
  @Transactional(readOnly = true)
  public List<PostResponseDTO> getTrendingPosts(int limit) {
    int normalizedLimit = normalizeLimit(limit);
    ensureFreshIndex();
    return buildTopPosts(trendingIndex, normalizedLimit);
  }

  @Caching(evict = {
      @CacheEvict(cacheNames = Constants.POPULAR_POSTS_CACHE_NAME, allEntries = true),
      @CacheEvict(cacheNames = Constants.TRENDING_POSTS_CACHE_NAME, allEntries = true)
  })
  @Transactional(readOnly = true)
  public void rebuildIndexes() {
    List<Post> posts = postRepository.findAllWithAuthorAndTags();
    Map<Long, Long> commentsCountByPostId = fetchCommentsCountByPostId(posts);

    popularIndex.clear();
    trendingIndex.clear();

    ConcurrentHashMap<Long, PostSnapshot> rebuiltSnapshotsById = new ConcurrentHashMap<>();

    for (Post post : posts) {
      long totalComments = commentsCountByPostId.getOrDefault(post.getId(), 0L);
      PostResponseDTO responseDTO = postUtils.createPostResponseFromPost(post, totalComments);

      PostSnapshot snapshot = new PostSnapshot(post.getId(), responseDTO, totalComments, post.getUpdatedAt());
      rebuiltSnapshotsById.put(post.getId(), snapshot);

      popularIndex.add(new RankedPost(post.getId(), totalComments));
      trendingIndex.add(new RankedPost(post.getId(), buildTrendingScore(totalComments, post.getUpdatedAt())));
    }

    snapshotsById.clear();
    snapshotsById.putAll(rebuiltSnapshotsById);
    lastRefreshEpochMillis.set(System.currentTimeMillis());

    log.debug("Rebuilt post ranking indexes: posts={} popularEntries={} trendingEntries={}",
        posts.size(), popularIndex.size(), trendingIndex.size());
  }

  private void ensureFreshIndex() {
    long ageMillis = System.currentTimeMillis() - lastRefreshEpochMillis.get();
    if (popularIndex.isEmpty() || trendingIndex.isEmpty() || ageMillis > INDEX_TTL_MILLIS) {
      rebuildIndexes();
    }
  }

  private List<PostResponseDTO> buildTopPosts(NavigableSet<RankedPost> index, int limit) {
    List<PostResponseDTO> topPosts = new ArrayList<>(limit);

    for (RankedPost rankedPost : index) {
      PostSnapshot snapshot = snapshotsById.get(rankedPost.postId());
      if (snapshot == null) {
        continue;
      }

      topPosts.add(snapshot.response());
      if (topPosts.size() == limit) {
        break;
      }
    }

    return topPosts;
  }

  private Map<Long, Long> fetchCommentsCountByPostId(List<Post> posts) {
    if (posts.isEmpty()) {
      return Map.of();
    }

    List<Long> postIds = posts.stream().map(Post::getId).toList();
    List<PostCommentCountProjection> groupedCounts = commentRepository.countCommentsByPostIds(postIds);

    Map<Long, Long> commentsCountByPostId = new HashMap<>();
    for (PostCommentCountProjection groupedCount : groupedCounts) {
      commentsCountByPostId.put(groupedCount.getPostId(), groupedCount.getTotalComments());
    }

    return commentsCountByPostId;
  }

  private double buildTrendingScore(long totalComments, LocalDateTime updatedAt) {
    long hoursSinceUpdate = Duration.between(updatedAt, LocalDateTime.now()).toHours();
    long recencyBonus = Math.max(0, 72 - hoursSinceUpdate);
    return (totalComments * TRENDING_COMMENTS_WEIGHT) + (recencyBonus * TRENDING_RECENCY_WEIGHT);
  }

  private int normalizeLimit(int limit) {
    if (limit <= 0) {
      return DEFAULT_LIMIT;
    }
    return Math.min(limit, 50);
  }

  private Comparator<RankedPost> rankComparator() {
    return Comparator.comparingDouble(RankedPost::score)
        .reversed()
        .thenComparing(RankedPost::postId, Comparator.reverseOrder());
  }

  private record PostSnapshot(Long postId, PostResponseDTO response, long totalComments, LocalDateTime updatedAt) {
  }

  private record RankedPost(Long postId, double score) {
  }
}
