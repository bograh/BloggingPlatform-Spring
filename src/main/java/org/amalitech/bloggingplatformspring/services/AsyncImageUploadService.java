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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

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

    private final PostImageRepository postImageRepository;
    private final PostRepository postRepository;
    private final ImageUploadProcessor imageUploadProcessor;

    @Transactional
    public ImageUploadDTO initiateUpload(Long postId, MultipartFile file) {
        validateFile(file);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        PostImage image = createImageRecord(post, file);
        PostImage saved = postImageRepository.save(image);
        saved.setCdnUrl(imageUploadProcessor.buildCdnUrl(saved.getId()));
        saved = postImageRepository.save(saved);

        log.info("Initiated image upload {} for post {}", saved.getId(), postId);

        try {
            byte[] fileBytes = file.getBytes();
            triggerUploadAfterCommit(saved.getId(), fileBytes);
        } catch (IOException e) {
            log.error("Failed to process image upload: {}", e.getMessage(), e);
        }

        return mapToDTO(saved);
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

        imageUploadProcessor.deleteObjectIfPresent(image.getStoragePath());

        postImageRepository.delete(image);
        log.info("Deleted image: {}", imageId);
    }

    /**
     * Deletes all images associated with a post, including their R2 objects.
     *
     * @param postId post ID whose images should be removed
     */
    @Async("applicationTaskExecutor")
    @Transactional
    public void deleteAllImagesForPost(Long postId) {
        List<PostImage> images = postImageRepository.findByPostId(postId);
        for (PostImage image : images) {
            imageUploadProcessor.deleteObjectIfPresent(image.getStoragePath());
        }
        postImageRepository.deleteAll(images);
        log.info("Deleted {} image(s) for post {}", images.size(), postId);
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

    private void triggerUploadAfterCommit(UUID imageId, byte[] fileBytes) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            imageUploadProcessor.processUploadAsync(imageId, fileBytes);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                imageUploadProcessor.processUploadAsync(imageId, fileBytes);
            }
        });
    }

    private ImageUploadDTO mapToDTO(PostImage image) {
        return ImageUploadDTO.builder()
                .imageId(image.getId())
                .postId(image.getPost() != null ? image.getPost().getId() : null)
                .originalFilename(image.getOriginalFilename())
                .status(image.getUploadStatus())
                .cdnUrl(image.getCdnUrl())
                .fileSize(image.getFileSize())
                .contentType(image.getContentType())
                .errorMessage(image.getErrorMessage())
                .uploadStartedAt(image.getUploadStartedAt())
                .uploadCompletedAt(image.getUploadCompletedAt())
                .build();
    }
}