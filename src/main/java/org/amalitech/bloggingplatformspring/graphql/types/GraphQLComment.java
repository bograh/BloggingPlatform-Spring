package org.amalitech.bloggingplatformspring.graphql.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphQLComment {
    private String id;
    private Integer postId;
    private String author;
    private String content;
    private LocalDateTime createdAt;
}