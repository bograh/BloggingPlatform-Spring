package org.amalitech.bloggingplatformspring.dao;

import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.config.ConnectionProvider;
import org.amalitech.bloggingplatformspring.dtos.requests.CreatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
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
                    RETURNING id, title, body, author_id, updated_at
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
                if (tagNames != null && !tagNames.isEmpty()) {

                    try (PreparedStatement ps = conn.prepareStatement(insertPostTagQuery)) {
                        for (String tagName : tagNames) {
                            int tagId;
                            if (tagRepository.existsByName(tagName)) {
                                tagId = tagRepository.getIdByName(tagName).orElse(-1);
                            } else {
                                tagId = tagRepository.saveTag(tagName).getId();
                            }
//                            tagIds.add(tagId);

                            ps.setInt(1, post.getId());
                            ps.setInt(2, tagId);
                            ps.addBatch();
                        }

                        ps.executeBatch();
                    }
                }
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
                        COALESCE(
                            ARRAY_AGG(t.name ORDER BY t.name)
                            FILTER (WHERE t.name IS NOT NULL),
                            '{}'
                        ) AS tags
                    FROM posts p
                    JOIN users u ON u.id = p.author_id
                    LEFT JOIN post_tags pt ON pt.post_id = p.id
                    LEFT JOIN tags t ON t.id = pt.tag_id
                    GROUP BY p.id, u.username, p.posted_at
                    ORDER BY p.posted_at DESC
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

    @Override
    public PostResponseDTO getPostById(int id) throws SQLException {
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
                        ) AS tags
                    FROM posts p
                    JOIN users u ON u.id = p.author_id
                    LEFT JOIN post_tags pt ON pt.post_id = p.id
                    LEFT JOIN tags t ON t.id = pt.tag_id
                    WHERE p.id = ?
                    GROUP BY p.id, u.username, p.posted_at
                    ORDER BY p.posted_at DESC
                """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return postUtils.mapRowToPostResponse(rs);
            }
        }
        return null;
    }

    @Override
    public Post updatePost() throws SQLException {
        return null;
    }

    @Override
    public void deletePost(int id, String signedInUserId) throws SQLException {

    }

    private ResultSet executeInsert(PreparedStatement stmt, CreatePostDTO dto) throws SQLException {
        stmt.setString(1, dto.getTitle());
        stmt.setString(2, dto.getBody());
        stmt.setString(3, dto.getAuthorId());
        return stmt.executeQuery();
    }
}