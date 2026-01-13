package org.amalitech.bloggingplatformspring.config;

import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class PostgresConnectionProvider implements ConnectionProvider {
    private final DatabaseConfig databaseConfig;

    public PostgresConnectionProvider(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                databaseConfig.getDbUrl(),
                databaseConfig.getDbUser(),
                databaseConfig.getDbPassword()
        );
    }
}