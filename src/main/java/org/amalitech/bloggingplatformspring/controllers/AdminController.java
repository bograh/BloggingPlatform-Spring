package org.amalitech.bloggingplatformspring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.requests.CommentFilterRequest;
import org.amalitech.bloggingplatformspring.dtos.requests.PostFilterRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.*;
import org.amalitech.bloggingplatformspring.services.AdminService;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.amalitech.bloggingplatformspring.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "6. Administration", description = "APIs for admin-only operations")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final PostService postService;
    private final UserService userService;
    private final CommentService commentService;

    @Operation(summary = "Get stats")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponseGeneric<StatsResponse>> getStats() {
        StatsResponse statsResponse = adminService.getStats();
        ApiResponseGeneric<StatsResponse> response =
                ApiResponseGeneric.success("Stats retrieved successfully", statsResponse);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all posts")
    @GetMapping("/posts")
    public ResponseEntity<ApiResponseGeneric<PageResponse<PostResponseDTO>>> getAllPosts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sort", defaultValue = "lastUpdated") String sortBy,
            @RequestParam(name = "order", defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String search) {

        PostFilterRequest postFilterRequest = new PostFilterRequest(author, search, tags);
        PageResponse<PostResponseDTO> posts = postService.getAllPosts(page, size, sortBy, sortDirection, postFilterRequest);
        ApiResponseGeneric<PageResponse<PostResponseDTO>> response = ApiResponseGeneric
                .success("Posts retrieved successfully", posts);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get a post by ID")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponseGeneric<PostResponseDTO>> getPostById(@PathVariable Long postId) {
        PostResponseDTO post = postService.getPostById(postId);
        ApiResponseGeneric<PostResponseDTO> response = ApiResponseGeneric.success("Post retrieved successfully", post);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a post")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> adminDeletePost(@PathVariable Long postId) {
        adminService.adminDeletePost(postId);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/users")
    @Operation(summary = "Get all users")
    public ResponseEntity<ApiResponseGeneric<PageResponse<UserResponseDTO>>> getAllUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "order", defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) String search
    ) {

        PageResponse<UserResponseDTO> users = userService.getAllUsers(page, size, sortBy, sortDirection, search);
        ApiResponseGeneric<PageResponse<UserResponseDTO>> response = ApiResponseGeneric.success("Users retrieved successfully", users);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/summary")
    @Operation(summary = "Get user summary")
    public ResponseEntity<ApiResponseGeneric<UserProfileSummary>> getUserSummary(@PathVariable String userId) {

        UserProfileSummary userProfileSummary = userService.getUserSummary(userId);
        ApiResponseGeneric<UserProfileSummary> response = ApiResponseGeneric.success("User summary retrieved successfully", userProfileSummary);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comments")
    @Operation(summary = "Get all comments")
    public ResponseEntity<ApiResponseGeneric<PageResponse<CommentResponse>>> getAllComments(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "16") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "order", defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) String author
    ) {

        CommentFilterRequest commentFilterRequest = new CommentFilterRequest(author, postId, search);
        PageResponse<CommentResponse> comments = commentService.getAllComments(page, size, sortBy, sortDirection, commentFilterRequest);
        ApiResponseGeneric<PageResponse<CommentResponse>> response = ApiResponseGeneric.success("Comments retrieved successfully", comments);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comments/{commentId}")
    @Operation(summary = "Get a comment by ID")
    public ResponseEntity<ApiResponseGeneric<CommentResponse>> getCommentById(@PathVariable String commentId) {
        CommentResponse comment = commentService.getCommentById(commentId);
        ApiResponseGeneric<CommentResponse> response = ApiResponseGeneric.success("Comment retrieved successfully", comment);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<Void> adminDeleteComment(@PathVariable String commentId) {
        adminService.adminDeleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

}