package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.dtos.requests.CreateCommentDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.DeleteCommentRequestDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.InvalidUserIdFormatException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.CommentUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentService commentService;

    private UUID userId;
    private User user;
    private Post post;
    private Comment comment;
    private CreateCommentDTO createCommentDTO;
    private DeleteCommentRequestDTO deleteCommentRequestDTO;
    private CommentResponse commentResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        post = new Post();
        post.setId(1L);
        post.setTitle("Test Post");
        post.setBody("Test Body");
        post.setAuthor(user);

        comment = new Comment();
        comment.setId("comment-123");
        comment.setContent("Test comment content");
        comment.setPostId(1L);
        comment.setAuthorId(userId.toString());
        comment.setAuthor("testuser");
        comment.setCommentedAt(LocalDateTime.now());

        createCommentDTO = new CreateCommentDTO();
        createCommentDTO.setAuthorId(userId.toString());
        createCommentDTO.setPostId(1L);
        createCommentDTO.setCommentContent("Test comment content");

        deleteCommentRequestDTO = new DeleteCommentRequestDTO();
        deleteCommentRequestDTO.setAuthorId(userId.toString());

        commentResponse = new CommentResponse();
        commentResponse.setId("comment-123");
        commentResponse.setContent("Test comment content");
        commentResponse.setPostId(1L);
        commentResponse.setAuthor("testuser");
        commentResponse.setCreatedAt(String.valueOf(LocalDateTime.now()));
    }


    @Test
    void addCommentToPost_WithValidData_ShouldReturnCommentResponse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        try (MockedStatic<CommentUtils> commentUtilsMock = mockStatic(CommentUtils.class)) {
            commentUtilsMock.when(() -> CommentUtils.createCommentResponseFromComment(any(Comment.class)))
                    .thenReturn(commentResponse);

            CommentResponse result = commentService.addCommentToPost(createCommentDTO);

            assertNotNull(result);
            assertEquals(commentResponse.getId(), result.getId());
            assertEquals(commentResponse.getContent(), result.getContent());
            assertEquals(commentResponse.getAuthor(), result.getAuthor());

            verify(userRepository).findById(userId);
            verify(commentRepository).save(argThat(c ->
                    c.getContent().equals("Test comment content") &&
                            c.getPostId().equals(1L) &&
                            c.getAuthorId().equals(userId.toString()) &&
                            c.getAuthor().equals("testuser") &&
                            c.getCommentedAt() != null
            ));
            commentUtilsMock.verify(() -> CommentUtils.createCommentResponseFromComment(any(Comment.class)));
        }
    }

    @Test
    void addCommentToPost_WithNonExistentUser_ShouldThrowResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> commentService.addCommentToPost(createCommentDTO)
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addCommentToPost_WithInvalidUUID_ShouldThrowInvalidUserIdFormatException() {
        createCommentDTO.setAuthorId("invalid-uuid");

        InvalidUserIdFormatException exception = assertThrows(
                InvalidUserIdFormatException.class,
                () -> commentService.addCommentToPost(createCommentDTO)
        );

        assertTrue(exception.getMessage().contains("User ID format is invalid"));
        verify(userRepository, never()).findById(any());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addCommentToPost_ShouldSetCurrentTimestamp() {
        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        try (MockedStatic<CommentUtils> commentUtilsMock = mockStatic(CommentUtils.class)) {
            commentUtilsMock.when(() -> CommentUtils.createCommentResponseFromComment(any(Comment.class)))
                    .thenReturn(commentResponse);

            commentService.addCommentToPost(createCommentDTO);
            LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);

            verify(commentRepository).save(argThat(c ->
                    c.getCommentedAt() != null &&
                            c.getCommentedAt().isAfter(beforeCall) &&
                            c.getCommentedAt().isBefore(afterCall)
            ));
        }
    }


    @Test
    void getAllCommentsByPostId_WithValidPostId_ShouldReturnCommentList() {
        Long postId = 1L;
        Comment comment2 = new Comment();
        comment2.setId("comment-456");
        comment2.setContent("Second comment");
        comment2.setPostId(postId);
        comment2.setAuthorId(userId.toString());
        comment2.setAuthor("testuser");
        comment2.setCommentedAt(LocalDateTime.now());

        List<Comment> comments = Arrays.asList(comment, comment2);

        CommentResponse commentResponse2 = new CommentResponse();
        commentResponse2.setId("comment-456");
        commentResponse2.setContent("Second comment");

        when(postRepository.findPostById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findByPostIdOrderByCommentedAtDesc(postId)).thenReturn(comments);

        try (MockedStatic<CommentUtils> commentUtilsMock = mockStatic(CommentUtils.class)) {
            commentUtilsMock.when(() -> CommentUtils.createCommentResponseFromComment(comment))
                    .thenReturn(commentResponse);
            commentUtilsMock.when(() -> CommentUtils.createCommentResponseFromComment(comment2))
                    .thenReturn(commentResponse2);

            List<CommentResponse> result = commentService.getAllCommentsByPostId(postId);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(commentResponse.getId(), result.get(0).getId());
            assertEquals(commentResponse2.getId(), result.get(1).getId());

            verify(postRepository).findPostById(postId);
            verify(commentRepository).findByPostIdOrderByCommentedAtDesc(postId);
            commentUtilsMock.verify(() -> CommentUtils.createCommentResponseFromComment(any(Comment.class)), times(2));
        }
    }

    @Test
    void getAllCommentsByPostId_WithNoComments_ShouldReturnEmptyList() {
        Long postId = 1L;
        when(postRepository.findPostById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findByPostIdOrderByCommentedAtDesc(postId)).thenReturn(Collections.emptyList());

        List<CommentResponse> result = commentService.getAllCommentsByPostId(postId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(postRepository).findPostById(postId);
        verify(commentRepository).findByPostIdOrderByCommentedAtDesc(postId);
    }

    @Test
    void getAllCommentsByPostId_WithNonExistentPost_ShouldThrowResourceNotFoundException() {
        Long postId = 999L;
        when(postRepository.findPostById(postId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> commentService.getAllCommentsByPostId(postId)
        );

        assertTrue(exception.getMessage().contains("Post not found with ID"));
        verify(postRepository).findPostById(postId);
        verify(commentRepository, never()).findByPostIdOrderByCommentedAtDesc(anyLong());
    }

    @Test
    void getAllCommentsByPostId_ShouldReturnCommentsOrderedByDateDesc() {
        Long postId = 1L;
        List<Comment> comments = Collections.singletonList(comment);

        when(postRepository.findPostById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findByPostIdOrderByCommentedAtDesc(postId)).thenReturn(comments);

        try (MockedStatic<CommentUtils> commentUtilsMock = mockStatic(CommentUtils.class)) {
            commentUtilsMock.when(() -> CommentUtils.createCommentResponseFromComment(any(Comment.class)))
                    .thenReturn(commentResponse);

            commentService.getAllCommentsByPostId(postId);

            verify(commentRepository).findByPostIdOrderByCommentedAtDesc(postId);
        }
    }


    @Test
    void getCommentById_WithValidId_ShouldReturnCommentResponse() {
        String commentId = "comment-123";
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        try (MockedStatic<CommentUtils> commentUtilsMock = mockStatic(CommentUtils.class)) {
            commentUtilsMock.when(() -> CommentUtils.createCommentResponseFromComment(comment))
                    .thenReturn(commentResponse);

            CommentResponse result = commentService.getCommentById(commentId);

            assertNotNull(result);
            assertEquals(commentResponse.getId(), result.getId());
            assertEquals(commentResponse.getContent(), result.getContent());

            verify(commentRepository).findById(commentId);
            commentUtilsMock.verify(() -> CommentUtils.createCommentResponseFromComment(comment));
        }
    }

    @Test
    void getCommentById_WithNonExistentId_ShouldThrowResourceNotFoundException() {
        String commentId = "non-existent-id";
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> commentService.getCommentById(commentId)
        );

        assertTrue(exception.getMessage().contains("Comment not found with id"));
        verify(commentRepository).findById(commentId);
    }

    @Test
    void getCommentById_WithEmptyString_ShouldAttemptToFind() {
        String commentId = "";
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> commentService.getCommentById(commentId)
        );

        verify(commentRepository).findById(commentId);
    }


    @Test
    void deleteComment_WithValidData_ShouldDeleteComment() {
        String commentId = "comment-123";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(commentRepository).deleteCommentById(commentId);

        commentService.deleteComment(commentId, deleteCommentRequestDTO);

        verify(userRepository).findById(userId);
        verify(commentRepository).deleteCommentById(commentId);
    }

    @Test
    void deleteComment_WithNonExistentUser_ShouldThrowResourceNotFoundException() {
        String commentId = "comment-123";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> commentService.deleteComment(commentId, deleteCommentRequestDTO)
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(commentRepository, never()).deleteCommentById(anyString());
    }

    @Test
    void deleteComment_WithInvalidUUID_ShouldThrowInvalidUserIdFormatException() {
        String commentId = "comment-123";
        deleteCommentRequestDTO.setAuthorId("invalid-uuid");

        InvalidUserIdFormatException exception = assertThrows(
                InvalidUserIdFormatException.class,
                () -> commentService.deleteComment(commentId, deleteCommentRequestDTO)
        );

        assertTrue(exception.getMessage().contains("User ID format is invalid"));
        verify(userRepository, never()).findById(any());
        verify(commentRepository, never()).deleteCommentById(anyString());
    }

    @Test
    void deleteComment_WithEmptyCommentId_ShouldCallDelete() {
        String commentId = "";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(commentRepository).deleteCommentById(commentId);

        commentService.deleteComment(commentId, deleteCommentRequestDTO);

        verify(userRepository).findById(userId);
        verify(commentRepository).deleteCommentById(commentId);
    }

    @Test
    void deleteComment_ShouldVerifyUserExistsBeforeDeletion() {
        String commentId = "comment-123";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        commentService.deleteComment(commentId, deleteCommentRequestDTO);

        verify(userRepository).findById(userId);
        verify(commentRepository).deleteCommentById(commentId);
    }
}