package org.amalitech.bloggingplatformspring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.amalitech.bloggingplatformspring.enums.ImageUploadStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an image associated with a post.
 * Supports async upload processing.
 */
@Entity
@Table(name = "post_images", indexes = {
        @Index(name = "idx_image_post", columnList = "post_id"),
        @Index(name = "idx_image_status", columnList = "upload_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "cdn_url")
    private String cdnUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false)
    private ImageUploadStatus uploadStatus = ImageUploadStatus.PENDING;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "upload_started_at")
    private LocalDateTime uploadStartedAt;

    @Column(name = "upload_completed_at")
    private LocalDateTime uploadCompletedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isFailed() {
        return uploadStatus == ImageUploadStatus.FAILED;
    }
}