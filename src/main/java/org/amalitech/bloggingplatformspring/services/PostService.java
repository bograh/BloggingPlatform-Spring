package org.amalitech.bloggingplatformspring.services;

import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.requests.CreatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.exceptions.SQLQueryException;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.PostUtils;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

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
            throw new SQLQueryException("Failed to create post");
        }
    }

    public List<PostResponseDTO> getAllPosts() {
        try {
            return postRepository.getAllPosts();

        } catch (SQLException e) {
            throw new SQLQueryException("Error occurred while fetching posts.");
        }
    }

    public PostResponseDTO getPostById(int postId) {
        if (postId <= 0) {
            throw new BadRequestException("Post ID must be a positive number");
        }

        try {

            return postRepository.getPostById(postId).orElseThrow(
                    () -> new ResourceNotFoundException("Post not Found with ID: " + postId)
            );

        } catch (SQLException e) {
            throw new SQLQueryException("Error occurred while fetching posts.");
        }
    }


}