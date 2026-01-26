package org.amalitech.bloggingplatformspring.graphql;

import org.amalitech.bloggingplatformspring.dtos.requests.*;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.graphql.resolvers.GraphQLMutationResolver;
import org.amalitech.bloggingplatformspring.graphql.types.*;
import org.amalitech.bloggingplatformspring.graphql.utils.GraphQLUtils;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.amalitech.bloggingplatformspring.services.UserService;
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
    private UserService userService;

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @Mock
    private GraphQLUtils graphQLUtils;

    @InjectMocks
    private GraphQLMutationResolver resolver;

    private UUID userId;
    private String userIdString;
    private UserResponseDTO userResponse;
    private PostResponseDTO postResponse;
    private GraphQLPost graphQLPost;
    private CommentResponse commentResponse;
    private GraphQLComment graphQLComment;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userIdString = String.valueOf(userId);

        userResponse = new UserResponseDTO();
        userResponse.setId(userIdString);
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@example.com");

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
        commentResponse.setAuthor(userResponse.getUsername());
        commentResponse.setContent("Test comment");
        commentResponse.setCreatedAt(String.valueOf(LocalDateTime.now()));

        graphQLComment = new GraphQLComment();
        graphQLComment.setId("comment-123");
        graphQLComment.setContent("Test comment");

        try (MockedConstruction<GraphQLUtils> mocked = mockConstruction(GraphQLUtils.class,
                (mock, context) -> {
                    when(mock.mapPostResponseToGraphQLPost(any(PostResponseDTO.class))).thenReturn(graphQLPost);
                    when(mock.mapCommentResponseToGraphQLComment(any(CommentResponse.class))).thenReturn(graphQLComment);
                })) {
            resolver = new GraphQLMutationResolver(userService, postService, commentService);
        }
    }


    @Test
    void registerUser_WithValidInput_ShouldReturnGraphQLUser() {
        RegisterUserInput input = new RegisterUserInput();
        input.setUsername("testuser");
        input.setEmail("test@example.com");
        input.setPassword("password123");

        when(userService.registerUser(any(RegisterUserDTO.class))).thenReturn(userResponse);

        GraphQLUser result = resolver.registerUser(input);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");

        verify(userService).registerUser(argThat(dto ->
                dto.getUsername().equals("testuser") &&
                        dto.getEmail().equals("test@example.com") &&
                        dto.getPassword().equals("password123")
        ));
    }

    @Test
    void registerUser_ShouldCallUserServiceWithCorrectDTO() {
        RegisterUserInput input = new RegisterUserInput();
        input.setUsername("newuser");
        input.setEmail("newuser@example.com");
        input.setPassword("securepass");

        when(userService.registerUser(any(RegisterUserDTO.class))).thenReturn(userResponse);

        resolver.registerUser(input);

        verify(userService).registerUser(argThat(dto ->
                dto.getUsername().equals("newuser") &&
                        dto.getEmail().equals("newuser@example.com") &&
                        dto.getPassword().equals("securepass")
        ));
    }


    @Test
    void signInUser_WithValidCredentials_ShouldReturnGraphQLUser() {
        SignInUserInput input = new SignInUserInput();
        input.setEmail("test@example.com");
        input.setPassword("password123");

        when(userService.signInUser(any(SignInUserDTO.class))).thenReturn(userResponse);

        GraphQLUser result = resolver.signInUser(input);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");

        verify(userService).signInUser(argThat(dto ->
                dto.getEmail().equals("test@example.com") &&
                        dto.getPassword().equals("password123")
        ));
    }

    @Test
    void signInUser_ShouldCallUserServiceWithCorrectDTO() {
        SignInUserInput input = new SignInUserInput();
        input.setEmail("user@example.com");
        input.setPassword("mypassword");

        when(userService.signInUser(any(SignInUserDTO.class))).thenReturn(userResponse);

        resolver.signInUser(input);

        verify(userService).signInUser(argThat(dto ->
                dto.getEmail().equals("user@example.com") &&
                        dto.getPassword().equals("mypassword")
        ));
    }


    @Test
    void createPost_WithValidInput_ShouldReturnGraphQLPost() {
        CreatePostInput input = new CreatePostInput();
        input.setTitle("New Post");
        input.setBody("Post content");
        input.setAuthorId(userIdString);
        input.setTags(Arrays.asList("Java", "Spring"));

        when(postService.createPost(any(CreatePostDTO.class))).thenReturn(postResponse);

        GraphQLPost result = resolver.createPost(input);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Post");

        verify(postService).createPost(argThat(dto ->
                dto.getTitle().equals("New Post") &&
                        dto.getBody().equals("Post content") &&
                        dto.getAuthorId().equals(userIdString) &&
                        dto.getTags().containsAll(Arrays.asList("Java", "Spring"))
        ));
    }

    @Test
    void createPost_WithEmptyTags_ShouldCreatePost() {
        CreatePostInput input = new CreatePostInput();
        input.setTitle("Post without tags");
        input.setBody("Content");
        input.setAuthorId(userIdString);
        input.setTags(List.of());

        when(postService.createPost(any(CreatePostDTO.class))).thenReturn(postResponse);

        GraphQLPost result = resolver.createPost(input);

        assertThat(result).isNotNull();
        verify(postService).createPost(any(CreatePostDTO.class));
    }

    @Test
    void createPost_WithMultipleTags_ShouldCreatePost() {
        CreatePostInput input = new CreatePostInput();
        input.setTitle("Multi-tag post");
        input.setBody("Content");
        input.setAuthorId(userIdString);
        input.setTags(Arrays.asList("Java", "Spring", "GraphQL", "Testing"));

        when(postService.createPost(any(CreatePostDTO.class))).thenReturn(postResponse);

        GraphQLPost result = resolver.createPost(input);

        assertThat(result).isNotNull();
        verify(postService).createPost(argThat(dto ->
                dto.getTags().size() == 4
        ));
    }


    @Test
    void updatePost_WithValidInput_ShouldReturnUpdatedGraphQLPost() {
        Long postId = 1L;
        UpdatePostInput input = new UpdatePostInput();
        input.setTitle("Updated Title");
        input.setBody("Updated content");
        input.setAuthorId(userIdString);
        input.setTags(Arrays.asList("Updated", "Tags"));

        when(postService.updatePost(eq(postId), any(UpdatePostDTO.class))).thenReturn(postResponse);

        GraphQLPost result = resolver.updatePost(postId, input);

        assertThat(result).isNotNull();
        verify(postService).updatePost(eq(postId), argThat(dto ->
                dto.getTitle().equals("Updated Title") &&
                        dto.getBody().equals("Updated content") &&
                        dto.getAuthorId().equals(userIdString) &&
                        dto.getTags().containsAll(Arrays.asList("Updated", "Tags"))
        ));
    }

    @Test
    void updatePost_ShouldPassCorrectPostId() {
        Long postId = 42L;
        UpdatePostInput input = new UpdatePostInput();
        input.setTitle("Title");
        input.setBody("Body");
        input.setAuthorId(userIdString);
        input.setTags(List.of("Tag"));

        when(postService.updatePost(eq(postId), any(UpdatePostDTO.class))).thenReturn(postResponse);

        resolver.updatePost(postId, input);

        verify(postService).updatePost(eq(42L), any(UpdatePostDTO.class));
    }


    @Test
    void deletePost_WithValidInput_ShouldReturnTrue() {
        Long postId = 1L;
        String authorId = userIdString;

        doNothing().when(postService).deletePost(eq(postId), any(DeletePostRequestDTO.class));

        Boolean result = resolver.deletePost(postId, authorId);

        assertThat(result).isTrue();
        verify(postService).deletePost(eq(postId), argThat(dto ->
                dto.getAuthorId().equals(authorId)
        ));
    }

    @Test
    void deletePost_ShouldCallPostServiceWithCorrectParameters() {
        Long postId = 5L;
        String authorId = UUID.randomUUID().toString();

        doNothing().when(postService).deletePost(eq(postId), any(DeletePostRequestDTO.class));

        resolver.deletePost(postId, authorId);

        verify(postService).deletePost(eq(5L), argThat(dto ->
                dto.getAuthorId().equals(authorId)
        ));
    }

    @Test
    void deletePost_AlwaysReturnsTrue() {
        Long postId = 1L;
        String authorId = userIdString;

        doNothing().when(postService).deletePost(anyLong(), any(DeletePostRequestDTO.class));

        Boolean result = resolver.deletePost(postId, authorId);

        assertThat(result).isTrue();
    }

    @Test
    void createComment_WithValidInput_ShouldReturnGraphQLComment() {
        CreateCommentInput input = new CreateCommentInput();
        input.setPostId(1L);
        input.setAuthorId(userIdString);
        input.setCommentContent("Great post!");

        when(commentService.addCommentToPost(any(CreateCommentDTO.class)))
                .thenReturn(commentResponse);

        GraphQLComment result = resolver.createComment(input);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("comment-123");
        assertThat(result.getContent()).isEqualTo("Test comment");

        ArgumentCaptor<CreateCommentDTO> captor =
                ArgumentCaptor.forClass(CreateCommentDTO.class);

        verify(commentService, times(1)).addCommentToPost(captor.capture());

        CreateCommentDTO dto = captor.getValue();
        assertThat(dto.getPostId()).isEqualTo(1L);
        assertThat(dto.getAuthorId()).isEqualTo(userIdString);
        assertThat(dto.getCommentContent()).isEqualTo("Great post!");
    }

    @Test
    void createComment_ShouldCallCommentServiceWithCorrectDTO() {
        CreateCommentInput input = new CreateCommentInput();
        input.setPostId(10L);
        input.setAuthorId(userIdString);
        input.setCommentContent("Nice article");

        when(commentService.addCommentToPost(any(CreateCommentDTO.class)))
                .thenReturn(commentResponse);

        resolver.createComment(input);
        ArgumentCaptor<CreateCommentDTO> captor =
                ArgumentCaptor.forClass(CreateCommentDTO.class);

        verify(commentService, times(1)).addCommentToPost(captor.capture());

        CreateCommentDTO dto = captor.getValue();
        assertThat(dto.getPostId()).isEqualTo(10L);
        assertThat(dto.getAuthorId()).isEqualTo(userIdString);
        assertThat(dto.getCommentContent()).isEqualTo("Nice article");
    }


    @Test
    void createComment_WithLongContent_ShouldCreateComment() {
        CreateCommentInput input = new CreateCommentInput();
        input.setPostId(1L);
        input.setAuthorId(userIdString);
        input.setCommentContent("This is a very long comment with lots of text to test that the system can handle longer comments properly.");

        when(commentService.addCommentToPost(any(CreateCommentDTO.class))).thenReturn(commentResponse);

        GraphQLComment result = resolver.createComment(input);

        assertThat(result).isNotNull();
        verify(commentService).addCommentToPost(any(CreateCommentDTO.class));
    }


    @Test
    void deleteComment_WithValidInput_ShouldReturnTrue() {
        String commentId = "comment-123";
        DeleteCommentInput input = new DeleteCommentInput();
        input.setAuthorId(userIdString);

        doNothing().when(commentService).deleteComment(eq(commentId), any(DeleteCommentRequestDTO.class));

        Boolean result = resolver.deleteComment(commentId, input);

        assertThat(result).isTrue();
        verify(commentService).deleteComment(eq(commentId), argThat(dto ->
                dto.getAuthorId().equals(userIdString)
        ));
    }

    @Test
    void deleteComment_ShouldCallCommentServiceWithCorrectParameters() {
        String commentId = "comment-456";
        DeleteCommentInput input = new DeleteCommentInput();
        input.setAuthorId(userIdString);

        doNothing().when(commentService).deleteComment(eq(commentId), any(DeleteCommentRequestDTO.class));

        resolver.deleteComment(commentId, input);

        verify(commentService).deleteComment(eq("comment-456"), argThat(dto ->
                dto.getAuthorId().equals(userIdString)
        ));
    }

    @Test
    void deleteComment_AlwaysReturnsTrue() {
        String commentId = "comment-789";
        DeleteCommentInput input = new DeleteCommentInput();
        input.setAuthorId(userIdString);

        doNothing().when(commentService).deleteComment(anyString(), any(DeleteCommentRequestDTO.class));

        Boolean result = resolver.deleteComment(commentId, input);

        assertThat(result).isTrue();
    }

    @Test
    void deleteComment_WithDifferentAuthor_ShouldCallService() {
        String commentId = "comment-123";
        String differentAuthorId = UUID.randomUUID().toString();
        DeleteCommentInput input = new DeleteCommentInput();
        input.setAuthorId(differentAuthorId);

        doNothing().when(commentService).deleteComment(eq(commentId), any(DeleteCommentRequestDTO.class));

        resolver.deleteComment(commentId, input);

        verify(commentService).deleteComment(eq(commentId), argThat(dto ->
                dto.getAuthorId().equals(differentAuthorId)
        ));
    }
}