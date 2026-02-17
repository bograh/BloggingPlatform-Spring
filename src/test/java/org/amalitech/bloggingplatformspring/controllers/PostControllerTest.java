package org.amalitech.bloggingplatformspring.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.bloggingplatformspring.dtos.requests.CreatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.PostFilterRequest;
import org.amalitech.bloggingplatformspring.dtos.requests.UpdatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.exceptions.ForbiddenException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    private ObjectMapper objectMapper;
    private UUID userID;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController).build();
        objectMapper = new ObjectMapper();
        userID = UUID.randomUUID();
    }

    @Test
    void createPost_WithValidData_ShouldReturnCreatedStatus() throws Exception {
        CreatePostDTO createPostDTO = new CreatePostDTO();
        createPostDTO.setTitle("Test Post");
        createPostDTO.setBody("This is test content");

        PostResponseDTO postResponse = new PostResponseDTO();
        postResponse.setId(1L);
        postResponse.setTitle("Test Post");
        postResponse.setBody("This is test content");

        when(postService.createPost(any(CreatePostDTO.class), any())).thenReturn(postResponse);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPostDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Post created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Post"))
                .andExpect(jsonPath("$.data.body").value("This is test content"));

        verify(postService).createPost(any(CreatePostDTO.class), any());
    }

    @Test
    void createPost_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        CreatePostDTO createPostDTO = new CreatePostDTO();

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPostDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllPosts_WithDefaultParameters_ShouldReturnOkStatus() throws Exception {
        PostResponseDTO post1 = new PostResponseDTO();
        post1.setId(1L);
        post1.setTitle("Post 1");

        PostResponseDTO post2 = new PostResponseDTO();
        post2.setId(2L);
        post2.setTitle("Post 2");

        PageResponse<PostResponseDTO> pageResponse = new PageResponse<>(
                Arrays.asList(post1, post2),
                0, 10,
                Sort.by(Sort.Direction.DESC, "lastUpdated").toString(), 2, true);

        when(postService.getAllPosts(anyInt(), anyInt(), anyString(), anyString(),
                any(PostFilterRequest.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Posts retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Post 1"))
                .andExpect(jsonPath("$.data.content[1].id").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(2));

        verify(postService).getAllPosts(eq(0), eq(12), eq("lastUpdated"), eq("DESC"),
                any(PostFilterRequest.class));
    }

    @Test
    void getAllPosts_WithCustomParameters_ShouldReturnFilteredPosts() throws Exception {
        PostResponseDTO post = new PostResponseDTO();
        post.setId(1L);
        post.setTitle("Filtered Post");

        PageResponse<PostResponseDTO> pageResponse = new PageResponse<>(
                List.of(post),
                1, 5,
                Sort.by(Sort.Direction.DESC, "lastUpdated").toString(), 1, true);

        when(postService.getAllPosts(anyInt(), anyInt(), anyString(), anyString(),
                any(PostFilterRequest.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/posts")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "title")
                        .param("order", "ASC")
                        .param("author", "John Doe")
                        .param("tags", "java", "spring")
                        .param("search", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Posts retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5));

        verify(postService).getAllPosts(eq(1), eq(5), eq("title"), eq("ASC"), any(PostFilterRequest.class));
    }

    @Test
    void getPostById_WithValidId_ShouldReturnPost() throws Exception {
        Long postId = 1L;
        PostResponseDTO postResponse = new PostResponseDTO();
        postResponse.setId(postId);
        postResponse.setTitle("Test Post");
        postResponse.setBody("Test Content");

        when(postService.getPostById(postId)).thenReturn(postResponse);

        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Post"))
                .andExpect(jsonPath("$.data.body").value("Test Content"));

        verify(postService).getPostById(postId);
    }

    @Test
    void getPostById_WithNonExistentId_ShouldThrowException() throws Exception {
        Long postId = 999L;
        when(postService.getPostById(postId))
                .thenThrow(new ResourceNotFoundException("Post not found"));

        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().is4xxClientError());

        verify(postService).getPostById(postId);
    }

    @Test
    void updatePost_WithValidData_ShouldReturnUpdatedPost() throws Exception {
        Long postId = 1L;
        UpdatePostDTO updatePostDTO = new UpdatePostDTO();
        updatePostDTO.setTitle("Updated Title");
        updatePostDTO.setBody("Updated Content");

        PostResponseDTO postResponse = new PostResponseDTO();
        postResponse.setId(postId);
        postResponse.setTitle("Updated Title");
        postResponse.setBody("Updated Content");

        when(postService.updatePost(eq(postId), any(UpdatePostDTO.class), any())).thenReturn(postResponse);

        mockMvc.perform(put("/api/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePostDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.body").value("Updated Content"));

        verify(postService).updatePost(eq(postId), any(UpdatePostDTO.class), any());
    }

    @Test
    void updatePost_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        Long postId = 1L;
        UpdatePostDTO updatePostDTO = new UpdatePostDTO();

        mockMvc.perform(put("/api/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePostDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePost_WithNonExistentId_ShouldThrowException() throws Exception {
        Long postId = 999L;
        UpdatePostDTO updatePostDTO = new UpdatePostDTO();
        updatePostDTO.setTitle("Updated Title");
        updatePostDTO.setBody("Updated Content");

        when(postService.updatePost(eq(postId), any(UpdatePostDTO.class), any()))
                .thenThrow(new ResourceNotFoundException("Post not found"));

        mockMvc.perform(put("/api/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePostDTO)))
                .andExpect(status().is4xxClientError());

        verify(postService).updatePost(eq(postId), any(UpdatePostDTO.class), any());
    }

    @Test
    void deletePost_shouldReturnNoContent_whenPostDeletedSuccessfully() {
        Long postId = 1L;

        doNothing().when(postService).deletePost(eq(postId), any());

        ResponseEntity<Void> response = postController.deletePost(postId, null);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals(204, response.getStatusCode().value());

        verify(postService, times(1)).deletePost(eq(postId), any());
    }

    @Test
    void deletePost_shouldReturnSuccessMessage_whenPostDeleted() {
        Long postId = 5L;

        doNothing().when(postService).deletePost(eq(postId), any());

        ResponseEntity<Void> response = postController.deletePost(postId, null);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(postService, times(1)).deletePost(eq(postId), any());
    }

    @Test
    void deletePost_shouldCallServiceWithCorrectParameters() {
        Long postId = 10L;

        doNothing().when(postService).deletePost(eq(postId), any());

        postController.deletePost(postId, null);

        verify(postService, times(1)).deletePost(eq(postId), any());
        verifyNoMoreInteractions(postService);
    }

    @Test
    void deletePost_shouldHandleDifferentPostIds() {
        Long postId = 999L;

        doNothing().when(postService).deletePost(eq(postId), any());

        ResponseEntity<Void> response = postController.deletePost(postId, null);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(postService, times(1)).deletePost(eq(999L), any());
    }

    @Test
    void deletePost_shouldReturnApiResponseGeneric_withVoidData() {
        Long postId = 1L;

        doNothing().when(postService).deletePost(eq(postId), any());

        ResponseEntity<Void> response = postController.deletePost(postId, null);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(postService, times(1)).deletePost(eq(postId), any());
    }

    @Test
    void deletePost_shouldPropagateServiceException_whenServiceThrowsException() {
        Long postId = 1L;

        doThrow(new ResourceNotFoundException("Post not found"))
                .when(postService).deletePost(eq(postId), any());

        assertThrows(ResourceNotFoundException.class,
                () -> postController.deletePost(postId, null));

        verify(postService, times(1)).deletePost(eq(postId), any());
    }

    @Test
    void deletePost_shouldPropagateServiceException_whenUnauthorized() {
        Long postId = 1L;

        doThrow(new ForbiddenException("You cannot delete this post"))
                .when(postService).deletePost(eq(postId), any());

        assertThrows(ForbiddenException.class,
                () -> postController.deletePost(postId, null));

        verify(postService, times(1)).deletePost(eq(postId), any());
    }
}