package org.amalitech.bloggingplatformspring.graphql.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphQLTag {
    private Long id;
    private String name;
}