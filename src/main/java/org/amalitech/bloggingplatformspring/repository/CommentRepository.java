package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.Comment;
import org.springframework.data.domain.Limit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    List<Comment> findByPostIdOrderByCommentedAtDesc(Long postId);

    Long countByPostId(Long postId);

    Long countByAuthor(String author);

    void deleteCommentById(String commentId);

    List<Comment> findCommentsByAuthorOrderByCommentedAtDesc(String author, Limit limit);

    void deleteCommentsByPostId(Long postId);
}