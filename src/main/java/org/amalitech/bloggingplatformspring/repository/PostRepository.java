package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.dtos.requests.CreatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository {
    Post savePost(CreatePostDTO createPostDTO) throws SQLException;

    List<PostResponseDTO> getAllPosts() throws SQLException;

    Optional<PostResponseDTO> getPostResponseById(int id) throws SQLException;

    Optional<Post> findPostById(int id) throws SQLException;

    void updatePost(Post post, List<String> tagNames) throws SQLException;

    void deletePost(int id, UUID signedInUserId) throws SQLException;

    List<String> getTagsByPostId(int postId) throws SQLException;
}