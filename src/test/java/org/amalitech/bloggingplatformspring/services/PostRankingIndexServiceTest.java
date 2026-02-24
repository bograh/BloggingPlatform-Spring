package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostCommentCountProjection;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.utils.PostUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostRankingIndexServiceTest {

  @Mock
  private PostRepository postRepository;

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private PostUtils postUtils;

  @InjectMocks
  private PostRankingIndexService postRankingIndexService;

  @Test
  void getPopularPosts_WhenGroupedAggregationMissesPosts_ShouldFallbackToCountByPostId() {
    Post firstPost = buildPost(1L, "first");
    Post secondPost = buildPost(2L, "second");

    when(postRepository.findAllWithAuthorAndTags()).thenReturn(List.of(firstPost, secondPost));

    PostCommentCountProjection firstProjection = mock(PostCommentCountProjection.class);
    when(firstProjection.getPostId()).thenReturn(1L);
    when(firstProjection.getTotalComments()).thenReturn(4L);
    when(commentRepository.countCommentsByPostIds(List.of(1L, 2L))).thenReturn(List.of(firstProjection));
    when(commentRepository.countByPostId(2L)).thenReturn(9L);

    when(postUtils.createPostResponseFromPost(any(Post.class), anyLong())).thenAnswer(invocation -> {
      Post post = invocation.getArgument(0);
      Long totalComments = invocation.getArgument(1);
      PostResponseDTO dto = new PostResponseDTO();
      dto.setId(post.getId());
      dto.setTotalComments(totalComments);
      return dto;
    });

    List<PostResponseDTO> result = postRankingIndexService.getPopularPosts(10);

    assertFalse(result.isEmpty());

    ArgumentCaptor<Long> totalCommentsCaptor = ArgumentCaptor.forClass(Long.class);
    verify(postUtils, times(2)).createPostResponseFromPost(any(Post.class), totalCommentsCaptor.capture());
    List<Long> capturedTotals = totalCommentsCaptor.getAllValues();
    assertEquals(Set.of(4L, 9L), Set.copyOf(capturedTotals));

    verify(commentRepository).countByPostId(2L);
  }

  private Post buildPost(Long postId, String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);

    Post post = new Post();
    post.setId(postId);
    post.setTitle("title-" + postId);
    post.setBody("body-" + postId);
    post.setAuthor(user);
    post.setUpdatedAt(LocalDateTime.now());
    post.setTags(Set.of());
    return post;
  }
}
