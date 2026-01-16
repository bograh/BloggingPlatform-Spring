package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.entity.CommentDocument;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {

    CommentDocument createComment(Comment comment, String author);

    List<CommentDocument> getAllCommentsByPostId(int postId);

    Optional<CommentDocument> getCommentById(String commentId);

    void deleteComment(String commentId, String authorId);

}