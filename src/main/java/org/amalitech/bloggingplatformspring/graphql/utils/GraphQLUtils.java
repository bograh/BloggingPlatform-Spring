package org.amalitech.bloggingplatformspring.graphql.utils;

import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.graphql.types.GraphQLComment;
import org.amalitech.bloggingplatformspring.graphql.types.GraphQLPost;
import org.amalitech.bloggingplatformspring.graphql.types.GraphQLTag;
import org.amalitech.bloggingplatformspring.graphql.types.GraphQLUser;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GraphQLUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT_PATTERN);

    public GraphQLUser mapUserToGraphQLUser(User user) {
        return new GraphQLUser(
                user.getId(),
                user.getUsername(),
                user.getEmail());
    }

    public GraphQLPost mapPostResponseToGraphQLPost(PostResponseDTO postResponse) {
        List<GraphQLTag> tags = postResponse.getTags().stream()
                .map(tagName -> new GraphQLTag(null, tagName))
                .collect(Collectors.toList());

        LocalDateTime postedAt = LocalDateTime.parse(postResponse.getPostedAt(), FORMATTER);
        LocalDateTime updatedAt = LocalDateTime.parse(postResponse.getLastUpdated(), FORMATTER);


        return new GraphQLPost(
                postResponse.getId(),
                postResponse.getTitle(),
                postResponse.getBody(),
                postResponse.getAuthorId(),
                postResponse.getAuthor(),
                tags,
                postResponse.getTotalComments(),
                postedAt,
                updatedAt);
    }

    public GraphQLComment mapCommentResponseToGraphQLComment(CommentResponse comment) {
        LocalDateTime createdAt = LocalDateTime.parse(comment.getCreatedAt(), FORMATTER);

        return new GraphQLComment(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthor(),
                comment.getContent(),
                createdAt);
    }
}