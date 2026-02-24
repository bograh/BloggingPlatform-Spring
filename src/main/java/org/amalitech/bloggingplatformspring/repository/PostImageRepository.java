package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.PostImage;
import org.amalitech.bloggingplatformspring.enums.ImageUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, UUID> {

  List<PostImage> findByPostIdAndUploadStatus(Long postId, ImageUploadStatus status);

  List<PostImage> findByPostId(Long postId);

  List<PostImage> findByUploadStatus(ImageUploadStatus status);

  long countByPostIdAndUploadStatus(Long postId, ImageUploadStatus status);
}
