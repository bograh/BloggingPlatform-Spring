package org.amalitech.bloggingplatformspring.graphql.types;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdatePostInput {
    private String title;
    private String body;
    private String authorId;
    private List<String> tags;
}