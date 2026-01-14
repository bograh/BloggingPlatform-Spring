package org.amalitech.bloggingplatformspring.dao;

import org.amalitech.bloggingplatformspring.config.ConnectionProvider;
import org.amalitech.bloggingplatformspring.entity.Tag;
import org.amalitech.bloggingplatformspring.repository.TagRepository;
import org.amalitech.bloggingplatformspring.utils.TagUtils;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class TagDAO implements TagRepository {

    private final ConnectionProvider connectionProvider;
    private final TagUtils tagUtils;

    public TagDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        tagUtils = new TagUtils();
    }

    private Connection getConnection() throws SQLException {
        return connectionProvider.getConnection();
    }

    @Override
    public Tag saveTag(String name) throws SQLException {
        String query = """
                INSERT INTO tags (name)
                VALUES (?)
                ON CONFLICT (name) DO NOTHING
                RETURNING id, name
                """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return tagUtils.mapRowToTag(rs);
                }
            }
        }
        throw new SQLException("Failed to save Tag");
    }

    @Override
    public List<Tag> getAllTags() throws SQLException {
        String query = "SELECT * FROM tags";
        List<Tag> tags = new ArrayList<>();
        TagUtils tagUtils = new TagUtils();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tags.add(tagUtils.mapRowToTag(rs));
            }
        }
        return tags;
    }

    @Override
    public List<Tag> getAllTagsFromNamesList(List<String> tagsList) throws SQLException {
        if (tagsList == null || tagsList.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = String.join(",", Collections.nCopies(tagsList.size(), "?"));
        String query = "SELECT * FROM tags WHERE name IN (" + placeholders + ")";

        List<Tag> tags = new ArrayList<>();
        TagUtils tagUtils = new TagUtils();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            for (int i = 0; i < tagsList.size(); i++) {
                stmt.setString(i + 1, tagsList.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tags.add(tagUtils.mapRowToTag(rs));
                }
            }
        }
        return tags;
    }

    @Override
    public Boolean existsByName(String name) throws SQLException {
        String query = "SELECT 1 FROM tags WHERE name = ? LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public Optional<Integer> getIdByName(String name) throws SQLException {
        String query = "SELECT id FROM tags WHERE name = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("id"));
                } else {
                    return Optional.empty();
                }
            }
        }
    }

}