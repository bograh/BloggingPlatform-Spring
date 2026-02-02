package org.amalitech.bloggingplatformspring.utils;

import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.entity.Comment;

import java.time.format.DateTimeFormatter;

public final class CommentUtils {

    public static CommentResponse createCommentResponseFromComment(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthor(),
                comment.getContent(),
                comment.getCommentedAt()
                        .format(DateTimeFormatter.ofPattern(Constants.DateTimeFormatPattern))
        );
    }
}