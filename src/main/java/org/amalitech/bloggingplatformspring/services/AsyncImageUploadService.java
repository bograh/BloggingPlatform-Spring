package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.responses.ImageUploadDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.PostImage;
import org.amalitech.bloggingplatformspring.enums.ImageUploadStatus;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.repository.PostImageRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for asynchronous image upload processing.
 * Handles file validation, storage, and thumbnail generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncImageUploadService {

  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
  private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
      "image/jpeg", "image/png", "image/gif", "image/webp");
  private static final String STORAGE_BASE_PATH = "/uploads/images/";
  private static final String THUMBNAILS_PATH = "/uploads/thumbnails/";
  private static final String CDN_BASE_URL = "/cdn/images/";

  private final PostImageRepository postImageRepository;
  private final PostRepository postRepository;

  /**
   * Initiates async image upload for a post.
   *
   * @param postId post ID to attach image to
   * @param file   multipart file to upload
   * @return image upload DTO with tracking info
   */
  @Transactional
  public ImageUploadDTO initiateUpload(Long postId, MultipartFile file) {
    validateFile(file);

    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

    PostImage image = createImageRecord(post, file);
    PostImage saved = postImageRepository.save(image);

    log.info("Initiated image upload {} for post {}", saved.getId(), postId);

    processUploadAsync(saved.getId(), file.getBytes());

    return mapToDTO(saved);
  }

  /**
   * Initiates async image upload without an associated post.
   *
   * @param file multipart file to upload
   * @return image upload DTO with tracking info
   */
  @Transactional
  public ImageUploadDTO initiateUploadWithoutPost(MultipartFile file) {
    validateFile(file);

    throw new BadRequestException("Image upload without post not yet supported - use initiateUpload with postId");
  }

  /**
   * Gets upload status for an image.
   *
   * @param imageId image UUID
   * @return image upload DTO
   */
  public ImageUploadDTO getUploadStatus(UUID imageId) {
    PostImage image = postImageRepository.findById(imageId)
        .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));
    return mapToDTO(image);
  }

  /**
   * Gets all images for a post.
   *
   * @param postId post ID
   * @return list of image DTOs
   */
  public List<ImageUploadDTO> getPostImages(Long postId) {
    List<PostImage> images = postImageRepository.findByPostId(postId);
    return images.stream().map(this::mapToDTO).toList();
  }

  /**
   * Gets completed images for a post.
   *
   * @param postId post ID
   * @return list of completed image DTOs
   */
  public List<ImageUploadDTO> getCompletedPostImages(Long postId) {
    List<PostImage> images = postImageRepository
        .findByPostIdAndUploadStatus(postId, ImageUploadStatus.COMPLETED);
    return images.stream().map(this::mapToDTO).toList();
  }

  /**
   * Processes image upload asynchronously.
   *
   * @param imageId   image UUID
   * @param fileBytes raw file bytes
   */
  @Async("applicationTaskExecutor")
  public CompletableFuture<Void> processUploadAsync(UUID imageId, byte[] fileBytes) {
    log.info("Starting async image processing for: {}", imageId);

    try {
      PostImage image = postImageRepository.findById(imageId)
          .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));

      markAsUploading(image);

      String storedPath = storeImage(imageId, fileBytes);

      String thumbnailPath = generateThumbnail(imageId, fileBytes);

      String cdnUrl = publishToCdn(imageId);

      completeUpload(image, storedPath, thumbnailPath, cdnUrl);

      log.info("Completed image processing for: {}", imageId);

    } catch (Exception e) {
      log.error("Failed to process image {}: {}", imageId, e.getMessage(), e);
      handleUploadError(imageId, e.getMessage());
    }

    return CompletableFuture.completedFuture(null);
  }

  /**
   * Retries a failed upload.
   *
   * @param imageId image UUID to retry
   * @return updated image DTO
   */
  @Transactional
  public ImageUploadDTO retryUpload(UUID imageId) {
    PostImage image = postImageRepository.findById(imageId)
        .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));

    if (!image.isFailed()) {
      throw new BadRequestException("Cannot retry upload that is not in failed state");
    }

    image.setUploadStatus(ImageUploadStatus.PENDING);
    image.setErrorMessage(null);
    PostImage saved = postImageRepository.save(image);

    log.info("Retrying image upload: {}", imageId);

    return mapToDTO(saved);
  }

  /**
   * Deletes an image.
   *
   * @param imageId image UUID
   */
  @Transactional
  public void deleteImage(UUID imageId) {
    PostImage image = postImageRepository.findById(imageId)
        .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));

    if (image.getStoragePath() != null) {
      deleteStoredFile(image.getStoragePath());
    }
    if (image.getThumbnailPath() != null) {
      deleteStoredFile(image.getThumbnailPath());
    }

    postImageRepository.delete(image);
    log.info("Deleted image: {}", imageId);
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("File cannot be empty");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new BadRequestException("File size exceeds maximum allowed: " + MAX_FILE_SIZE + " bytes");
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new BadRequestException("Invalid file type. Allowed types: " + ALLOWED_CONTENT_TYPES);
    }
  }

  private PostImage createImageRecord(Post post, MultipartFile file) {
    PostImage image = new PostImage();
    image.setPost(post);
    image.setOriginalFilename(file.getOriginalFilename());
    image.setContentType(file.getContentType());
    image.setFileSize(file.getSize());
    image.setUploadStatus(ImageUploadStatus.PENDING);
    return image;
  }

  private void markAsUploading(PostImage image) {
    image.setUploadStatus(ImageUploadStatus.UPLOADING);
    image.setUploadStartedAt(LocalDateTime.now());
    postImageRepository.save(image);
  }

  private String storeImage(UUID imageId, byte[] fileBytes) {
    String storedPath = STORAGE_BASE_PATH + imageId + ".jpg";
    log.debug("Simulating image storage at: {}", storedPath);
    simulateProcessingDelay();
    return storedPath;
  }

  private String generateThumbnail(UUID imageId, byte[] fileBytes) {
    String thumbnailPath = THUMBNAILS_PATH + imageId + "_thumb.jpg";
    log.debug("Simulating thumbnail generation at: {}", thumbnailPath);
    simulateProcessingDelay();
    return thumbnailPath;
  }

  private String publishToCdn(UUID imageId) {
    String cdnUrl = CDN_BASE_URL + imageId;
    log.debug("Simulating CDN publish at: {}", cdnUrl);
    return cdnUrl;
  }

  private void completeUpload(PostImage image, String storedPath, String thumbnailPath, String cdnUrl) {
    image.setUploadStatus(ImageUploadStatus.COMPLETED);
    image.setStoragePath(storedPath);
    image.setThumbnailPath(thumbnailPath);
    image.setCdnUrl(cdnUrl);
    image.setUploadCompletedAt(LocalDateTime.now());
    postImageRepository.save(image);
  }

  private void handleUploadError(UUID imageId, String errorMessage) {
    postImageRepository.findById(imageId).ifPresent(image -> {
      image.setUploadStatus(ImageUploadStatus.FAILED);
      image.setErrorMessage(errorMessage);
      postImageRepository.save(image);
    });
  }

  private void deleteStoredFile(String path) {
    log.debug("Simulating file deletion at: {}", path);
  }

  private void simulateProcessingDelay() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private ImageUploadDTO mapToDTO(PostImage image) {
    return ImageUploadDTO.builder()
        .imageId(image.getId())
        .postId(image.getPost() != null ? image.getPost().getId() : null)
        .originalFilename(image.getOriginalFilename())
        .status(image.getUploadStatus())
        .cdnUrl(image.getCdnUrl())
        .thumbnailUrl(image.getThumbnailPath())
        .fileSize(image.getFileSize())
        .contentType(image.getContentType())
        .errorMessage(image.getErrorMessage())
        .uploadStartedAt(image.getUploadStartedAt())
        .uploadCompletedAt(image.getUploadCompletedAt())
        .build();
  }
}
