package org.amalitech.bloggingplatformspring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponseGeneric;
import org.amalitech.bloggingplatformspring.dtos.responses.ImageUploadDTO;
import org.amalitech.bloggingplatformspring.exceptions.ErrorResponse;
import org.amalitech.bloggingplatformspring.services.AsyncImageUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Controller for async image upload operations.
 */
@RestController
@RequestMapping("/api/images")
@Tag(name = "14. Image Upload", description = "APIs for async image upload and management")
@RequiredArgsConstructor
public class ImageUploadController {

  private final AsyncImageUploadService imageUploadService;

  @PostMapping(value = "/upload/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload image for post", description = "Initiates async upload and processing of an image for a post")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "202", description = "Upload initiated", content = @Content(schema = @Schema(implementation = ImageUploadDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Post not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<ImageUploadDTO>> uploadImage(
      @Parameter(description = "Post ID to attach image to") @PathVariable Long postId,
      @Parameter(description = "Image file to upload") @RequestParam("file") MultipartFile file) {

    ImageUploadDTO upload = imageUploadService.initiateUpload(postId, file);
    return new ResponseEntity<>(
        ApiResponseGeneric.success("Image upload initiated", upload),
        HttpStatus.ACCEPTED);
  }

  @GetMapping("/status/{imageId}")
  @Operation(summary = "Get upload status", description = "Returns the current status of an image upload")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Status retrieved", content = @Content(schema = @Schema(implementation = ImageUploadDTO.class))),
      @ApiResponse(responseCode = "404", description = "Image not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<ImageUploadDTO>> getUploadStatus(
      @Parameter(description = "Image UUID") @PathVariable UUID imageId) {

    ImageUploadDTO upload = imageUploadService.getUploadStatus(imageId);
    return ResponseEntity.ok(ApiResponseGeneric.success("Status retrieved", upload));
  }

  @GetMapping("/post/{postId}")
  @Operation(summary = "Get post images", description = "Returns all images for a post")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Images retrieved"),
      @ApiResponse(responseCode = "404", description = "Post not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<List<ImageUploadDTO>>> getPostImages(
      @Parameter(description = "Post ID") @PathVariable Long postId) {

    List<ImageUploadDTO> images = imageUploadService.getPostImages(postId);
    return ResponseEntity.ok(ApiResponseGeneric.success("Images retrieved", images));
  }

  @GetMapping("/post/{postId}/completed")
  @Operation(summary = "Get completed post images", description = "Returns only completed images for a post")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Completed images retrieved")
  })
  public ResponseEntity<ApiResponseGeneric<List<ImageUploadDTO>>> getCompletedPostImages(
      @Parameter(description = "Post ID") @PathVariable Long postId) {

    List<ImageUploadDTO> images = imageUploadService.getCompletedPostImages(postId);
    return ResponseEntity.ok(ApiResponseGeneric.success("Completed images retrieved", images));
  }

  @PostMapping("/retry/{imageId}")
  @Operation(summary = "Retry failed upload", description = "Retries a failed image upload")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Retry initiated", content = @Content(schema = @Schema(implementation = ImageUploadDTO.class))),
      @ApiResponse(responseCode = "400", description = "Cannot retry - not in failed state", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Image not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApiResponseGeneric<ImageUploadDTO>> retryUpload(
      @Parameter(description = "Image UUID") @PathVariable UUID imageId) {

    ImageUploadDTO upload = imageUploadService.retryUpload(imageId);
    return ResponseEntity.ok(ApiResponseGeneric.success("Retry initiated", upload));
  }

  @DeleteMapping("/{imageId}")
  @Operation(summary = "Delete image", description = "Deletes an image and its associated files")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Image deleted"),
      @ApiResponse(responseCode = "404", description = "Image not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<Void> deleteImage(
      @Parameter(description = "Image UUID") @PathVariable UUID imageId) {

    imageUploadService.deleteImage(imageId);
    return ResponseEntity.noContent().build();
  }
}
