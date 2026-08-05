package org.amalitech.bloggingplatformspring.utils;

import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostCommentCountProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostUtilsTest {

  @Mock
  private CommentRepository commentRepository;

  private PostUtils postUtils;

  @BeforeEach
  void setUp() {
    postUtils = new PostUtils(commentRepository);
  }

  @Test
  void mapPostPageToPostResponsePage_ShouldUseAggregatedCommentCounts() {
    Post firstPost = buildPost(1L, "first");
    Post secondPost = buildPost(2L, "second");
    Page<Post> postPage = new PageImpl<>(List.of(firstPost, secondPost), PageRequest.of(0, 10), 2);

    PostCommentCountProjection firstProjection = mock(PostCommentCountProjection.class);
    when(firstProjection.getPostId()).thenReturn(1L);
    when(firstProjection.getTotalComments()).thenReturn(3L);

    PostCommentCountProjection secondProjection = mock(PostCommentCountProjection.class);
    when(secondProjection.getPostId()).thenReturn(2L);
    when(secondProjection.getTotalComments()).thenReturn(7L);

    when(commentRepository.countCommentsByPostIds(List.of(1L, 2L)))
        .thenReturn(List.of(firstProjection, secondProjection));

    PageResponse<PostResponseDTO> response = postUtils.mapPostPageToPostResponsePage(postPage);

    assertEquals(3L, response.content().get(0).getTotalComments());
    assertEquals(7L, response.content().get(1).getTotalComments());
    verify(commentRepository, never()).countByPostId(anyLong());
  }

  @Test
  void mapPostPageToPostResponsePage_ShouldFallbackToSingleCounts_WhenAggregationReturnsNoRows() {
    Post firstPost = buildPost(1L, "first");
    Post secondPost = buildPost(2L, "second");
    Page<Post> postPage = new PageImpl<>(List.of(firstPost, secondPost), PageRequest.of(0, 10), 2);

    when(commentRepository.countCommentsByPostIds(List.of(1L, 2L))).thenReturn(List.of());
    when(commentRepository.countByPostId(1L)).thenReturn(5L);
    when(commentRepository.countByPostId(2L)).thenReturn(1L);

    PageResponse<PostResponseDTO> response = postUtils.mapPostPageToPostResponsePage(postPage);

    assertEquals(5L, response.content().get(0).getTotalComments());
    assertEquals(1L, response.content().get(1).getTotalComments());
    verify(commentRepository).countByPostId(1L);
    verify(commentRepository).countByPostId(2L);
  }

  private Post buildPost(Long id, String title) {
    User author = new User();
    author.setId(UUID.randomUUID());
    author.setUsername("author");

    Post post = new Post();
    post.setId(id);
    post.setTitle(title);
    post.setBody("body");
    post.setAuthor(author);
    post.setTags(java.util.Set.of());
    post.setPostedAt(LocalDateTime.now());
    post.setUpdatedAt(LocalDateTime.now());

    return post;
  }
}
