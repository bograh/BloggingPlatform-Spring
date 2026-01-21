package org.amalitech.bloggingplatformspring.graphql.resolvers;

import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.Tag;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.graphql.types.GraphQLComment;
import org.amalitech.bloggingplatformspring.graphql.types.GraphQLPost;
import org.amalitech.bloggingplatformspring.graphql.types.GraphQLTag;
import org.amalitech.bloggingplatformspring.graphql.types.GraphQLUser;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.TagRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class GraphQLQueryResolver {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(Constants.DateTimeFormatPattern);
    private final PostService postService;
    private final CommentService commentService;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final PostRepository postRepository;

    @QueryMapping
    public GraphQLUser getUser(@Argument UUID userId) throws SQLException {
        return userRepository.findById(userId)
                .map(this::mapToGraphQLUser)
                .orElse(null);
    }

    @QueryMapping
    public GraphQLPost getPost(@Argument Long postId) {
        PostResponseDTO post = postService.getPostById(postId);
        return mapToGraphQLPost(post);
    }

    @QueryMapping
    public List<GraphQLPost> getAllPosts() {
        List<PostResponseDTO> posts = postService.getAllPosts();
        return posts.stream()
                .map(this::mapToGraphQLPost)
                .collect(Collectors.toList());
    }

    /*@QueryMapping
    public GraphQLPostPage getPaginatedPosts(
            @Argument PageRequestInput pageRequest,
            @Argument PostFilterInput filter) {

        int page = (pageRequest != null && pageRequest.getPage() != null) ? pageRequest.getPage() : 0;
        int size = (pageRequest != null && pageRequest.getSize() != null) ? pageRequest.getSize() : 10;
        String sortBy = (pageRequest != null && pageRequest.getSortBy() != null) ? pageRequest.getSortBy()
                : "createdAt";
        String sortDirection = (pageRequest != null && pageRequest.getSortDirection() != null)
                ? pageRequest.getSortDirection()
                : "DESC";

        PageRequest pr = new PageRequest(page, size, sortBy, sortDirection);

        String author = (filter != null) ? filter.getAuthorId() : null;
        String search = (filter != null) ? filter.getKeyword() : null;
        List<String> tags = (filter != null && filter.getTag() != null) ? List.of(filter.getTag()) : null;

        PostFilterRequest pfr = new PostFilterRequest(author, search, tags);

        PageResponse<PostResponseDTO> response = postService.getPaginatedPosts(pr, pfr);

        GraphQLPostPage page2 = new GraphQLPostPage();
        page2.setContent(response.content().stream()
                .map(this::mapToGraphQLPost)
                .collect(Collectors.toList()));
        page2.setPageNumber(response.page());
        page2.setPageSize(response.size());
        page2.setTotalElements((long) response.totalElements());
        page2.setTotalPages((int) Math.ceil((double) response.totalElements() / response.size()));

        return page2;
    }*/

    @QueryMapping
    public GraphQLComment getComment(@Argument String commentId) {
        CommentResponse comment = commentService.getCommentById(commentId);
        return mapToGraphQLComment(comment);
    }

    @QueryMapping
    public List<GraphQLComment> getCommentsByPost(@Argument Integer postId) {
        List<CommentResponse> comments = commentService.getAllCommentsByPostId(postId);
        return comments.stream()
                .map(this::mapToGraphQLComment)
                .collect(Collectors.toList());
    }

    @QueryMapping
    public List<GraphQLTag> getAllTags() throws SQLException {
        List<Tag> tags = tagRepository.findAll();
        return tags.stream()
                .map(tag -> new GraphQLTag(tag.getId(), tag.getName()))
                .collect(Collectors.toList());
    }

    @SchemaMapping(typeName = "Post", field = "author")
    public GraphQLUser getPostAuthor(GraphQLPost post) throws SQLException {
        return userRepository.findUserByUsername(post.getAuthor())
                .map(this::mapToGraphQLUser)
                .orElse(null);
    }

    private GraphQLUser mapToGraphQLUser(User user) {
        return new GraphQLUser(
                user.getId(),
                user.getUsername(),
                user.getEmail());
    }

    private GraphQLPost mapToGraphQLPost(PostResponseDTO postResponse) {
        List<GraphQLTag> tags = postResponse.getTags().stream()
                .map(tagName -> new GraphQLTag(null, tagName))
                .collect(Collectors.toList());

        LocalDateTime updatedAt = LocalDateTime.parse(postResponse.getLastUpdated(), FORMATTER);

        // Fetch the Post entity to get authorId and createdAt
        Post post = postRepository.findPostById(postResponse.getId())
                .orElseThrow(() -> new RuntimeException("Post not found"));

        return new GraphQLPost(
                postResponse.getId(),
                postResponse.getTitle(),
                postResponse.getBody(),
                String.valueOf(post.getAuthor().getId()),
                postResponse.getAuthor(),
                tags,
                post.getPostedAt(),
                updatedAt);

    }

    private GraphQLComment mapToGraphQLComment(CommentResponse comment) {
        LocalDateTime createdAt = LocalDateTime.parse(comment.getCreatedAt(), FORMATTER);

        return new GraphQLComment(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthor(),
                comment.getContent(),
                createdAt);
    }
}