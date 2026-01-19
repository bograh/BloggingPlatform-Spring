package org.amalitech.bloggingplatformspring.graphql.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphQLPost {
    private Integer id;
    private String title;
    private String body;
    private String author;
    private List<GraphQLTag> tags;
    private LocalDateTime updatedAt;
}