package org.amalitech.bloggingplatformspring.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.bloggingplatformspring.dtos.requests.*;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;


    @Test
    void createPost_Success_Returns201() throws Exception {
        CreatePostDTO request = new CreatePostDTO(
                "Post title",
                "Post content",
                "author-id",
                List.of("java", "spring")
        );

        PostResponseDTO responseDTO = new PostResponseDTO(
                1,
                "Post title",
                "Post content",
                "testuser",
                List.of("java", "spring"),
                LocalDateTime.now().toString(),
                0
        );

        when(postService.createPost(any(CreatePostDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Post created successfully"))
                .andExpect(jsonPath("$.data.title").value("Post title"));
    }

    @Test
    void createPost_InvalidInput_Returns400() throws Exception {
        CreatePostDTO invalidRequest = new CreatePostDTO(
                "",
                "",
                "",
                null
        );

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void getAllPosts_Success_Returns200() throws Exception {
        PageResponse<PostResponseDTO> pageResponse =
                new PageResponse<>(
                        List.of(new PostResponseDTO(
                                1,
                                "Post title",
                                "Post content",
                                "author-id",
                                List.of("java"),
                                LocalDateTime.now().toString(),
                                0
                        )),
                        0,
                        1,
                        "lastUpdated",
                        10
                );

        when(postService.getPaginatedPosts(any(PageRequest.class), any(PostFilterRequest.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "lastUpdated")
                        .param("order", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Posts retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].title").value("Post title"));
    }

    // =========================
    // Get Post By ID
    // =========================

    @Test
    void getPostById_Success_Returns200() throws Exception {
        PostResponseDTO responseDTO = new PostResponseDTO(
                1,
                "Post title",
                "Post content",
                "testuser",
                List.of("java", "spring"),
                LocalDateTime.now().toString(),
                0
        );

        when(postService.getPostById(1)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/posts/{postId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // =========================
    // Update Post
    // =========================

    @Test
    void updatePost_Success_Returns200() throws Exception {
        UpdatePostDTO request = new UpdatePostDTO(
                "Updated title",
                "Updated content",
                UUID.randomUUID().toString(),
                List.of("spring")
        );

        PostResponseDTO responseDTO = new PostResponseDTO(
                1,
                "Post title",
                "Post content",
                "testuser",
                List.of("java", "spring"),
                LocalDateTime.now().toString(),
                0
        );

        when(postService.updatePost(eq(1), any(UpdatePostDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/posts/{postId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Post updated successfully"))
                .andExpect(jsonPath("$.data.title").value("Updated title"));
    }


    @Test
    void deletePost_Success_Returns204() throws Exception {
        DeletePostRequestDTO request = new DeletePostRequestDTO(
                UUID.randomUUID().toString()
        );

        doNothing().when(postService)
                .deletePost(eq(1), any(DeletePostRequestDTO.class));

        mockMvc.perform(delete("/api/v1/posts/{postId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}