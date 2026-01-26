package org.amalitech.bloggingplatformspring.graphql;

import org.amalitech.bloggingplatformspring.dtos.requests.PostFilterRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Tag;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.graphql.resolvers.GraphQLQueryResolver;
import org.amalitech.bloggingplatformspring.graphql.types.*;
import org.amalitech.bloggingplatformspring.graphql.utils.GraphQLUtils;
import org.amalitech.bloggingplatformspring.repository.TagRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphQLQueryResolverTest {

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private GraphQLUtils graphQLUtils;

    @InjectMocks
    private GraphQLQueryResolver resolver;

    private User user;
    private GraphQLUser graphQLUser;
    private PostResponseDTO postResponse;
    private GraphQLPost graphQLPost;
    private CommentResponse commentResponse;
    private GraphQLComment graphQLComment;
    private Tag tag;

    @BeforeEach
    void setUp() {
        // Setup User
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        graphQLUser = new GraphQLUser();
        graphQLUser.setId(user.getId());
        graphQLUser.setUsername("testuser");
        graphQLUser.setEmail("test@example.com");

        // Setup Post
        postResponse = new PostResponseDTO();
        postResponse.setId(1L);
        postResponse.setTitle("Test Post");
        postResponse.setBody("Test Content");
        postResponse.setAuthor("testuser");

        graphQLPost = new GraphQLPost();
        graphQLPost.setId(1L);
        graphQLPost.setTitle("Test Post");
        graphQLPost.setBody("Test Content");
        graphQLPost.setAuthor("testuser");

        // Setup Comment
        commentResponse = new CommentResponse();
        commentResponse.setId("comment-123");
        commentResponse.setContent("Test comment");
        commentResponse.setAuthor(user.getUsername());

        graphQLComment = new GraphQLComment();
        graphQLComment.setId("comment-123");
        graphQLComment.setContent("Test comment");

        // Setup Tag
        tag = new Tag();
        tag.setId(1L);
        tag.setName("Java");

        // Mock GraphQLUtils construction
        try (MockedConstruction<GraphQLUtils> mocked = mockConstruction(GraphQLUtils.class,
                (mock, context) -> {
                    when(mock.mapUserToGraphQLUser(any(User.class))).thenReturn(graphQLUser);
                    when(mock.mapPostResponseToGraphQLPost(any(PostResponseDTO.class))).thenReturn(graphQLPost);
                    when(mock.mapCommentResponseToGraphQLComment(any(CommentResponse.class))).thenReturn(graphQLComment);
                })) {
            resolver = new GraphQLQueryResolver(postService, commentService, userRepository, tagRepository);
        }
    }

    @Test
    void getUser_WithValidUserId_ShouldReturnGraphQLUser() {
        UUID userId = user.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        GraphQLUser result = resolver.getUser(userId);

        assertThat(result).isNotNull();
        verify(userRepository).findById(userId);
    }

    @Test
    void getUser_WithNonExistentUserId_ShouldReturnNull() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        GraphQLUser result = resolver.getUser(userId);

        assertThat(result).isNull();
        verify(userRepository).findById(userId);
    }

    @Test
    void getPost_WithValidPostId_ShouldReturnGraphQLPost() {
        Long postId = 1L;
        when(postService.getPostById(postId)).thenReturn(postResponse);

        GraphQLPost result = resolver.getPost(postId);

        assertThat(result).isNotNull();
        verify(postService).getPostById(postId);
    }

    @Test
    void getAllPosts_WithNoFilters_ShouldReturnGraphQLPostPage() {
        List<PostResponseDTO> posts = Collections.singletonList(postResponse);
        PageResponse<PostResponseDTO> pageResponse = new PageResponse<>(
                posts, 0, 10,
                Sort.by(Sort.Direction.DESC, "createdAt").toString(), 1
        );

        when(postService.getAllPosts(
                eq(0), eq(10), eq("createdAt"), eq("desc"), any(PostFilterRequest.class)
        )).thenReturn(pageResponse);

        GraphQLPostPage result = resolver.getAllPosts(
                0, 10, "createdAt", "desc", null, null, null
        );

        assertThat(result).isNotNull();
        assertThat(result.getPageNumber()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        verify(postService).getAllPosts(eq(0), eq(10), eq("createdAt"), eq("desc"), any(PostFilterRequest.class));
    }

    @Test
    void getAllPosts_WithFilters_ShouldReturnFilteredGraphQLPostPage() {
        String author = "testuser";
        List<String> tags = Arrays.asList("Java", "Spring");
        String search = "test";

        List<PostResponseDTO> posts = Collections.singletonList(postResponse);
        PageResponse<PostResponseDTO> pageResponse = new PageResponse<>(
                posts, 0, 10,
                Sort.by(Sort.Direction.DESC, "createdAt").toString(), 1
        );

        when(postService.getAllPosts(
                eq(0), eq(10), eq("createdAt"), eq("desc"), any(PostFilterRequest.class)
        )).thenReturn(pageResponse);

        GraphQLPostPage result = resolver.getAllPosts(
                0, 10, "createdAt", "desc", author, tags, search
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(postService).getAllPosts(eq(0), eq(10), eq("createdAt"), eq("desc"), any(PostFilterRequest.class));
    }

    @Test
    void getAllPosts_WithMultiplePages_ShouldCalculateTotalPagesCorrectly() {
        List<PostResponseDTO> posts = Collections.singletonList(postResponse);
        PageResponse<PostResponseDTO> pageResponse = new PageResponse<>(
                posts, 0, 10,
                Sort.by(Sort.Direction.DESC, "createdAt").toString(), 3
        );

        when(postService.getAllPosts(
                eq(0), eq(10), eq("createdAt"), eq("desc"), any(PostFilterRequest.class)
        )).thenReturn(pageResponse);

        GraphQLPostPage result = resolver.getAllPosts(
                0, 10, "createdAt", "desc", null, null, null
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    void getAllPosts_WithEmptyResults_ShouldReturnEmptyPage() {
        PageResponse<PostResponseDTO> pageResponse = new PageResponse<>(
                Collections.emptyList(), 0, 10,
                Sort.by(Sort.Direction.DESC, "createdAt").toString(), 0
        );

        when(postService.getAllPosts(
                eq(0), eq(10), eq("createdAt"), eq("desc"), any(PostFilterRequest.class)
        )).thenReturn(pageResponse);

        GraphQLPostPage result = resolver.getAllPosts(
                0, 10, "createdAt", "desc", null, null, null
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    void getComment_WithValidCommentId_ShouldReturnGraphQLComment() {
        String commentId = "comment-123";
        when(commentService.getCommentById(commentId)).thenReturn(commentResponse);

        GraphQLComment result = resolver.getComment(commentId);

        assertThat(result).isNotNull();
        verify(commentService).getCommentById(commentId);
    }

    @Test
    void getCommentsByPost_WithValidPostId_ShouldReturnListOfGraphQLComments() {
        Long postId = 1L;
        List<CommentResponse> comments = Collections.singletonList(commentResponse);
        when(commentService.getAllCommentsByPostId(postId)).thenReturn(comments);

        List<GraphQLComment> result = resolver.getCommentsByPost(postId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(commentService).getAllCommentsByPostId(postId);
    }

    @Test
    void getCommentsByPost_WithNoComments_ShouldReturnEmptyList() {
        Long postId = 1L;
        when(commentService.getAllCommentsByPostId(postId)).thenReturn(Collections.emptyList());

        List<GraphQLComment> result = resolver.getCommentsByPost(postId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(commentService).getAllCommentsByPostId(postId);
    }

    @Test
    void getCommentsByPost_WithMultipleComments_ShouldReturnAllComments() {
        Long postId = 1L;
        CommentResponse comment2 = new CommentResponse();
        comment2.setId("comment-456");
        comment2.setContent("Another comment");

        List<CommentResponse> comments = Arrays.asList(commentResponse, comment2);
        when(commentService.getAllCommentsByPostId(postId)).thenReturn(comments);

        List<GraphQLComment> result = resolver.getCommentsByPost(postId);

        assertThat(result).hasSize(2);
        verify(commentService).getAllCommentsByPostId(postId);
    }

    @Test
    void getAllTags_ShouldReturnListOfGraphQLTags() {
        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setName("Spring");

        List<Tag> tags = Arrays.asList(tag, tag2);
        when(tagRepository.findAll()).thenReturn(tags);

        List<GraphQLTag> result = resolver.getAllTags();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("Java");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getName()).isEqualTo("Spring");
        verify(tagRepository).findAll();
    }

    @Test
    void getAllTags_WithNoTags_ShouldReturnEmptyList() {
        when(tagRepository.findAll()).thenReturn(Collections.emptyList());

        List<GraphQLTag> result = resolver.getAllTags();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(tagRepository).findAll();
    }

    @Test
    void getPostAuthor_WithValidAuthor_ShouldReturnGraphQLUser() {
        when(userRepository.findUserByUsernameIgnoreCase("testuser"))
                .thenReturn(Optional.of(user));

        GraphQLUser result = resolver.getPostAuthor(graphQLPost);

        assertThat(result).isNotNull();
        verify(userRepository).findUserByUsernameIgnoreCase("testuser");
    }

    @Test
    void getPostAuthor_WithNonExistentAuthor_ShouldReturnNull() {
        when(userRepository.findUserByUsernameIgnoreCase("testuser"))
                .thenReturn(Optional.empty());

        GraphQLUser result = resolver.getPostAuthor(graphQLPost);

        assertThat(result).isNull();
        verify(userRepository).findUserByUsernameIgnoreCase("testuser");
    }

    @Test
    void getPostAuthor_WithCaseInsensitiveUsername_ShouldReturnGraphQLUser() {
        GraphQLPost postWithUppercaseAuthor = new GraphQLPost();
        postWithUppercaseAuthor.setAuthor("TESTUSER");

        when(userRepository.findUserByUsernameIgnoreCase("TESTUSER"))
                .thenReturn(Optional.of(user));

        GraphQLUser result = resolver.getPostAuthor(postWithUppercaseAuthor);

        assertThat(result).isNotNull();
        verify(userRepository).findUserByUsernameIgnoreCase("TESTUSER");
    }
}