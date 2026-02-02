package org.amalitech.bloggingplatformspring.graphql.resolvers;

import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.requests.*;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.graphql.types.*;
import org.amalitech.bloggingplatformspring.graphql.utils.GraphQLUtils;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.amalitech.bloggingplatformspring.services.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class GraphQLMutationResolver {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final GraphQLUtils graphQLUtils = new GraphQLUtils();

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
        return graphQLUtils.mapPostResponseToGraphQLPost(post);
    }

    @MutationMapping
    public GraphQLPost updatePost(@Argument Long postId, @Argument UpdatePostInput input) {
        UpdatePostDTO dto = new UpdatePostDTO(
                input.getTitle(),
                input.getBody(),
                input.getAuthorId(),
                input.getTags());

        PostResponseDTO post = postService.updatePost(postId, dto);
        return graphQLUtils.mapPostResponseToGraphQLPost(post);
    }

    @MutationMapping
    public Boolean deletePost(@Argument Long postId, @Argument String authorId) {
        DeletePostRequestDTO dto = new DeletePostRequestDTO(authorId);
        postService.deletePost(postId, dto);
        return true;
    }

    @MutationMapping
    public GraphQLComment createComment(@Argument CreateCommentInput input) {
        CreateCommentDTO dto = new CreateCommentDTO(
                input.getPostId(),
                input.getCommentContent(),
                input.getAuthorId());

        CommentResponse comment = commentService.addCommentToPost(dto);
        return graphQLUtils.mapCommentResponseToGraphQLComment(comment);
    }

    @MutationMapping
    public Boolean deleteComment(@Argument String commentId, @Argument DeleteCommentInput input) {
        DeleteCommentRequestDTO dto = new DeleteCommentRequestDTO(input.getAuthorId(), input.getPostId());
        commentService.deleteComment(commentId, dto);
        return true;
    }

}