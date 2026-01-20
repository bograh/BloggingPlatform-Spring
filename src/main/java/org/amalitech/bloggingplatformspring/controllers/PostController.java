package org.amalitech.bloggingplatformspring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.amalitech.bloggingplatformspring.dtos.requests.*;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponseGeneric;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.exceptions.ErrorResponse;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@Tag(name = "Post Management", description = "APIs for creating, reading, updating, and deleting blog posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @Operation(summary = "Create a new blog post", description = "Creates a new blog post with title, content, author, and optional tags")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Post successfully created", content = @Content(schema = @Schema(implementation = PostResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Author not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseGeneric<PostResponseDTO>> createPost(
            @Valid @RequestBody CreatePostDTO createPostDTO) {
        PostResponseDTO postResponseDTO = postService.createPost(createPostDTO);
        ApiResponseGeneric<PostResponseDTO> response = ApiResponseGeneric.success("Post created successfully",
                postResponseDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all blog posts with pagination", description = "Retrieves a paginated list of blog posts with optional filtering by author, tags, and search term. Supports sorting by various fields.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Posts successfully retrieved", content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseGeneric<PageResponse<PostResponseDTO>>> getAllPosts(
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (max 50)", example = "10") @RequestParam(name = "size", defaultValue = "10") int size,
            @Parameter(description = "Sort field (id, createdAt, lastUpdated, title)", example = "lastUpdated") @RequestParam(name = "sort", defaultValue = "lastUpdated") String sortBy,
            @Parameter(description = "Sort order (ASC or DESC)", example = "DESC") @RequestParam(name = "order", defaultValue = "DESC") String sortDirection,
            @Parameter(description = "Filter by author name") @RequestParam(required = false) String author,
            @Parameter(description = "Filter by tag names") @RequestParam(required = false) List<String> tags,
            @Parameter(description = "Search in title and content") @RequestParam(required = false) String search) {
        PageRequest pageRequest = new PageRequest(page, Math.min(50, size), sortBy, sortDirection);
        PostFilterRequest filterRequest = new PostFilterRequest(author, search, tags);

        PageResponse<PostResponseDTO> posts = postService.getPaginatedPosts(pageRequest, filterRequest);
        ApiResponseGeneric<PageResponse<PostResponseDTO>> response = ApiResponseGeneric
                .success("Posts retrieved successfully", posts);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Get a post by ID", description = "Retrieves a single blog post by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post successfully retrieved", content = @Content(schema = @Schema(implementation = PostResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Post not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseGeneric<PostResponseDTO>> getPostById(
            @Parameter(description = "Post ID", example = "1") @PathVariable int postId) {
        PostResponseDTO post = postService.getPostById(postId);
        ApiResponseGeneric<PostResponseDTO> response = ApiResponseGeneric.success("Post retrieved successfully", post);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}")
    @Operation(summary = "Update a blog post", description = "Updates an existing blog post. Only the author can update their post.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post successfully updated", content = @Content(schema = @Schema(implementation = PostResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not authorized to update this post", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Post not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseGeneric<PostResponseDTO>> updatePost(
            @Parameter(description = "Post ID", example = "1") @PathVariable int postId,
            @Valid @RequestBody UpdatePostDTO updatePostDTO) {
        PostResponseDTO post = postService.updatePost(postId, updatePostDTO);
        ApiResponseGeneric<PostResponseDTO> response = ApiResponseGeneric.success("Post updated successfully", post);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete a blog post", description = "Deletes an existing blog post. Only the author can delete their post.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Post successfully deleted"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this post", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Post not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseGeneric<Void>> deletePost(
            @Parameter(description = "Post ID", example = "1") @PathVariable int postId,
            @RequestBody DeletePostRequestDTO deletePostRequestDTO) {
        postService.deletePost(postId, deletePostRequestDTO);
        ApiResponseGeneric<Void> response = ApiResponseGeneric.success("Post deleted successfully.");
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }
}