package org.amalitech.bloggingplatformspring.utils;

import org.amalitech.bloggingplatformspring.entity.CommentDocument;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class CommentUtils {

    public CommentDocument mapDocumentToComment(Document document) {
        return new CommentDocument(
                document.getObjectId("_id").toHexString(),
                document.getInteger("postId"),
                document.getString("author"),
                document.getString("content"),
                formatCommentedAt(document.getDate("commentedAt"))
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