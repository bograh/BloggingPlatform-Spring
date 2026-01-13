package org.amalitech.bloggingplatformspring.config;

import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class PostgresConnectionProvider implements ConnectionProvider {

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD
        );
    }
}