package org.amalitech.bloggingplatformspring.graphql.types;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateCommentInput {
    private Long postId;
    private String authorId;
    private String commentContent;

}