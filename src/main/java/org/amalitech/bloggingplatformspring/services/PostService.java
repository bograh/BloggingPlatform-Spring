package org.amalitech.bloggingplatformspring.services;

import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.requests.CreatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.DeletePostRequestDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.UpdatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.*;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.PostUtils;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostUtils postUtils;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postUtils = new PostUtils();
    }

    public PostResponseDTO createPost(CreatePostDTO createPostDTO) {
        try {
            Post post = postRepository.savePost(createPostDTO);
            List<String> tagNames = createPostDTO.getTags();
            String authorName = userRepository.getUsernameById(post.getAuthorId());

            return postUtils.createResponseFromPostAndTags(post, authorName, tagNames);

        } catch (SQLException e) {
            throw new SQLQueryException("Failed to create post: " + e.getMessage());
        }
    }

    public List<PostResponseDTO> getAllPosts() {
        try {
            return postRepository.getAllPosts();

        } catch (SQLException e) {
            throw new SQLQueryException("Error occurred while fetching posts: " + e.getMessage());
        }
    }

    public PostResponseDTO getPostById(int postId) {
        if (postId <= 0) {
            throw new BadRequestException("Post ID must be a positive number");
        }

        try {

            return postRepository.getPostResponseById(postId).orElseThrow(
                    () -> new ResourceNotFoundException("Post not Found with ID: " + postId)
            );

        } catch (SQLException e) {
            throw new SQLQueryException("Error occurred while fetching posts: " + e.getMessage());
        }
    }

    public PostResponseDTO updatePost(int postId, UpdatePostDTO updatePostDTO) {
        try {

            PostResponseDTO existingPost = postRepository.getPostResponseById(postId).orElseThrow(
                    () -> new ResourceNotFoundException("Post with ID: " + postId + " not found.")
            );

            User user = userRepository.findUserByUsername(existingPost.getAuthor()).orElseThrow(
                    () -> new ResourceNotFoundException("User not found with username: " + existingPost.getAuthor())
            );

            if (!user.getUsername().equalsIgnoreCase(existingPost.getAuthor())) {
                throw new ForbiddenException("You are not permitted to edit this post.");
            }

            String title = updatePostDTO.getTitle() == null
                    ? existingPost.getTitle() : updatePostDTO.getTitle();

            String body = updatePostDTO.getBody() == null
                    ? existingPost.getBody() : updatePostDTO.getBody();

            UUID authorId = user.getId();

            List<String> newTagList = new ArrayList<>(existingPost.getTags());
            List<String> newTags = updatePostDTO.getTags() == null ? List.of() : updatePostDTO.getTags();
            newTagList.addAll(newTags);

            Post post = new Post(existingPost.getId(), title, body, authorId, null, null);

            Post updatedPost = postRepository.updatePost(post, newTagList);

            return postUtils.createResponseFromPostAndTags(updatedPost, user.getUsername(), newTagList);

        } catch (SQLException e) {
            throw new SQLQueryException("Error occurred while updating post: " + e.getMessage());
        }
    }

    public void deletePost(int postId, DeletePostRequestDTO deletePostRequestDTO) {
        try {
            UUID userID = UUID.fromString(deletePostRequestDTO.getAuthorId());

            Post post = postRepository.findPostById(postId).orElseThrow(
                    () -> new ResourceNotFoundException("Post with ID: " + postId + " not found.")
            );

            User user = userRepository.findUserById(userID).orElseThrow(
                    () -> new ResourceNotFoundException("User not found with username: " + userID.toString()));

            if (!user.getId().equals(post.getAuthorId())) {
                throw new ForbiddenException("You are not permitted to delete this post.");
            }

            postRepository.deletePost(postId, userID);

        } catch (IllegalArgumentException e) {
            throw new InvalidUserIdFormatException("Invalid user ID format: " + e.getMessage());
        } catch (SQLException e) {
            throw new SQLQueryException("Error occurred while deleting post: " + e.getMessage());
        }
    }


}