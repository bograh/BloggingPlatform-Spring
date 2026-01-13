package org.amalitech.bloggingplatformspring.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class DatabaseConfig {

    @Value("${app.db.url}")
    private String dbUrl;

    @Value("${app.db.user}")
    private String dbUser;

    @Value("${app.db.password}")
    private String dbPassword;
}