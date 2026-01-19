package org.amalitech.bloggingplatformspring.controllers;

import jakarta.validation.Valid;
import org.amalitech.bloggingplatformspring.dtos.requests.CreateCommentDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.DeleteCommentRequestDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponse;
import org.amalitech.bloggingplatformspring.entity.CommentDocument;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommentDocument>> addCommentToPost(@Valid @RequestBody CreateCommentDTO newComment) {
        CommentDocument commentDocument = commentService.addCommentToPost(newComment);
        ApiResponse<CommentDocument> response = ApiResponse.success(
                "Comment added to post successfully",
                commentDocument
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<List<CommentDocument>>> getAllCommentsByPostId(@PathVariable int postId) {
        List<CommentDocument> comments = commentService.getAllCommentsByPostId(postId);
        ApiResponse<List<CommentDocument>> response = ApiResponse.success(
                "Comments for post retrieved successfully",
                comments
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentDocument>> getCommentById(@PathVariable String commentId) {
        CommentDocument commentDocument = commentService.getCommentById(commentId);
        ApiResponse<CommentDocument> response = ApiResponse.success(
                "Comment retrieved successfully",
                commentDocument
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable String commentId, @RequestBody DeleteCommentRequestDTO deleteCommentRequestDTO) {
        commentService.deleteComment(commentId, deleteCommentRequestDTO);
        ApiResponse<Void> response = ApiResponse.success(
                "Comment deleted successfully");
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }

}