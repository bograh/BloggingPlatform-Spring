package org.amalitech.bloggingplatformspring.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import org.amalitech.bloggingplatformspring.config.MongoConnection;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.entity.CommentDocument;
import org.amalitech.bloggingplatformspring.exceptions.ForbiddenException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.utils.CommentUtils;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CommentDAO implements CommentRepository {

    private final MongoCollection<Document> commentsCollection;
    private final CommentUtils commentUtils;

    public CommentDAO(MongoDatabase mongoDatabase) {
        this.commentUtils = new CommentUtils();
        this.commentsCollection = mongoDatabase.getCollection(Constants.CommentsMongoCollection);
    }

    @Override
    public CommentDocument createComment(Comment comment, String author) {
        Document document = new Document("content", comment.getContent())
                .append("postId", comment.getPostId())
                .append("authorId", comment.getAuthorId())
                .append("author", author)
                .append("commentedAt", new Date());

        InsertOneResult result = commentsCollection.insertOne(document);

        if (!result.wasAcknowledged()) {
            throw new RuntimeException("Failed to create comment");
        }

        return commentUtils.mapDocumentToComment(document);
    }

    @Override
    public List<CommentDocument> getAllCommentsByPostId(int postId) {
        Bson filter = Filters.eq("postId", postId);
        CommentUtils commentUtils = new CommentUtils();

        return commentsCollection.find(filter)
                .into(new ArrayList<>())
                .stream()
                .map(commentUtils::mapDocumentToComment)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CommentDocument> getCommentById(String commentId) {
        ObjectId objectId = new ObjectId(commentId);
        CommentUtils commentUtils = new CommentUtils();
        Document document = commentsCollection.find(Filters.eq("_id", objectId)).first();

        if (document == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(commentUtils.mapDocumentToComment(document));
    }

    @Override
    public void deleteComment(String commentId, String authorId) {
        ObjectId objectId = new ObjectId(commentId);
        DeleteResult result = commentsCollection.deleteOne(
                Filters.and(
                        Filters.eq("_id", objectId),
                        Filters.eq("authorId", authorId)));
        if (result.getDeletedCount() == 0) {
            throw new ForbiddenException("You are not allowed to delete this comment.");
        }
        result.getDeletedCount();
    }
}