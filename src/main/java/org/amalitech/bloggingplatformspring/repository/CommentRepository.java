package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.Comment;
import org.springframework.data.domain.Limit;
import org.springframework.data.mongodb.repository.Aggregation;
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

    @Aggregation(pipeline = {
            "{ '$addFields': { 'postIdLong': { '$convert': { 'input': '$post_id', 'to': 'long', 'onError': null, 'onNull': null } } } }",
            "{ '$match': { 'postIdLong': { '$in': ?0 } } }",
            "{ '$group': { '_id': '$postIdLong', 'totalComments': { '$sum': 1 } } }",
            "{ '$project': { '_id': 0, 'postId': '$_id', 'totalComments': 1 } }"
    })
    List<PostCommentCountProjection> countCommentsByPostIds(List<Long> postIds);

    void deleteCommentsByPostId(Long postId);
}