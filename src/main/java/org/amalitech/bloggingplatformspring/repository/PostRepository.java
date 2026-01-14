package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.dtos.requests.CreatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface PostRepository {
    Post savePost(CreatePostDTO createPostDTO) throws SQLException;

    List<PostResponseDTO> getAllPosts() throws SQLException;

    Optional<PostResponseDTO> getPostById(int id) throws SQLException;

    Post updatePost() throws SQLException;

    void deletePost(int id, String signedInUserId) throws SQLException;

}