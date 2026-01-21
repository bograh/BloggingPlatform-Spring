package org.amalitech.bloggingplatformspring.utils;

import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class CommentUtils {

    public CommentResponse mapDocumentToComment(Document document) {
        return new CommentResponse(
                document.getObjectId("_id").toHexString(),
                (long) document.getInteger("postId"),
                document.getString("author"),
                document.getString("content"),
                formatCommentedAt(document.getDate("commentedAt"))
        );
    }

    public CommentResponse createCommentResponseFromComment(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthor(),
                comment.getContent(),
                comment.getCommentedAt()
                        .format(DateTimeFormatter.ofPattern(Constants.DateTimeFormatPattern))
        );
    }

    private String formatCommentedAt(Date date) {
        if (date == null) return null;
        LocalDateTime commentedAt = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        return commentedAt.format(DateTimeFormatter.ofPattern(Constants.DateTimeFormatPattern));
    }

}