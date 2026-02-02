package org.amalitech.bloggingplatformspring.graphql.resolvers;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.amalitech.bloggingplatformspring.exceptions.*;
import org.amalitech.bloggingplatformspring.graphql.types.GraphQLErrorResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

@Component
public class CustomExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(
            @NonNull Throwable ex,
            @NonNull DataFetchingEnvironment env
    ) {

        return switch (ex) {
            case UnauthorizedException e ->
                    buildError(e, env, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value(), ErrorType.UNAUTHORIZED);

            case ForbiddenException e ->
                    buildError(e, env, "FORBIDDEN", HttpStatus.FORBIDDEN.value(), ErrorType.FORBIDDEN);

            case BadRequestException e ->
                    buildError(e, env, "BAD_REQUEST", HttpStatus.BAD_REQUEST.value(), ErrorType.BAD_REQUEST);

            case InvalidUserIdFormatException e ->
                    buildError(e, env, "BAD_REQUEST", HttpStatus.BAD_REQUEST.value(), ErrorType.BAD_REQUEST);

            case MethodArgumentNotValidException e ->
                    buildError(e, env, "VALIDATION FAILED", HttpStatus.BAD_REQUEST.value(), ErrorType.BAD_REQUEST);

            case ResourceNotFoundException e ->
                    buildError(e, env, "NOT_FOUND", HttpStatus.NOT_FOUND.value(), ErrorType.NOT_FOUND);

            default -> null;
        };
    }

    private GraphQLError buildError(
            Exception ex,
            DataFetchingEnvironment env,
            String code,
            int status,
            ErrorType errorType
    ) {

        GraphQLErrorResponse errorResponse = new GraphQLErrorResponse(
                code,
                status,
                ex.getMessage(),
                ex.getClass().getSimpleName()
        );

        return GraphqlErrorBuilder.newError(env)
                .message(ex.getMessage())
                .errorType(errorType)
                .extensions(Map.of(
                        "error", errorResponse
                ))
                .build();
    }
}