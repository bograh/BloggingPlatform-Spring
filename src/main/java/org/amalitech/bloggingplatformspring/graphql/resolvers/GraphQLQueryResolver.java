package org.amalitech.bloggingplatformspring.graphql.resolvers;

import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.requests.PostFilterRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Tag;
import org.amalitech.bloggingplatformspring.graphql.types.*;
import org.amalitech.bloggingplatformspring.graphql.utils.GraphQLUtils;
import org.amalitech.bloggingplatformspring.repository.TagRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class GraphQLQueryResolver {

    private final PostService postService;
    private final CommentService commentService;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final GraphQLUtils graphQLUtils = new GraphQLUtils();

    @QueryMapping
    public GraphQLUser getUser(@Argument UUID userId) {
        return userRepository.findById(userId)
                .map(graphQLUtils::mapUserToGraphQLUser)
                .orElse(null);
    }

    @QueryMapping
    public GraphQLPost getPost(@Argument Long postId) {
        PostResponseDTO post = postService.getPostById(postId);
        return graphQLUtils.mapPostResponseToGraphQLPost(post);
    }

    @QueryMapping
    public GraphQLPostPage getAllPosts(
            @Argument Integer page,
            @Argument Integer size,
            @Argument String sortBy,
            @Argument String sortDirection,
            @Argument String author,
            @Argument List<String> tags,
            @Argument String search
    ) {
        PostFilterRequest postFilterRequest = new PostFilterRequest(author, search, tags);
        PageResponse<PostResponseDTO> posts = postService.getAllPosts(page, size, sortBy, sortDirection, postFilterRequest);

        GraphQLPostPage graphQlPostPage = new GraphQLPostPage();
        graphQlPostPage.setContent(posts.content().stream()
                .map(graphQLUtils::mapPostResponseToGraphQLPost)
                .collect(Collectors.toList()));
        graphQlPostPage.setPageNumber(posts.page());
        graphQlPostPage.setPageSize(posts.size());
        graphQlPostPage.setTotalElements(posts.totalElements());
        graphQlPostPage.setTotalPages((int) Math.ceil((double) posts.totalElements() / posts.size()));
        graphQlPostPage.setLast(posts.last());

        return graphQlPostPage;
    }

    @QueryMapping
    public GraphQLComment getComment(@Argument String commentId) {
        CommentResponse comment = commentService.getCommentById(commentId);
        return graphQLUtils.mapCommentResponseToGraphQLComment(comment);
    }

    @QueryMapping
    public List<GraphQLComment> getCommentsByPost(@Argument Long postId) {
        List<CommentResponse> comments = commentService.getAllCommentsByPostId(postId);
        return comments.stream()
                .map(graphQLUtils::mapCommentResponseToGraphQLComment)
                .collect(Collectors.toList());
    }

    @QueryMapping
    public List<GraphQLTag> getAllTags() {
        List<Tag> tags = tagRepository.findAll();
        return tags.stream()
                .map(tag -> new GraphQLTag(tag.getId(), tag.getName()))
                .collect(Collectors.toList());
    }

    @SchemaMapping(typeName = "Post", field = "author")
    public GraphQLUser getPostAuthor(GraphQLPost post) {
        return userRepository.findUserByUsernameIgnoreCase(post.getAuthor())
                .map(graphQLUtils::mapUserToGraphQLUser)
                .orElse(null);
    }
}