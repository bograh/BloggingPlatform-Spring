package org.amalitech.bloggingplatformspring.controllers;

import jakarta.validation.Valid;
import org.amalitech.bloggingplatformspring.dtos.requests.CreatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        ApiResponse<PostResponseDTO> response = ApiResponse.success("Post created successfully", postResponseDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponseDTO>>> getAllPosts() {
        List<PostResponseDTO> posts = postService.getAllPosts();
        ApiResponse<List<PostResponseDTO>> response = ApiResponse.success("Posts retrieved successfully", posts);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponseDTO>> getPostById(@PathVariable int postId) {
        PostResponseDTO post = postService.getPostById(postId);
        ApiResponse<PostResponseDTO> response = ApiResponse.success("Post retrieved successfully", post);
        return ResponseEntity.ok(response);
    }
}