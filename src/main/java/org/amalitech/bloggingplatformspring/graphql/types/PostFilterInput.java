package org.amalitech.bloggingplatformspring.graphql.types;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostFilterInput {
    private String tag;
    private String authorId;
    private String keyword;
}