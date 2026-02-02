package org.amalitech.bloggingplatformspring.graphql.types;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteCommentInput {
    private String authorId;
    private Long postId;
}