package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findPostById(Long id);

    void deletePostById(Long id);

    // PageResponse<PostResponseDTO> getAllPosts(PageRequest pageRequest, PostFilterRequest postFilterRequest);
}