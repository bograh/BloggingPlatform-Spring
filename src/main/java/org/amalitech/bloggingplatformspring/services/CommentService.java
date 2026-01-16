package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.dtos.requests.CreateCommentDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.DeleteCommentRequestDTO;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.entity.CommentDocument;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.InvalidUserIdFormatException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.exceptions.SQLQueryException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
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

    public CommentDocument addCommentToPost(CreateCommentDTO newComment) {
        String authorId = newComment.getAuthorId();
        Comment comment = new Comment(
                newComment.getPostId(),
                authorId,
                newComment.getCommentContent(),
                LocalDateTime.now()
        );

        try {
            User user = userRepository.findUserById(UUID.fromString(authorId)).orElseThrow(
                    () -> new ResourceNotFoundException("User not found")
            );

            return commentRepository.createComment(comment, user.getUsername());

        } catch (IllegalArgumentException ex) {
            throw new InvalidUserIdFormatException("User ID format is invalid: " + ex.getMessage());
        } catch (SQLException e) {
            throw new SQLQueryException("Failed to create comment: " + e.getMessage());
        }
    }

    public List<CommentDocument> getAllCommentsByPostId(int postId) {
        try {
            Post post = postRepository.findPostById(postId).orElseThrow(
                    () -> new ResourceNotFoundException("Post not found with ID: " + postId)
            );

            List<CommentDocument> commentDocuments = commentRepository.getAllCommentsByPostId(post.getId());

            return commentDocuments.isEmpty() ? List.of() : commentDocuments;

        } catch (SQLException e) {
            throw new SQLQueryException("Failed to find comment: " + e.getMessage());
        }
    }

    public CommentDocument getCommentById(String commentId) {

        return commentRepository.getCommentById(commentId).orElseThrow(
                () -> new ResourceNotFoundException("Comment not found with id: " + commentId)
        );
    }

    public void deleteComment(String commentId, DeleteCommentRequestDTO deleteCommentRequestDTO) {
        try {
            String authorId = deleteCommentRequestDTO.getAuthorId();
            UUID userId = UUID.fromString(authorId);
            userRepository.findUserById(userId).orElseThrow(
                    () -> new ResourceNotFoundException("User not found")
            );

            commentRepository.deleteComment(commentId, authorId);

        } catch (IllegalArgumentException ex) {
            throw new InvalidUserIdFormatException("User ID format is invalid: " + ex.getMessage());
        } catch (SQLException e) {
            throw new SQLQueryException("Failed to delete comment: " + e.getMessage());
        }
    }

}