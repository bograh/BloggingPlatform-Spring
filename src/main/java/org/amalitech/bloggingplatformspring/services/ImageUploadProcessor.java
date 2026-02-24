package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.entity.PostImage;
import org.amalitech.bloggingplatformspring.enums.ImageUploadStatus;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.repository.PostImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Slf4j
@Service
public class ImageUploadProcessor {

    private static final String R2_IMAGES_PREFIX = "images/";
    private final PostImageRepository postImageRepository;
    private final S3Client r2Client;
    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;
    @Value("${cloudflare.r2.public-url}")
    private String r2PublicUrl;

    public String buildCdnUrl(UUID imageId) {
        return r2PublicUrl + "/" + R2_IMAGES_PREFIX + imageId;
    }

    public void deleteObjectIfPresent(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        r2Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build());
        log.debug("Deleted R2 object: {}", objectKey);
    }

    @Async("applicationTaskExecutor")
    public CompletableFuture<Void> processUploadAsync(UUID imageId, byte[] fileBytes) {
        log.info("Starting async image processing for: {}", imageId);
        try {
            PostImage image = postImageRepository.findById(imageId)
                    .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));

            markAsUploading(image);

            String objectKey = storeImage(imageId, fileBytes, image.getContentType());
            String cdnUrl = buildCdnUrl(imageId);

            completeUpload(image, objectKey, cdnUrl);

            log.info("Completed image processing for: {}", imageId);
        } catch (Exception e) {
            log.error("Failed to process image {}: {}", imageId, e.getMessage(), e);
            handleUploadError(imageId, e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    private void markAsUploading(PostImage image) {
        image.setUploadStatus(ImageUploadStatus.UPLOADING);
        image.setUploadStartedAt(LocalDateTime.now());
        postImageRepository.save(image);
    }

    private String storeImage(UUID imageId, byte[] fileBytes, String contentType) {
        String objectKey = R2_IMAGES_PREFIX + imageId;
        r2Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType(contentType)
                        .contentLength((long) fileBytes.length)
                        .build(),
                RequestBody.fromBytes(fileBytes));
        log.debug("Uploaded image to R2: {}", objectKey);
        return objectKey;
    }

    private void completeUpload(PostImage image, String storedPath, String cdnUrl) {
        image.setUploadStatus(ImageUploadStatus.COMPLETED);
        image.setStoragePath(storedPath);
        image.setThumbnailPath(null);
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
}