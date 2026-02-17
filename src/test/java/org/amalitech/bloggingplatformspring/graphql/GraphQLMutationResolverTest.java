package org.amalitech.bloggingplatformspring.graphql;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import jakarta.servlet.http.HttpServletRequest;
import org.amalitech.bloggingplatformspring.dtos.requests.*;
import org.amalitech.bloggingplatformspring.dtos.responses.AuthResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.AuthResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.graphql.resolvers.GraphQLMutationResolver;
import org.amalitech.bloggingplatformspring.graphql.types.*;
import org.amalitech.bloggingplatformspring.graphql.utils.GraphQLUtils;
import org.amalitech.bloggingplatformspring.services.AuthService;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphQLMutationResolverTest {

    @Mock
    private AuthService authService;

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @Mock
    private GraphQLUtils graphQLUtils;

    @Mock
    private DataFetchingEnvironment environment;

    @Mock
    private GraphQLContext graphQLContext;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GraphQLMutationResolver resolver;

    private UUID userId;
    private String userIdString;
    private AuthResponseDTO authResponse;
    private PostResponseDTO postResponse;
    private GraphQLPost graphQLPost;
    private CommentResponse commentResponse;
    private GraphQLComment graphQLComment;
    private GraphQLAuthResponse graphQLAuthResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userIdString = String.valueOf(userId);

        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(userIdString);
        userResponseDTO.setUsername("testuser");
        userResponseDTO.setEmail("test@example.com");

        AuthResponse auth = new AuthResponse(userResponseDTO, "test-token");
        authResponse = new AuthResponseDTO("test-refresh-token", auth);

        GraphQLUser graphQLUser = new GraphQLUser();
        graphQLUser.setId(userId);
        graphQLUser.setUsername("testuser");
        graphQLUser.setEmail("test@example.com");

        graphQLAuthResponse = new GraphQLAuthResponse();
        graphQLAuthResponse.setToken("test-token");
        graphQLAuthResponse.setUser(graphQLUser);

        postResponse = new PostResponseDTO();
        postResponse.setId(1L);
        postResponse.setTitle("Test Post");
        postResponse.setBody("Test Content");
        postResponse.setAuthor("testuser");
        postResponse.setLastUpdated(String.valueOf(LocalDateTime.now()));
        postResponse.setTags(Arrays.asList("Java", "Spring"));

        graphQLPost = new GraphQLPost();
        graphQLPost.setId(1L);
        graphQLPost.setTitle("Test Post");
        graphQLPost.setBody("Test Content");
        graphQLPost.setAuthor("testuser");

        commentResponse = new CommentResponse();
        commentResponse.setId("comment-123");
        commentResponse.setPostId(1L);
        commentResponse.setAuthor(userResponseDTO.getUsername());
        commentResponse.setContent("Test comment");
        commentResponse.setCreatedAt(String.valueOf(LocalDateTime.now()));

        graphQLComment = new GraphQLComment();
        graphQLComment.setId("comment-123");
        graphQLComment.setContent("Test comment");

        // Setup environment mocks
        lenient().when(environment.getGraphQlContext()).thenReturn(graphQLContext);
        lenient().when(graphQLContext.get("httpServletRequest")).thenReturn(request);

        try (MockedConstruction<GraphQLUtils> mocked = mockConstruction(GraphQLUtils.class,
                (mock, context) -> {
                    lenient().when(mock.createGraphQLAuthResponse(any(AuthResponseDTO.class)))
                            .thenReturn(graphQLAuthResponse);
                    lenient().when(mock.mapPostResponseToGraphQLPost(any(PostResponseDTO.class)))
                            .thenReturn(graphQLPost);
                    lenient().when(mock.mapCommentResponseToGraphQLComment(any(CommentResponse.class)))
                            .thenReturn(graphQLComment);
                })) {
            resolver = new GraphQLMutationResolver(authService, postService, commentService);
        }
    }

    @Test
    void registerUser_WithValidInput_ShouldReturnGraphQLUser() {
        RegisterUserInput input = new RegisterUserInput();
        input.setUsername("testuser");
        input.setEmail("test@example.com");
        input.setPassword("password123");

        when(authService.registerUser(any(RegisterUserDTO.class))).thenReturn(authResponse);

        GraphQLAuthResponse result = resolver.registerUser(input);

        assertThat(result).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(userId);
        assertThat(result.getUser().getUsername()).isEqualTo("testuser");
        assertThat(result.getUser().getEmail()).isEqualTo("test@example.com");

        verify(authService).registerUser(argThat(dto -> dto.getUsername().equals("testuser") &&
                dto.getEmail().equals("test@example.com") &&
                dto.getPassword().equals("password123")));
    }

    @Test
    void registerUser_ShouldCallUserServiceWithCorrectDTO() {
        RegisterUserInput input = new RegisterUserInput();
        input.setUsername("newuser");
        input.setEmail("newuser@example.com");
        input.setPassword("securepass");

        when(authService.registerUser(any(RegisterUserDTO.class))).thenReturn(authResponse);

        resolver.registerUser(input);

        verify(authService).registerUser(argThat(dto -> dto.getUsername().equals("newuser") &&
                dto.getEmail().equals("newuser@example.com") &&
                dto.getPassword().equals("securepass")));
    }

    @Test
    void signInUser_WithValidCredentials_ShouldReturnGraphQLUser() {
        SignInUserInput input = new SignInUserInput();
        input.setEmail("test@example.com");
        input.setPassword("password123");

        when(authService.signInUser(any(SignInUserDTO.class))).thenReturn(authResponse);

        GraphQLAuthResponse result = resolver.signInUser(input);

        assertThat(result).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(userId);
        assertThat(result.getUser().getUsername()).isEqualTo("testuser");
        assertThat(result.getUser().getEmail()).isEqualTo("test@example.com");

        verify(authService).signInUser(argThat(dto -> dto.getEmail().equals("test@example.com") &&
                dto.getPassword().equals("password123")));
    }

    @Test
    void signInUser_ShouldCallUserServiceWithCorrectDTO() {
        SignInUserInput input = new SignInUserInput();
        input.setEmail("user@example.com");
        input.setPassword("mypassword");

        when(authService.signInUser(any(SignInUserDTO.class))).thenReturn(authResponse);

        resolver.signInUser(input);

        verify(authService).signInUser(argThat(dto -> dto.getEmail().equals("user@example.com") &&
                dto.getPassword().equals("mypassword")));
    }

    @Test
    void createPost_WithValidInput_ShouldReturnGraphQLPost() {
        CreatePostInput input = new CreatePostInput();
        input.setTitle("New Post");
        input.setBody("Post content");
        input.setTags(Arrays.asList("Java", "Spring"));

        when(postService.createPost(any(CreatePostDTO.class), any(HttpServletRequest.class))).thenReturn(postResponse);

        GraphQLPost result = resolver.createPost(input, environment);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Post");

        verify(postService).createPost(argThat(dto -> dto.getTitle().equals("New Post") &&
                dto.getBody().equals("Post content") &&
                dto.getTags().containsAll(Arrays.asList("Java", "Spring"))), any(HttpServletRequest.class));
    }

    @Test
    void createPost_WithEmptyTags_ShouldCreatePost() {
        CreatePostInput input = new CreatePostInput();
        input.setTitle("Post without tags");
        input.setBody("Content");
        input.setTags(List.of());

        when(postService.createPost(any(CreatePostDTO.class), any(HttpServletRequest.class))).thenReturn(postResponse);

        GraphQLPost result = resolver.createPost(input, environment);

        assertThat(result).isNotNull();
        verify(postService).createPost(any(CreatePostDTO.class), any(HttpServletRequest.class));
    }

    @Test
    void createPost_WithMultipleTags_ShouldCreatePost() {
        CreatePostInput input = new CreatePostInput();
        input.setTitle("Multi-tag post");
        input.setBody("Content");
        input.setTags(Arrays.asList("Java", "Spring", "GraphQL", "Testing"));

        when(postService.createPost(any(CreatePostDTO.class), any(HttpServletRequest.class))).thenReturn(postResponse);

        GraphQLPost result = resolver.createPost(input, environment);

        assertThat(result).isNotNull();
        verify(postService).createPost(argThat(dto -> dto.getTags().size() == 4), any(HttpServletRequest.class));
    }

    @Test
    void updatePost_WithValidInput_ShouldReturnUpdatedGraphQLPost() {
        Long postId = 1L;
        UpdatePostInput input = new UpdatePostInput();
        input.setTitle("Updated Title");
        input.setBody("Updated content");
        input.setTags(Arrays.asList("Updated", "Tags"));

        when(postService.updatePost(eq(postId), any(UpdatePostDTO.class), any(HttpServletRequest.class)))
                .thenReturn(postResponse);

        GraphQLPost result = resolver.updatePost(postId, input, environment);

        assertThat(result).isNotNull();
        verify(postService).updatePost(eq(postId), argThat(dto -> dto.getTitle().equals("Updated Title") &&
                dto.getBody().equals("Updated content") &&
                dto.getTags().containsAll(Arrays.asList("Updated", "Tags"))), any(HttpServletRequest.class));
    }

    @Test
    void updatePost_ShouldPassCorrectPostId() {
        Long postId = 42L;
        UpdatePostInput input = new UpdatePostInput();
        input.setTitle("Title");
        input.setBody("Body");
        input.setTags(List.of("Tag"));

        when(postService.updatePost(eq(postId), any(UpdatePostDTO.class), any(HttpServletRequest.class)))
                .thenReturn(postResponse);

        resolver.updatePost(postId, input, environment);

        verify(postService).updatePost(eq(42L), any(UpdatePostDTO.class), any(HttpServletRequest.class));
    }

    @Test
    void deletePost_WithValidInput_ShouldReturnTrue() {
        Long postId = 1L;

        doNothing().when(postService).deletePost(eq(postId), any(HttpServletRequest.class));

        Boolean result = resolver.deletePost(postId, environment);

        assertThat(result).isTrue();
        verify(postService).deletePost(eq(postId), any(HttpServletRequest.class));
    }

    @Test
    void deletePost_ShouldCallPostServiceWithCorrectParameters() {
        Long postId = 5L;

        doNothing().when(postService).deletePost(eq(postId), any(HttpServletRequest.class));

        resolver.deletePost(postId, environment);

        verify(postService).deletePost(eq(5L), any(HttpServletRequest.class));
    }

    @Test
    void deletePost_AlwaysReturnsTrue() {
        Long postId = 1L;

        doNothing().when(postService).deletePost(anyLong(), any(HttpServletRequest.class));

        Boolean result = resolver.deletePost(postId, environment);

        assertThat(result).isTrue();
    }

    @Test
    void createComment_WithValidInput_ShouldReturnGraphQLComment() {
        CreateCommentInput input = new CreateCommentInput();
        input.setPostId(1L);
        input.setCommentContent("Great post!");

        when(commentService.addCommentToPost(any(CreateCommentDTO.class), any(HttpServletRequest.class)))
                .thenReturn(commentResponse);

        GraphQLComment result = resolver.createComment(input, environment);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("comment-123");
        assertThat(result.getContent()).isEqualTo("Test comment");

        ArgumentCaptor<CreateCommentDTO> captor = ArgumentCaptor.forClass(CreateCommentDTO.class);

        verify(commentService, times(1)).addCommentToPost(captor.capture(), any(HttpServletRequest.class));

        CreateCommentDTO dto = captor.getValue();
        assertThat(dto.getPostId()).isEqualTo(1L);
        assertThat(dto.getCommentContent()).isEqualTo("Great post!");
    }

    @Test
    void createComment_ShouldCallCommentServiceWithCorrectDTO() {
        CreateCommentInput input = new CreateCommentInput();
        input.setPostId(10L);
        input.setCommentContent("Nice article");

        when(commentService.addCommentToPost(any(CreateCommentDTO.class), any(HttpServletRequest.class)))
                .thenReturn(commentResponse);

        resolver.createComment(input, environment);
        ArgumentCaptor<CreateCommentDTO> captor = ArgumentCaptor.forClass(CreateCommentDTO.class);

        verify(commentService, times(1)).addCommentToPost(captor.capture(), any(HttpServletRequest.class));

        CreateCommentDTO dto = captor.getValue();
        assertThat(dto.getPostId()).isEqualTo(10L);
        assertThat(dto.getCommentContent()).isEqualTo("Nice article");
    }

    @Test
    void createComment_WithLongContent_ShouldCreateComment() {
        CreateCommentInput input = new CreateCommentInput();
        input.setPostId(1L);
        input.setCommentContent(
                "This is a very long comment with lots of text to test that the system can handle longer comments properly.");

        when(commentService.addCommentToPost(any(CreateCommentDTO.class), any(HttpServletRequest.class)))
                .thenReturn(commentResponse);

        GraphQLComment result = resolver.createComment(input, environment);

        assertThat(result).isNotNull();
        verify(commentService).addCommentToPost(any(CreateCommentDTO.class), any(HttpServletRequest.class));
    }

    @Test
    void deleteComment_WithValidInput_ShouldReturnTrue() {
        String commentId = "comment-123";
        DeleteCommentInput input = new DeleteCommentInput();
        input.setPostId(1L);

        doNothing().when(commentService).deleteComment(eq(commentId), any(DeleteCommentRequestDTO.class),
                any(HttpServletRequest.class));

        Boolean result = resolver.deleteComment(commentId, input, environment);

        assertThat(result).isTrue();
        verify(commentService).deleteComment(eq(commentId), argThat(dto -> dto.getPostId().equals(1L)),
                any(HttpServletRequest.class));
    }

    @Test
    void deleteComment_ShouldCallCommentServiceWithCorrectParameters() {
        String commentId = "comment-456";
        DeleteCommentInput input = new DeleteCommentInput();
        input.setPostId(2L);

        doNothing().when(commentService).deleteComment(eq(commentId), any(DeleteCommentRequestDTO.class),
                any(HttpServletRequest.class));

        resolver.deleteComment(commentId, input, environment);

        verify(commentService).deleteComment(eq("comment-456"), argThat(dto -> dto.getPostId().equals(2L)),
                any(HttpServletRequest.class));
    }

    @Test
    void deleteComment_AlwaysReturnsTrue() {
        String commentId = "comment-789";
        DeleteCommentInput input = new DeleteCommentInput();
        input.setPostId(3L);

        doNothing().when(commentService).deleteComment(anyString(), any(DeleteCommentRequestDTO.class),
                any(HttpServletRequest.class));

        Boolean result = resolver.deleteComment(commentId, input, environment);

        assertThat(result).isTrue();
    }

    @Test
    void deleteComment_WithDifferentAuthor_ShouldCallService() {
        String commentId = "comment-123";
        DeleteCommentInput input = new DeleteCommentInput();
        input.setPostId(5L);

        doNothing().when(commentService).deleteComment(eq(commentId), any(DeleteCommentRequestDTO.class),
                any(HttpServletRequest.class));

        resolver.deleteComment(commentId, input, environment);

        verify(commentService).deleteComment(eq(commentId), argThat(dto -> dto.getPostId().equals(5L)),
                any(HttpServletRequest.class));
    }
}