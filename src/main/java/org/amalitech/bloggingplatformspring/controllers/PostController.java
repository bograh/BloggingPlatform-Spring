package org.amalitech.bloggingplatformspring.controllers;

import jakarta.validation.Valid;
import org.amalitech.bloggingplatformspring.dtos.requests.CreatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.DeletePostRequestDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.UpdatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponseDTO>> createPost(@Valid @RequestBody CreatePostDTO createPostDTO) {
        PostResponseDTO postResponseDTO = postService.createPost(createPostDTO);
        ApiResponse<PostResponseDTO> response =
                ApiResponse.success("Post created successfully", postResponseDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostResponseDTO>>> getAllPosts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "lastUpdated") String sortBy,
            @RequestParam(name = "order", defaultValue = "DESC") String sortDirection
    ) {
        PageResponse<PostResponseDTO> posts = postService.getPaginatedPosts(page, size, sortBy, sortDirection);
        ApiResponse<PageResponse<PostResponseDTO>> response =
                ApiResponse.success("Posts retrieved successfully", posts);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponseDTO>> getPostById(@PathVariable int postId) {
        PostResponseDTO post = postService.getPostById(postId);
        ApiResponse<PostResponseDTO> response =
                ApiResponse.success("Post retrieved successfully", post);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponseDTO>> updatePost(
            @PathVariable int postId,
            @Valid @RequestBody UpdatePostDTO updatePostDTO) {
        PostResponseDTO post = postService.updatePost(postId, updatePostDTO);
        ApiResponse<PostResponseDTO> response =
                ApiResponse.success("Post updated successfully", post);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable int postId, @RequestBody DeletePostRequestDTO deletePostRequestDTO) {
        postService.deletePost(postId, deletePostRequestDTO);
        ApiResponse<Void> response = ApiResponse.success("Post deleted successfully.");
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }
}