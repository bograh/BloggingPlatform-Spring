package org.amalitech.bloggingplatformspring.graphql.resolvers;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.requests.*;
import org.amalitech.bloggingplatformspring.dtos.responses.AuthResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.graphql.types.*;
import org.amalitech.bloggingplatformspring.graphql.utils.GraphQLUtils;
import org.amalitech.bloggingplatformspring.services.AuthService;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GraphQLMutationResolver {

    private final AuthService authService;
    private final PostService postService;
    private final CommentService commentService;
    private final GraphQLUtils graphQLUtils = new GraphQLUtils();

    @MutationMapping
    public GraphQLAuthResponse registerUser(@Argument RegisterUserInput input) {
        RegisterUserDTO dto = new RegisterUserDTO(
                input.getUsername(),
                input.getEmail(),
                input.getPassword());

        AuthResponseDTO authResponseDTO = authService.registerUser(dto);
        return graphQLUtils.createGraphQLAuthResponse(authResponseDTO);
    }

    @MutationMapping
    public GraphQLAuthResponse signInUser(@Argument SignInUserInput input) {
        SignInUserDTO dto = new SignInUserDTO(
                input.getEmail(),
                input.getPassword());

        AuthResponseDTO authResponseDTO = authService.signInUser(dto);
        return graphQLUtils.createGraphQLAuthResponse(authResponseDTO);
    }

    @MutationMapping
    @PreAuthorize("hasRole('AUTHOR')")
    public GraphQLPost createPost(@Argument CreatePostInput input, DataFetchingEnvironment environment) {

        GraphQLContext graphQLContext = environment.getGraphQlContext();
        HttpServletRequest request = graphQLContext.get("httpServletRequest");

        CreatePostDTO dto = new CreatePostDTO(
                input.getTitle(),
                input.getBody(),
                input.getTags());

        PostResponseDTO post = postService.createPost(dto, request);
        return graphQLUtils.mapPostResponseToGraphQLPost(post);
    }

    @MutationMapping
    @PreAuthorize("hasRole('AUTHOR')")
    public GraphQLPost updatePost(
            @Argument Long postId, @Argument UpdatePostInput input, DataFetchingEnvironment environment) {

        GraphQLContext graphQLContext = environment.getGraphQlContext();
        HttpServletRequest request = graphQLContext.get("httpServletRequest");

        UpdatePostDTO dto = new UpdatePostDTO(
                input.getTitle(),
                input.getBody(),
                input.getTags());

        PostResponseDTO post = postService.updatePost(postId, dto, request);
        return graphQLUtils.mapPostResponseToGraphQLPost(post);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public Boolean deletePost(@Argument Long postId, DataFetchingEnvironment environment) {

        GraphQLContext graphQLContext = environment.getGraphQlContext();
        HttpServletRequest request = graphQLContext.get("httpServletRequest");

        postService.deletePost(postId, request);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('AUTHOR')")
    public GraphQLComment createComment(@Argument CreateCommentInput input, DataFetchingEnvironment environment) {

        GraphQLContext graphQLContext = environment.getGraphQlContext();
        HttpServletRequest request = graphQLContext.get("httpServletRequest");

        CreateCommentDTO dto = new CreateCommentDTO(
                input.getPostId(),
                input.getCommentContent());

        CommentResponse comment = commentService.addCommentToPost(dto, request);
        return graphQLUtils.mapCommentResponseToGraphQLComment(comment);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public Boolean deleteComment(@Argument String commentId, @Argument DeleteCommentInput input, DataFetchingEnvironment environment) {

        GraphQLContext graphQLContext = environment.getGraphQlContext();
        HttpServletRequest request = graphQLContext.get("httpServletRequest");

        DeleteCommentRequestDTO dto = new DeleteCommentRequestDTO(input.getPostId());
        commentService.deleteComment(commentId, dto, request);
        return true;
    }

}