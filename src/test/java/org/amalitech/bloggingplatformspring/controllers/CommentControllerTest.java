package org.amalitech.bloggingplatformspring.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.bloggingplatformspring.dtos.requests.CreateCommentDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.DeleteCommentRequestDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.exceptions.UnauthorizedException;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    private ObjectMapper objectMapper;
    private UUID userID;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();
        objectMapper = new ObjectMapper();
        userID = UUID.randomUUID();

        user = new User();
        user.setId(userID);
        user.setUsername("testuser");
        user.setEmail("testuser@email.com");
    }

    @Test
    void addCommentToPost_WithValidData_ShouldReturnCreatedStatus() throws Exception {
        CreateCommentDTO createCommentDTO = new CreateCommentDTO();
        createCommentDTO.setPostId(1L);
        createCommentDTO.setCommentContent("This is a test comment");
        createCommentDTO.setAuthorId(String.valueOf(userID));

        CommentResponse commentResponse = new CommentResponse();
        commentResponse.setId("507f1f77bcf86cd799439011");
        commentResponse.setPostId(1L);
        commentResponse.setContent("This is a test comment");
        commentResponse.setAuthor(user.getUsername());
        commentResponse.setCreatedAt(String.valueOf(LocalDateTime.now()));

        when(commentService.addCommentToPost(any(CreateCommentDTO.class))).thenReturn(commentResponse);

        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCommentDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Comment added to post successfully"))
                .andExpect(jsonPath("$.data.id").value("507f1f77bcf86cd799439011"))
                .andExpect(jsonPath("$.data.postId").value(1))
                .andExpect(jsonPath("$.data.content").value("This is a test comment"))
                .andExpect(jsonPath("$.data.author").value("testuser"));

        verify(commentService).addCommentToPost(any(CreateCommentDTO.class));
    }

    @Test
    void addCommentToPost_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        CreateCommentDTO createCommentDTO = new CreateCommentDTO();

        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCommentDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCommentToPost_WithNonExistentPost_ShouldThrowException() throws Exception {
        CreateCommentDTO createCommentDTO = new CreateCommentDTO();
        createCommentDTO.setPostId(999L);
        createCommentDTO.setCommentContent("Test comment");
        createCommentDTO.setAuthorId(String.valueOf(userID));

        when(commentService.addCommentToPost(any(CreateCommentDTO.class)))
                .thenThrow(new ResourceNotFoundException("Post not found"));

        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCommentDTO)))
                .andExpect(status().is4xxClientError());

        verify(commentService).addCommentToPost(any(CreateCommentDTO.class));
    }

    @Test
    void getAllCommentsByPostId_WithExistingPost_ShouldReturnCommentsList() throws Exception {
        Long postId = 1L;

        CommentResponse comment1 = new CommentResponse();
        comment1.setId("507f1f77bcf86cd799439011");
        comment1.setPostId(postId);
        comment1.setContent("First comment");
        comment1.setAuthor(user.getUsername());

        CommentResponse comment2 = new CommentResponse();
        comment2.setId("507f1f77bcf86cd799439012");
        comment2.setPostId(postId);
        comment2.setContent("Second comment");
        comment2.setAuthor(user.getUsername());

        List<CommentResponse> comments = Arrays.asList(comment1, comment2);

        when(commentService.getAllCommentsByPostId(postId)).thenReturn(comments);

        mockMvc.perform(get("/api/v1/comments/post/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comments for post retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("507f1f77bcf86cd799439011"))
                .andExpect(jsonPath("$.data[0].content").value("First comment"))
                .andExpect(jsonPath("$.data[1].id").value("507f1f77bcf86cd799439012"))
                .andExpect(jsonPath("$.data[1].content").value("Second comment"));

        verify(commentService).getAllCommentsByPostId(postId);
    }

    @Test
    void getAllCommentsByPostId_WithNoComments_ShouldReturnEmptyList() throws Exception {
        Long postId = 1L;
        when(commentService.getAllCommentsByPostId(postId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/comments/post/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comments for post retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(commentService).getAllCommentsByPostId(postId);
    }

    @Test
    void getAllCommentsByPostId_WithNonExistentPost_ShouldThrowException() throws Exception {
        Long postId = 999L;
        when(commentService.getAllCommentsByPostId(postId))
                .thenThrow(new ResourceNotFoundException("Post not found"));

        mockMvc.perform(get("/api/v1/comments/post/{postId}", postId))
                .andExpect(status().is4xxClientError());

        verify(commentService).getAllCommentsByPostId(postId);
    }

    @Test
    void getCommentById_WithValidId_ShouldReturnComment() throws Exception {
        String commentId = "507f1f77bcf86cd799439011";

        CommentResponse commentResponse = new CommentResponse();
        commentResponse.setId(commentId);
        commentResponse.setPostId(1L);
        commentResponse.setContent("Test comment");
        commentResponse.setAuthor(user.getUsername());
        commentResponse.setCreatedAt(String.valueOf(LocalDateTime.now()));

        when(commentService.getCommentById(commentId)).thenReturn(commentResponse);

        mockMvc.perform(get("/api/v1/comments/{commentId}", commentId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Comment retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(commentId))
                .andExpect(jsonPath("$.data.postId").value(1))
                .andExpect(jsonPath("$.data.content").value("Test comment"))
                .andExpect(jsonPath("$.data.author").value("testuser"));

        verify(commentService).getCommentById(commentId);
    }

    @Test
    void getCommentById_WithInvalidId_ShouldThrowException() throws Exception {
        String commentId = "invalid-id";
        when(commentService.getCommentById(commentId))
                .thenThrow(new ResourceNotFoundException("Comment not found"));

        mockMvc.perform(get("/api/v1/comments/{commentId}", commentId))
                .andExpect(status().is4xxClientError());

        verify(commentService).getCommentById(commentId);
    }

    @Test
    void getCommentById_WithNonExistentId_ShouldThrowException() throws Exception {
        String commentId = "507f1f77bcf86cd799439999";
        when(commentService.getCommentById(commentId))
                .thenThrow(new ResourceNotFoundException("Comment not found"));

        mockMvc.perform(get("/api/v1/comments/{commentId}", commentId))
                .andExpect(status().is4xxClientError());

        verify(commentService).getCommentById(commentId);
    }

    @Test
    void deleteComment_WithValidId_ShouldReturnNoContent() throws Exception {
        String commentId = "507f1f77bcf86cd799439011";
        DeleteCommentRequestDTO deleteRequest = new DeleteCommentRequestDTO();
        deleteRequest.setAuthorId(String.valueOf(userID));

        doNothing().when(commentService).deleteComment(eq(commentId), any(DeleteCommentRequestDTO.class));

        mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.message").value("Comment deleted successfully"));

        verify(commentService).deleteComment(eq(commentId), any(DeleteCommentRequestDTO.class));
    }

    @Test
    void deleteComment_WithNonExistentId_ShouldThrowException() throws Exception {
        String commentId = "507f1f77bcf86cd799439999";
        DeleteCommentRequestDTO deleteRequest = new DeleteCommentRequestDTO();
        deleteRequest.setAuthorId(String.valueOf(userID));

        doThrow(new ResourceNotFoundException("Comment not found"))
                .when(commentService).deleteComment(eq(commentId), any(DeleteCommentRequestDTO.class));

        mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().is4xxClientError());

        verify(commentService).deleteComment(eq(commentId), any(DeleteCommentRequestDTO.class));
    }

    @Test
    void deleteComment_WithUnauthorizedUser_ShouldThrowException() throws Exception {
        String commentId = "507f1f77bcf86cd799439011";
        DeleteCommentRequestDTO deleteRequest = new DeleteCommentRequestDTO();
        deleteRequest.setAuthorId(String.valueOf(UUID.randomUUID()));

        doThrow(new UnauthorizedException("Unauthorized to delete this comment"))
                .when(commentService).deleteComment(eq(commentId), any(DeleteCommentRequestDTO.class));

        mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().is4xxClientError());

        verify(commentService).deleteComment(eq(commentId), any(DeleteCommentRequestDTO.class));
    }
}