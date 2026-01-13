package org.amalitech.bloggingplatformspring.config;

import org.springframework.beans.factory.annotation.Value;

public class DatabaseConfig {

    @Value("${app.db.url}")
    public static String DB_URL;

    @Value("${app.db.user}")
    public static String DB_USER;

    @Value("${app.db.password}")
    public static String DB_PASSWORD;
}