package org.amalitech.bloggingplatformspring.dao;

import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.config.ConnectionProvider;
import org.amalitech.bloggingplatformspring.dtos.requests.CreatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.PageRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.enums.PostSortField;
import org.amalitech.bloggingplatformspring.enums.SortDirection;
import org.amalitech.bloggingplatformspring.exceptions.ForbiddenException;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.TagRepository;
import org.amalitech.bloggingplatformspring.utils.PostUtils;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
public class PostDAO implements PostRepository {

    private final ConnectionProvider connectionProvider;
    private final TagRepository tagRepository;
    private final PostUtils postUtils;

    public PostDAO(ConnectionProvider connectionProvider, TagRepository tagRepository) {
        this.connectionProvider = connectionProvider;
        this.tagRepository = tagRepository;
        this.postUtils = new PostUtils();
    }

    private Connection getConnection() throws SQLException {
        return connectionProvider.getConnection();
    }

    @Override
    public Post savePost(CreatePostDTO createPostDTO) throws SQLException {
        String insertPostQuery = """
                    INSERT INTO posts (title, body, author_id) VALUES (?, ?, ?)
                    RETURNING id, title, body, author_id, posted_at, updated_at
                """;
        String insertPostTagQuery = "INSERT INTO post_tags (post_id, tag_id) VALUES (?, ?)";

        Post post;
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                try (PreparedStatement stmt = conn.prepareStatement(insertPostQuery);
                     ResultSet rs = executeInsert(stmt, createPostDTO)) {

                    if (!rs.next()) {
                        throw new SQLException("Failed to insert post");
                    }
                    post = postUtils.mapRowToPost(rs);
                }

                List<String> tagNames = createPostDTO.getTags();
                savePostTags(post, conn, tagNames);
                conn.commit();
                return post;
            } catch (SQLException e) {
                conn.close();
                throw e;
            }
        }
    }

    @Override
    public List<PostResponseDTO> getAllPosts() throws SQLException {
        String query = """
                SELECT
                    p.id,
                    p.title,
                    p.body,
                    p.updated_at,
                    u.username AS author,
                    COALESCE(ARRAY_AGG (t.name ORDER BY t.name)
                    FILTER (WHERE t.name IS NOT NULL), '{}') AS tags
                FROM posts p
                    JOIN users u ON u.id = p.author_id
                    LEFT JOIN post_tags pt ON pt.post_id = p.id
                    LEFT JOIN tags t ON t.id = pt.tag_id
                GROUP BY
                    p.id, p.title,p.body, p.updated_at, u.username
                ORDER BY p.updated_at DESC
                """;

        List<PostResponseDTO> posts = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                posts.add(postUtils.mapRowToPostResponse(rs));
            }
        }

        return posts;
    }

    public PageResponse<PostResponseDTO> getAllPosts(PageRequest pageRequest) throws SQLException {
        if (pageRequest == null) {
            throw new IllegalArgumentException("PageRequest cannot be null");
        }

        int size = pageRequest.size();
        int page = pageRequest.page();
        int offset = page * size;

        PostSortField sortField = matchSortByToEntityField(pageRequest.sortBy());
        SortDirection direction = getSortDirection(pageRequest.sortDirection());
        String orderByClause = buildOrderByClause(sortField, direction);

        String query = """
                SELECT
                    p.id,
                    p.title,
                    p.body,
                    p.updated_at,
                    u.username AS author,
                    COALESCE(
                        ARRAY_AGG(t.name ORDER BY t.name)
                        FILTER (WHERE t.name IS NOT NULL),
                        '{}'
                    ) AS tags,
                    COUNT(*) OVER() AS total_count
                FROM posts p
                JOIN users u ON u.id = p.author_id
                LEFT JOIN post_tags pt ON pt.post_id = p.id
                LEFT JOIN tags t ON t.id = pt.tag_id
                GROUP BY
                    p.id, p.title, p.body, p.updated_at, u.username
                ORDER BY %s
                LIMIT ? OFFSET ?
                """.formatted(orderByClause);

        List<PostResponseDTO> posts = new ArrayList<>();
        int totalElements = 0;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, size);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    if (totalElements == 0) {
                        totalElements = rs.getInt("total_count");
                    }
                    posts.add(postUtils.mapRowToPostResponse(rs));
                }
            }
        }

        String sort = String.format("%s : %s", sortField.name().toLowerCase(), direction.name());
        return new PageResponse<>(
                posts,
                page,
                size,
                sort,
                totalElements
        );
    }

    @Override
    public Optional<Post> findPostById(int id) throws SQLException {
        String query = "SELECT * FROM posts WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.ofNullable(postUtils.mapRowToPost(rs));
            }

        }
        return Optional.empty();
    }

    @Override
    public Optional<PostResponseDTO> getPostResponseById(int id) throws SQLException {
        String query = """
                SELECT
                    p.id,
                    p.title,
                    p.body,
                    p.updated_at,
                    u.username AS author,
                    COALESCE(ARRAY_AGG (t.name ORDER BY t.name)
                             FILTER (WHERE t.name IS NOT NULL), '{}') AS tags
                FROM posts p
                         JOIN users u ON u.id = p.author_id
                         LEFT JOIN post_tags pt ON pt.post_id = p.id
                         LEFT JOIN tags t ON t.id = pt.tag_id
                WHERE p.id = ?
                GROUP BY
                    p.id, p.title,p.body, p.updated_at, u.username
                ORDER BY p.updated_at DESC
                """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.ofNullable(postUtils.mapRowToPostResponse(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public void updatePost(Post post, List<String> tagNames) throws SQLException {
        String updatePostSql =
                "UPDATE posts SET title=?, body=?, updated_at=CURRENT_TIMESTAMP " +
                        "WHERE id=? AND author_id=?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(updatePostSql)) {
                stmt.setString(1, post.getTitle());
                stmt.setString(2, post.getBody());
                stmt.setInt(3, post.getId());
                stmt.setObject(4, post.getAuthorId());

                if (stmt.executeUpdate() == 0) {
                    throw new ForbiddenException("You are not permitted to update this post");
                }
            }

            try (PreparedStatement ps =
                         conn.prepareStatement("DELETE FROM post_tags WHERE post_id=?")) {
                ps.setInt(1, post.getId());
                ps.executeUpdate();
            }

            savePostTags(post, conn, tagNames);
            conn.commit();

        }
    }

    @Override
    public void deletePost(int postId, UUID signedInUserId) throws SQLException {
        String query = "DELETE FROM posts WHERE id = ? AND author_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, postId);
            stmt.setObject(2, signedInUserId);

            stmt.executeUpdate();
        }
    }

    @Override
    public List<String> getTagsByPostId(int postId) throws SQLException {
        String query = """
                SELECT t.name
                FROM tags t
                JOIN post_tags pt ON pt.tag_id = t.id
                WHERE pt.post_id = ?
                ORDER BY t.name;
                """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, postId);
            ResultSet rs = stmt.executeQuery();

            List<String> tags = new ArrayList<>();


            while (rs.next()) {
                tags.add(rs.getString("name"));
            }
            return tags;
        }
    }

    private ResultSet executeInsert(PreparedStatement stmt, CreatePostDTO dto) throws SQLException {
        stmt.setString(1, dto.getTitle());
        stmt.setString(2, dto.getBody());
        stmt.setObject(3, dto.getAuthorId());
        return stmt.executeQuery();
    }

    private void savePostTags(Post post, Connection conn, List<String> tagNames) throws SQLException {
        if (tagNames != null && !tagNames.isEmpty()) {
            try (PreparedStatement ps =
                         conn.prepareStatement("INSERT INTO post_tags (post_id, tag_id) VALUES (?, ?)")) {
                for (String tagName : tagNames) {
                    int tagId = tagRepository.findOrCreate(tagName, conn);
                    ps.setInt(1, post.getId());
                    ps.setInt(2, tagId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private PostSortField matchSortByToEntityField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return PostSortField.UPDATED_AT;
        }

        return switch (sortBy.toLowerCase().trim()) {
            case "id" -> PostSortField.ID;
            case "title" -> PostSortField.TITLE;
            case "body" -> PostSortField.BODY;
            case "author" -> PostSortField.AUTHOR;
            default -> PostSortField.UPDATED_AT;
        };
    }

    private SortDirection getSortDirection(String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return SortDirection.DESC;
        }

        return switch (sortDirection.toUpperCase().trim()) {
            case "ASC", "ASCENDING" -> SortDirection.ASC;
            default -> SortDirection.DESC;
        };
    }

    private String buildOrderByClause(PostSortField sortField, SortDirection direction) {
        String column = switch (sortField) {
            case ID -> "p.id";
            case TITLE -> "p.title";
            case BODY -> "p.body";
            case AUTHOR -> "u.username";
            case UPDATED_AT -> "p.updated_at";
        };

        String dir = switch (direction) {
            case ASC -> "ASC";
            case DESC -> "DESC";
        };

        return column + " " + dir;
    }
}