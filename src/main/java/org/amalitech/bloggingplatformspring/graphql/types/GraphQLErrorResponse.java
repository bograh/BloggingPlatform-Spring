package org.amalitech.bloggingplatformspring.graphql.types;

public record GraphQLErrorResponse(
        String code,
        int status,
        String message,
        String details
) {
}