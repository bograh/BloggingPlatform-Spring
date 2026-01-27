package org.amalitech.bloggingplatformspring.dao;

import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.config.ConnectionProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Configuration
@Profile("!test")
public class InitDB {

    private final ConnectionProvider connectionProvider;

    public InitDB(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    private Connection getConnection() throws SQLException {
        return connectionProvider.getConnection();
    }

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            createUsersTable();
            createPostsTable();
            createTagsTable();
            createPostTagsTable();
        };
    }

    private void createUsersTable() throws SQLException {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                            id UUID PRIMARY KEY,
                            username VARCHAR(50) UNIQUE NOT NULL,
                            email VARCHAR(100) UNIQUE NOT NULL,
                            password VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            log.info("Users table created successfully");
        }
    }

    private void createPostsTable() throws SQLException {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS posts (
                            id SERIAL PRIMARY KEY,
                            title VARCHAR(255) NOT NULL,
                            body TEXT NOT NULL,
                            author_id UUID NOT NULL,
                            posted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE
                        )
                    """);
            log.info("Posts table created successfully");
        }
    }

    private void createTagsTable() throws SQLException {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS tags (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(255) NOT NULL UNIQUE
                        )
                    """);
            log.info("Tags table created successfully");
        }
    }

    private void createPostTagsTable() throws SQLException {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS post_tags (
                            post_id INT NOT NULL,
                            tag_id  INT NOT NULL,
                            PRIMARY KEY (post_id, tag_id),
                            CONSTRAINT fk_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
                            CONSTRAINT fk_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
                        )
                    """);
            log.info("PostTags table created successfully");
        }
    }

}