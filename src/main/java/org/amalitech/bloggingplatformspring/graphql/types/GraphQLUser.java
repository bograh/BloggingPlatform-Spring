package org.amalitech.bloggingplatformspring.graphql.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphQLUser {
    private UUID id;
    private String username;
    private String email;
}