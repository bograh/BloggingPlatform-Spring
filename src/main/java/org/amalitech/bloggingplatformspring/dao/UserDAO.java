package org.amalitech.bloggingplatformspring.dao;

import org.amalitech.bloggingplatformspring.config.ConnectionProvider;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.ForbiddenException;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Repository
public class UserDAO implements UserRepository {

    private final ConnectionProvider connectionProvider;
    private final UserUtils userUtils;

    public UserDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        this.userUtils = new UserUtils();
    }

    private Connection getConnection() throws SQLException {
        return connectionProvider.getConnection();
    }

    @Override
    public User saveUser(String username, String email, String password) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {

            checkStmt.setString(1, username);
            checkStmt.setString(2, email);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new BadRequestException("User with this username or email already exists");
                }
            }

            String insertQuery = """
                        INSERT INTO users (id, username, email, password)
                        VALUES (?, ?, ?, ?)
                        RETURNING id, username, email, password, created_at
                    """;
            try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setString(2, username);
                stmt.setString(3, email);
                stmt.setString(4, password);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return userUtils.mapRowToUser(rs);
                    }
                }
            }
        }
        throw new SQLException("Failed to register user");
    }


    @Override
    public User getUserByEmailAndPassword(String email, String password) throws SQLException {
        UserUtils userUtils = new UserUtils();
        String query = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return userUtils.mapRowToUser(rs);
            }
        }
        throw new ForbiddenException("Invalid email or password");
    }

    @Override
    public String getUsernameById(String authorId) throws SQLException {
        String query = "SELECT username FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, authorId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
        }
        return "";
    }


}