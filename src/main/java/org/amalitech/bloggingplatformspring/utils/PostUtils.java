package org.amalitech.bloggingplatformspring.utils;

import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.Tag;
import org.amalitech.bloggingplatformspring.enums.PostSortField;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.springframework.data.domain.Page;

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
                (long) id, title, body,
                null, createdAt, updatedAt, null
        );
    }

    public PostResponseDTO mapRowToPostResponse(ResultSet rs, Long totalComments) throws SQLException {
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
                (long) id, title, body,
                author, tags, formatDate(updatedAt), totalComments
        );
    }

    public PostResponseDTO createResponseFromPostAndTags(Post post, String authorName, List<String> tags, Long totalComments) {
        return new PostResponseDTO(
                post.getId(), post.getTitle(), post.getBody(), authorName, tags, formatDate(post.getUpdatedAt()), totalComments
        );
    }

    public PostResponseDTO createPostResponseFromPost(Post post, Long totalComments) {
        return new PostResponseDTO(
                post.getId(),
                post.getTitle(),
                post.getBody(),
                post.getAuthor().getUsername(),
                post.getTags().stream()
                        .map(Tag::getName)
                        .toList(),
                formatDate(post.getUpdatedAt()),
                totalComments
        );
    }

    public PageResponse<PostResponseDTO> mapPostPageToPostResponsePage(Page<Post> postPage, CommentRepository commentRepository) {
        List<PostResponseDTO> postsResponse = postPage.getContent().stream()
                .map(post -> {
                    Long totalComments = commentRepository.countByPostId(post.getId());
                    return createPostResponseFromPost(post, totalComments);
                })
                .toList();

        return new PageResponse<>(
                postsResponse,
                postPage.getPageable().getPageNumber(),
                postPage.getSize(),
                postPage.getSort().toString(),
                postPage.getTotalElements()
        );
    }


    public String mapSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return PostSortField.UPDATED_AT.sqlName();
        }

        return switch (sortBy.toLowerCase().trim()) {
            case "id" -> PostSortField.ID.sqlName();
            case "title" -> PostSortField.TITLE.sqlName();
            case "body" -> PostSortField.BODY.sqlName();
            case "author" -> PostSortField.AUTHOR.sqlName();
            default -> PostSortField.UPDATED_AT.sqlName();
        };
    }

    public String mapOrderField(String order) {
        if (order == null || order.isBlank()) {
            return "DESC";
        }

        if (order.trim().equalsIgnoreCase("asc"))
            return "ASC";

        return "DESC";
    }

    private String formatDate(LocalDateTime localDateTime) {
        return localDateTime.format(
                DateTimeFormatter.ofPattern(Constants.DateTimeFormatPattern)
        );
    }


}