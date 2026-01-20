package org.amalitech.bloggingplatformspring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.amalitech.bloggingplatformspring.dtos.requests.CreateCommentDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.DeleteCommentRequestDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponseGeneric;
import org.amalitech.bloggingplatformspring.entity.CommentDocument;
import org.amalitech.bloggingplatformspring.exceptions.ErrorResponse;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@Tag(name = "Comment Management", description = "APIs for managing comments on blog posts (MongoDB-backed)")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @Operation(summary = "Add a comment to a post", description = "Creates a new comment on a blog post. Comments are stored in MongoDB.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Comment successfully added", content = @Content(schema = @Schema(implementation = CommentDocument.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Post or user not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseGeneric<CommentDocument>> addCommentToPost(
            @Valid @RequestBody CreateCommentDTO newComment) {
        CommentDocument commentDocument = commentService.addCommentToPost(newComment);
        ApiResponseGeneric<CommentDocument> response = ApiResponseGeneric.success(
                "Comment added to post successfully",
                commentDocument);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/post/{postId}")
    @Operation(summary = "Get all comments for a post", description = "Retrieves all comments associated with a specific blog post from MongoDB")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comments successfully retrieved", content = @Content(schema = @Schema(implementation = CommentDocument.class))),
            @ApiResponse(responseCode = "404", description = "Post not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseGeneric<List<CommentDocument>>> getAllCommentsByPostId(
            @Parameter(description = "Post ID", example = "1") @PathVariable int postId) {
        List<CommentDocument> comments = commentService.getAllCommentsByPostId(postId);
        ApiResponseGeneric<List<CommentDocument>> response = ApiResponseGeneric.success(
                "Comments for post retrieved successfully",
                comments);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{commentId}")
    @Operation(summary = "Get a comment by ID", description = "Retrieves a single comment by its MongoDB ObjectId")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment successfully retrieved", content = @Content(schema = @Schema(implementation = CommentDocument.class))),
            @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseGeneric<CommentDocument>> getCommentById(
            @Parameter(description = "MongoDB Comment ID", example = "507f1f77bcf86cd799439011") @PathVariable String commentId) {
        CommentDocument commentDocument = commentService.getCommentById(commentId);
        ApiResponseGeneric<CommentDocument> response = ApiResponseGeneric.success(
                "Comment retrieved successfully",
                commentDocument);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment", description = "Deletes a comment from MongoDB. Only the comment author can delete their comment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Comment successfully deleted"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this comment", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseGeneric<Void>> deleteComment(
            @Parameter(description = "MongoDB Comment ID", example = "507f1f77bcf86cd799439011") @PathVariable String commentId,
            @RequestBody DeleteCommentRequestDTO deleteCommentRequestDTO) {
        commentService.deleteComment(commentId, deleteCommentRequestDTO);
        ApiResponseGeneric<Void> response = ApiResponseGeneric.success(
                "Comment deleted successfully");
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }

}