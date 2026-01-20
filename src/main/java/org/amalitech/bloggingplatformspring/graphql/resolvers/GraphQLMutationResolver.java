package org.amalitech.bloggingplatformspring.graphql.resolvers;

import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.requests.*;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.entity.CommentDocument;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.graphql.types.*;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.amalitech.bloggingplatformspring.services.UserService;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class GraphQLMutationResolver {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(Constants.DateTimeFormatPattern);

  private final UserService userService;
  private final PostService postService;
  private final CommentService commentService;
  private final PostRepository postRepository;

  @MutationMapping
  public GraphQLUser registerUser(@Argument RegisterUserInput input) {
    RegisterUserDTO dto = new RegisterUserDTO(
        input.getUsername(),
        input.getEmail(),
        input.getPassword());

    UserResponseDTO user = userService.registerUser(dto);
    return new GraphQLUser(
        UUID.fromString(user.getId()),
        user.getUsername(),
        user.getEmail());
  }

  @MutationMapping
  public GraphQLUser signInUser(@Argument SignInUserInput input) {
    SignInUserDTO dto = new SignInUserDTO(
        input.getEmail(),
        input.getPassword());

    UserResponseDTO user = userService.signInUser(dto);
    return new GraphQLUser(
        UUID.fromString(user.getId()),
        user.getUsername(),
        user.getEmail());
  }

  @MutationMapping
  public GraphQLPost createPost(@Argument CreatePostInput input) {
    CreatePostDTO dto = new CreatePostDTO(
        input.getTitle(),
        input.getBody(),
        input.getAuthorId(),
        input.getTags());

    PostResponseDTO post = postService.createPost(dto);
    return mapToGraphQLPost(post);
  }

  @MutationMapping
  public GraphQLPost updatePost(@Argument Integer postId, @Argument UpdatePostInput input) {
    UpdatePostDTO dto = new UpdatePostDTO(
        input.getTitle(),
        input.getBody(),
        input.getAuthorId(),
        input.getTags());

    PostResponseDTO post = postService.updatePost(postId, dto);
    return mapToGraphQLPost(post);
  }

  @MutationMapping
  public Boolean deletePost(@Argument Integer postId, @Argument String authorId) {
    DeletePostRequestDTO dto = new DeletePostRequestDTO(authorId);
    postService.deletePost(postId, dto);
    return true;
  }

  @MutationMapping
  public GraphQLComment createComment(@Argument CreateCommentInput input) {
    CreateCommentDTO dto = new CreateCommentDTO(
        input.getPostId(),
        input.getAuthorId(),
        input.getCommentContent());

    CommentDocument comment = commentService.addCommentToPost(dto);
    return mapToGraphQLComment(comment);
  }

  @MutationMapping
  public Boolean deleteComment(@Argument String commentId, @Argument DeleteCommentInput input) {
    DeleteCommentRequestDTO dto = new DeleteCommentRequestDTO(input.getAuthorId());
    commentService.deleteComment(commentId, dto);
    return true;
  }

  private GraphQLPost mapToGraphQLPost(PostResponseDTO postResponse) {
    List<GraphQLTag> tags = postResponse.getTags().stream()
        .map(tagName -> new GraphQLTag(null, tagName))
        .collect(Collectors.toList());

    LocalDateTime updatedAt = LocalDateTime.parse(postResponse.getLastUpdated(), FORMATTER);

    // Fetch the Post entity to get authorId and createdAt
    try {
      Post post = postRepository.findPostById(postResponse.getId())
          .orElseThrow(() -> new RuntimeException("Post not found"));

      return new GraphQLPost(
          postResponse.getId(),
          postResponse.getTitle(),
          postResponse.getBody(),
          post.getAuthorId().toString(),
          postResponse.getAuthor(),
          tags,
          post.getCreatedAt(),
          updatedAt);
    } catch (SQLException e) {
      throw new RuntimeException("Error fetching post details", e);
    }
  }

  private GraphQLComment mapToGraphQLComment(CommentDocument comment) {
    LocalDateTime createdAt = LocalDateTime.parse(comment.getCreatedAt(), FORMATTER);

    return new GraphQLComment(
        comment.getId(),
        comment.getPostId(),
        comment.getAuthor(),
        comment.getAuthor(),
        comment.getContent(),
        createdAt);
  }
}
