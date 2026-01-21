package org.amalitech.bloggingplatformspring.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.bloggingplatformspring.dtos.requests.CreateCommentDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.DeleteCommentRequestDTO;
import org.amalitech.bloggingplatformspring.entity.CommentDocument;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @Test
    void addCommentToPost_Success_Returns201() throws Exception {
        var request = new CreateCommentDTO(
                1,
                "This is a comment",
                String.valueOf(UUID.randomUUID())
        );

        CommentDocument commentDocument = new CommentDocument(
                "507f1f77bcf86cd799439011",
                1,
                "testuser",
                "This is a comment",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.DateTimeFormatPattern))
        );

        when(commentService.addCommentToPost(any(CreateCommentDTO.class)))
                .thenReturn(commentDocument);

        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Comment added to post successfully"))
                .andExpect(jsonPath("$.data.content").value("This is a comment"));
    }

    @Test
    void addCommentToPost_InvalidInput_Returns400() throws Exception {
        CreateCommentDTO invalidRequest = new CreateCommentDTO(
                0,
                "",
                ""
        );

        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllCommentsByPostId_Success_Returns200() throws Exception {
        List<CommentDocument> comments = List.of(
                new CommentDocument(
                        "507f1f77bcf86cd799439011",
                        1,
                        "testuser",
                        "Nice post!",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.DateTimeFormatPattern))
                )
        );

        when(commentService.getAllCommentsByPostId(1))
                .thenReturn(comments);

        mockMvc.perform(get("/api/v1/comments/post/{postId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Comments for post retrieved successfully"))
                .andExpect(jsonPath("$.data[0].content").value("Nice post!"));
    }

    @Test
    void getCommentById_Success_Returns201() throws Exception {
        CommentDocument commentDocument = new CommentDocument(
                "507f1f77bcf86cd799439011",
                1,
                "testuser",
                "Great article",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.DateTimeFormatPattern))

        );

        when(commentService.getCommentById("507f1f77bcf86cd799439011"))
                .thenReturn(commentDocument);

        mockMvc.perform(get("/api/v1/comments/{commentId}", "507f1f77bcf86cd799439011"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content").value("Great article"));
    }

    @Test
    void deleteComment_Success_Returns204() throws Exception {
        DeleteCommentRequestDTO request = new DeleteCommentRequestDTO(
                String.valueOf(UUID.randomUUID())
        );

        doNothing().when(commentService)
                .deleteComment(eq("507f1f77bcf86cd799439011"), any(DeleteCommentRequestDTO.class));

        mockMvc.perform(delete("/api/v1/comments/{commentId}", "507f1f77bcf86cd799439011")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}