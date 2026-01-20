package org.amalitech.bloggingplatformspring.utils;

import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PostUtils {

    public Post mapRowToPost(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String body = rs.getString("body");
        LocalDateTime createdAt = rs.getTimestamp("posted_at").toLocalDateTime();
        LocalDateTime updatedAt = rs.getTimestamp("updated_at").toLocalDateTime();
        UUID authorId = (UUID) rs.getObject("author_id");

        return new Post(
                id, title, body,
                authorId, createdAt, updatedAt
        );
    }

    public PostResponseDTO mapRowToPostResponse(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String body = rs.getString("body");
        String author = rs.getString("author");
        LocalDateTime updatedAt = rs.getTimestamp("updated_at").toLocalDateTime();

        Array tagsArray = rs.getArray("tags");
        List<String> tags = tagsArray == null
                ? List.of()
                : Arrays.asList((String[]) tagsArray.getArray());

        return new PostResponseDTO(
                id, title, body,
                author, tags, formatDate(updatedAt)
        );
    }

    public PostResponseDTO createResponseFromPostAndTags(Post post, String authorName, List<String> tags) {
        return new PostResponseDTO(
                post.getId(), post.getTitle(), post.getBody(), authorName, tags, formatDate(post.getUpdatedAt())
        );
    }

    private String formatDate(LocalDateTime localDateTime) {
        return localDateTime.format(
                DateTimeFormatter.ofPattern(Constants.DateTimeFormatPattern)
        );
    }

}