package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.dtos.requests.CreateCommentDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.DeleteCommentRequestDTO;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.entity.CommentDocument;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.InvalidUserIdFormatException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.exceptions.SQLQueryException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

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
    private CommentDocument commentDocument;
    private CreateCommentDTO createCommentDTO;
    private DeleteCommentRequestDTO deleteCommentRequestDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        post = new Post(
                1,
                "Test Post",
                "Test Body",
                userId,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Comment comment = new Comment(
                1,
                userId.toString(),
                "This is a test comment",
                LocalDateTime.now()
        );

        commentDocument = new CommentDocument();
        commentDocument.setId("commentId123");
        commentDocument.setPostId(1);
        commentDocument.setAuthor("testuser");
        commentDocument.setContent("This is a test comment");
        commentDocument.setCreatedAt(LocalDateTime.now().toString());

        createCommentDTO = new CreateCommentDTO();
        createCommentDTO.setPostId(1);
        createCommentDTO.setAuthorId(userId.toString());
        createCommentDTO.setCommentContent("This is a test comment");

        deleteCommentRequestDTO = new DeleteCommentRequestDTO();
        deleteCommentRequestDTO.setAuthorId(userId.toString());
    }

    @Test
    void addCommentToPost_Success() throws SQLException {
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.createComment(any(Comment.class), eq("testuser")))
                .thenReturn(commentDocument);

        CommentDocument result = commentService.addCommentToPost(createCommentDTO);

        assertNotNull(result);
        assertEquals("commentId123", result.getId());
        assertEquals(1, result.getPostId());
        assertEquals("testuser", result.getAuthor());
        assertEquals("This is a test comment", result.getContent());

        verify(userRepository).findUserById(userId);
        verify(commentRepository).createComment(any(Comment.class), eq("testuser"));
    }

    @Test
    void addCommentToPost_VerifyCommentObjectCreation() throws SQLException {
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);

        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.createComment(any(Comment.class), eq("testuser")))
                .thenReturn(commentDocument);

        commentService.addCommentToPost(createCommentDTO);

        verify(commentRepository).createComment(commentCaptor.capture(), eq("testuser"));

        Comment capturedComment = commentCaptor.getValue();
        assertEquals(1, capturedComment.getPostId());
        assertEquals(userId.toString(), capturedComment.getAuthorId());
        assertEquals("This is a test comment", capturedComment.getContent());
        assertNotNull(capturedComment.getCreatedAt());
    }

    @Test
    void addCommentToPost_UserNotFound_ThrowsResourceNotFoundException() throws SQLException {
        when(userRepository.findUserById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> commentService.addCommentToPost(createCommentDTO));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findUserById(userId);
        verify(commentRepository, never()).createComment(any(Comment.class), anyString());
    }

    @Test
    void addCommentToPost_InvalidUserIdFormat_ThrowsInvalidUserIdFormatException() throws SQLException {
        createCommentDTO.setAuthorId("invalid-uuid");

        InvalidUserIdFormatException exception = assertThrows(InvalidUserIdFormatException.class,
                () -> commentService.addCommentToPost(createCommentDTO));

        assertTrue(exception.getMessage().contains("User ID format is invalid"));
        verify(userRepository, never()).findUserById(any());
        verify(commentRepository, never()).createComment(any(Comment.class), anyString());
    }

    @Test
    void addCommentToPost_UserRepositoryThrowsSQLException_ThrowsSQLQueryException() throws SQLException {
        when(userRepository.findUserById(userId))
                .thenThrow(new SQLException("Database connection error"));

        SQLQueryException exception = assertThrows(SQLQueryException.class,
                () -> commentService.addCommentToPost(createCommentDTO));

        assertEquals("Failed to create comment: Database connection error", exception.getMessage());
        verify(commentRepository, never()).createComment(any(Comment.class), anyString());
    }

    @Test
    void addCommentToPost_EmptyCommentContent_StillCreatesComment() throws SQLException {
        createCommentDTO.setCommentContent("");

        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.createComment(any(Comment.class), eq("testuser")))
                .thenReturn(commentDocument);

        CommentDocument result = commentService.addCommentToPost(createCommentDTO);

        assertNotNull(result);
        verify(commentRepository).createComment(any(Comment.class), eq("testuser"));
    }

    @Test
    void addCommentToPost_DifferentUser_Success() throws SQLException {
        UUID differentUserId = UUID.randomUUID();
        User differentUser = new User();
        differentUser.setId(differentUserId);
        differentUser.setUsername("differentuser");

        createCommentDTO.setAuthorId(differentUserId.toString());

        CommentDocument differentCommentDoc = new CommentDocument();
        differentCommentDoc.setId("comment456");
        differentCommentDoc.setAuthor("differentuser");
        differentCommentDoc.setContent("Different comment");

        when(userRepository.findUserById(differentUserId)).thenReturn(Optional.of(differentUser));
        when(commentRepository.createComment(any(Comment.class), eq("differentuser")))
                .thenReturn(differentCommentDoc);

        CommentDocument result = commentService.addCommentToPost(createCommentDTO);

        assertNotNull(result);
        assertEquals("differentuser", result.getAuthor());
        verify(userRepository).findUserById(differentUserId);
    }

    @Test
    void getAllCommentsByPostId_Success_ReturnsComments() throws SQLException {
        List<CommentDocument> comments = Collections.singletonList(commentDocument);

        when(postRepository.findPostById(1)).thenReturn(Optional.of(post));
        when(commentRepository.getAllCommentsByPostId(1)).thenReturn(comments);

        List<CommentDocument> result = commentService.getAllCommentsByPostId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("commentId123", result.getFirst().getId());
        assertEquals("This is a test comment", result.getFirst().getContent());
        verify(postRepository).findPostById(1);
        verify(commentRepository).getAllCommentsByPostId(1);
    }

    @Test
    void getAllCommentsByPostId_Success_ReturnsMultipleComments() throws SQLException {
        CommentDocument comment2 = new CommentDocument();
        comment2.setId("commentId456");
        comment2.setPostId(1);
        comment2.setAuthor("anotheruser");
        comment2.setContent("Second comment");
        comment2.setCreatedAt(LocalDateTime.now().toString());

        List<CommentDocument> comments = Arrays.asList(commentDocument, comment2);

        when(postRepository.findPostById(1)).thenReturn(Optional.of(post));
        when(commentRepository.getAllCommentsByPostId(1)).thenReturn(comments);

        List<CommentDocument> result = commentService.getAllCommentsByPostId(1);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("commentId123", result.getFirst().getId());
        assertEquals("commentId456", result.get(1).getId());
        verify(commentRepository).getAllCommentsByPostId(1);
    }

    @Test
    void getAllCommentsByPostId_NoComments_ReturnsEmptyList() throws SQLException {
        when(postRepository.findPostById(1)).thenReturn(Optional.of(post));
        when(commentRepository.getAllCommentsByPostId(1)).thenReturn(Collections.emptyList());

        List<CommentDocument> result = commentService.getAllCommentsByPostId(1);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(postRepository).findPostById(1);
        verify(commentRepository).getAllCommentsByPostId(1);
    }

    @Test
    void getAllCommentsByPostId_PostNotFound_ThrowsResourceNotFoundException() throws SQLException {
        when(postRepository.findPostById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> commentService.getAllCommentsByPostId(999));

        assertEquals("Post not found with ID: 999", exception.getMessage());
        verify(postRepository).findPostById(999);
        verify(commentRepository, never()).getAllCommentsByPostId(anyInt());
    }

    @Test
    void getAllCommentsByPostId_PostRepositoryThrowsSQLException_ThrowsSQLQueryException() throws SQLException {
        when(postRepository.findPostById(1))
                .thenThrow(new SQLException("Database error"));

        SQLQueryException exception = assertThrows(SQLQueryException.class,
                () -> commentService.getAllCommentsByPostId(1));

        assertEquals("Failed to find comment: Database error", exception.getMessage());
        verify(postRepository).findPostById(1);
        verify(commentRepository, never()).getAllCommentsByPostId(anyInt());
    }

    @Test
    void getAllCommentsByPostId_DifferentPostId_Success() throws SQLException {
        Post differentPost = new Post(
                5,
                "Different Post",
                "Different Body",
                userId,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CommentDocument comment = new CommentDocument();
        comment.setId("comment789");
        comment.setPostId(5);
        comment.setAuthor("testuser");
        comment.setContent("Comment on different post");

        when(postRepository.findPostById(5)).thenReturn(Optional.of(differentPost));
        when(commentRepository.getAllCommentsByPostId(5)).thenReturn(List.of(comment));

        List<CommentDocument> result = commentService.getAllCommentsByPostId(5);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5, result.getFirst().getPostId());
    }

    @Test
    void getCommentById_Success() {
        when(commentRepository.getCommentById("commentId123"))
                .thenReturn(Optional.of(commentDocument));

        CommentDocument result = commentService.getCommentById("commentId123");

        assertNotNull(result);
        assertEquals("commentId123", result.getId());
        assertEquals("This is a test comment", result.getContent());
        assertEquals("testuser", result.getAuthor());
        verify(commentRepository).getCommentById("commentId123");
    }

    @Test
    void getCommentById_CommentNotFound_ThrowsResourceNotFoundException() {
        when(commentRepository.getCommentById("nonexistent"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> commentService.getCommentById("nonexistent"));

        assertEquals("Comment not found with id: nonexistent", exception.getMessage());
        verify(commentRepository).getCommentById("nonexistent");
    }

    @Test
    void getCommentById_WithDifferentCommentId_Success() {
        CommentDocument differentComment = new CommentDocument();
        differentComment.setId("differentId");
        differentComment.setAuthor("anotheruser");
        differentComment.setContent("Different comment");
        differentComment.setPostId(2);

        when(commentRepository.getCommentById("differentId"))
                .thenReturn(Optional.of(differentComment));

        CommentDocument result = commentService.getCommentById("differentId");

        assertNotNull(result);
        assertEquals("differentId", result.getId());
        assertEquals("Different comment", result.getContent());
        assertEquals("anotheruser", result.getAuthor());
    }

    @Test
    void getCommentById_EmptyStringId_ThrowsResourceNotFoundException() {
        when(commentRepository.getCommentById(""))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.getCommentById(""));

        verify(commentRepository).getCommentById("");
    }

    @Test
    void deleteComment_Success() throws SQLException {
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        doNothing().when(commentRepository).deleteComment("commentId123", userId.toString());

        assertDoesNotThrow(() ->
                commentService.deleteComment("commentId123", deleteCommentRequestDTO));

        verify(userRepository).findUserById(userId);
        verify(commentRepository).deleteComment("commentId123", userId.toString());
    }

    @Test
    void deleteComment_UserNotFound_ThrowsResourceNotFoundException() throws SQLException {
        when(userRepository.findUserById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> commentService.deleteComment("commentId123", deleteCommentRequestDTO));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findUserById(userId);
        verify(commentRepository, never()).deleteComment(anyString(), anyString());
    }

    @Test
    void deleteComment_InvalidUserIdFormat_ThrowsInvalidUserIdFormatException() throws SQLException {
        deleteCommentRequestDTO.setAuthorId("invalid-uuid");

        InvalidUserIdFormatException exception = assertThrows(InvalidUserIdFormatException.class,
                () -> commentService.deleteComment("commentId123", deleteCommentRequestDTO));

        assertTrue(exception.getMessage().contains("User ID format is invalid"));
        verify(userRepository, never()).findUserById(any());
        verify(commentRepository, never()).deleteComment(anyString(), anyString());
    }

    @Test
    void deleteComment_UserRepositoryThrowsSQLException_ThrowsSQLQueryException() throws SQLException {
        when(userRepository.findUserById(userId))
                .thenThrow(new SQLException("Database connection error"));

        SQLQueryException exception = assertThrows(SQLQueryException.class,
                () -> commentService.deleteComment("commentId123", deleteCommentRequestDTO));

        assertEquals("Failed to delete comment: Database connection error", exception.getMessage());
        verify(commentRepository, never()).deleteComment(anyString(), anyString());
    }

    @Test
    void deleteComment_DifferentCommentId_Success() throws SQLException {
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        doNothing().when(commentRepository).deleteComment("differentCommentId", userId.toString());

        assertDoesNotThrow(() ->
                commentService.deleteComment("differentCommentId", deleteCommentRequestDTO));

        verify(commentRepository).deleteComment("differentCommentId", userId.toString());
    }

    @Test
    void deleteComment_DifferentUser_Success() throws SQLException {
        UUID differentUserId = UUID.randomUUID();
        User differentUser = new User();
        differentUser.setId(differentUserId);
        differentUser.setUsername("differentuser");

        deleteCommentRequestDTO.setAuthorId(differentUserId.toString());

        when(userRepository.findUserById(differentUserId)).thenReturn(Optional.of(differentUser));
        doNothing().when(commentRepository).deleteComment("commentId123", differentUserId.toString());

        assertDoesNotThrow(() ->
                commentService.deleteComment("commentId123", deleteCommentRequestDTO));

        verify(userRepository).findUserById(differentUserId);
        verify(commentRepository).deleteComment("commentId123", differentUserId.toString());
    }

    @Test
    void deleteComment_EmptyCommentId_StillCallsRepository() throws SQLException {
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        doNothing().when(commentRepository).deleteComment("", userId.toString());

        assertDoesNotThrow(() ->
                commentService.deleteComment("", deleteCommentRequestDTO));

        verify(commentRepository).deleteComment("", userId.toString());
    }

    @Test
    void addCommentToPost_NullAuthorId_ThrowsException() {
        createCommentDTO.setAuthorId(null);

        assertThrows(Exception.class,
                () -> commentService.addCommentToPost(createCommentDTO));

        verify(commentRepository, never()).createComment(any(Comment.class), anyString());
    }

    @Test
    void getAllCommentsByPostId_NegativePostId_ThrowsResourceNotFoundException() throws SQLException {
        when(postRepository.findPostById(-1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.getAllCommentsByPostId(-1));

        verify(postRepository).findPostById(-1);
    }

    @Test
    void deleteComment_VerifyCorrectParametersPassedToRepository() throws SQLException {
        String specificCommentId = "specificComment123";
        String specificAuthorId = UUID.randomUUID().toString();

        deleteCommentRequestDTO.setAuthorId(specificAuthorId);
        UUID specificUserId = UUID.fromString(specificAuthorId);

        User specificUser = new User();
        specificUser.setId(specificUserId);
        specificUser.setUsername("specificuser");

        when(userRepository.findUserById(specificUserId)).thenReturn(Optional.of(specificUser));
        doNothing().when(commentRepository).deleteComment(specificCommentId, specificAuthorId);

        commentService.deleteComment(specificCommentId, deleteCommentRequestDTO);

        verify(commentRepository).deleteComment(specificCommentId, specificAuthorId);
    }

    @Test
    void getAllCommentsByPostId_VerifyEmptyListNotNull() throws SQLException {
        when(postRepository.findPostById(1)).thenReturn(Optional.of(post));
        when(commentRepository.getAllCommentsByPostId(1)).thenReturn(new ArrayList<>());

        List<CommentDocument> result = commentService.getAllCommentsByPostId(1);

        assertNotNull(result);
        assertEquals(0, result.size());
        assertInstanceOf(List.class, result);
    }
}