package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.dtos.requests.CreateCommentDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.DeleteCommentRequestDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.ForbiddenException;
import org.amalitech.bloggingplatformspring.exceptions.InvalidUserIdFormatException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.CommentUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, UserRepository userRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "comments", allEntries = true),
            @CacheEvict(cacheNames = "users", key = "#newComment.getAuthorId"),
    })
    public CommentResponse addCommentToPost(CreateCommentDTO newComment) {
        String authorId = newComment.getAuthorId();
        Comment comment = new Comment();
        comment.setContent(newComment.getCommentContent());
        comment.setPostId(newComment.getPostId());
        comment.setCommentedAt(LocalDateTime.now());

        try {
            User user = userRepository.findById(UUID.fromString(authorId)).orElseThrow(
                    () -> new ResourceNotFoundException("User not found")
            );

            comment.setAuthorId(String.valueOf(user.getId()));
            comment.setAuthor(user.getUsername());

            commentRepository.save(comment);

            return CommentUtils.createCommentResponseFromComment(comment);

        } catch (IllegalArgumentException ex) {
            throw new InvalidUserIdFormatException("User ID format is invalid: " + ex.getMessage());
        }
    }

    @Cacheable(cacheNames = "comments", key = "'post:' + #postId")
    public List<CommentResponse> getAllCommentsByPostId(Long postId) {
        postRepository.findPostById(postId).orElseThrow(
                () -> new ResourceNotFoundException("Post not found with ID: " + postId)
        );
        List<Comment> comments = commentRepository.findByPostIdOrderByCommentedAtDesc(postId);

        return comments.stream()
                .map(CommentUtils::createCommentResponseFromComment)
                .toList();
    }

    @Cacheable(cacheNames = "comments", key = "#commentId")
    public CommentResponse getCommentById(String commentId) {

        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new ResourceNotFoundException("Comment not found with id: " + commentId)
        );

        return CommentUtils.createCommentResponseFromComment(comment);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "comments", allEntries = true),
            @CacheEvict(cacheNames = "users", key = "#deleteCommentRequestDTO.getAuthorId"),
    })
    public void deleteComment(String commentId, DeleteCommentRequestDTO deleteCommentRequestDTO) {
        try {
            String authorId = deleteCommentRequestDTO.getAuthorId();
            UUID userId = UUID.fromString(authorId);
            User user = userRepository.findById(userId).orElseThrow(
                    () -> new ResourceNotFoundException("User not found")
            );

            Comment comment = commentRepository.findById(commentId).orElseThrow(
                    () -> new ResourceNotFoundException("Comment not found with id: " + commentId)
            );

            if (!comment.getAuthorId().equalsIgnoreCase(String.valueOf(user.getId()))) {
                throw new ForbiddenException("You cannot delete this comment");
            }

            commentRepository.deleteCommentById(commentId);

        } catch (IllegalArgumentException ex) {
            throw new InvalidUserIdFormatException("User ID format is invalid: " + ex.getMessage());
        }
    }

}