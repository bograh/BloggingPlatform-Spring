package org.amalitech.bloggingplatformspring.dtos.requests;

public record CommentFilterRequest(
        String author,
        Long postId,
        String search
) {
}