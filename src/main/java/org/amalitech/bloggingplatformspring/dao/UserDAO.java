package org.amalitech.bloggingplatformspring.dao;

import org.amalitech.bloggingplatformspring.config.ConnectionProvider;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class UserDAO implements UserRepository {

    private final ConnectionProvider connectionProvider;
    private UserUtils userUtils;

    public UserDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        this.userUtils = new UserUtils();
    }

    private Connection getConnection() throws SQLException {
        return connectionProvider.getConnection();
    }

    @Override
    public User registerUser() throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {

            checkStmt.setString(1, "user");
            checkStmt.setString(2, "user@email.com");

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new BadRequestException("User with this username or email already exists");
                }
            }

            String insertQuery = """
                        INSERT INTO users (username, email, password)
                        VALUES (?, ?, ?)
                        RETURNING id, username, email, password
                    """;
            try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                stmt.setString(1, "user");
                stmt.setString(2, "user@email.com");
                stmt.setString(3, "password");

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return userUtils.mapRowToUser(rs);
                    }
                }
            }
        }
        throw new SQLException("Failed to register user");
    }
}