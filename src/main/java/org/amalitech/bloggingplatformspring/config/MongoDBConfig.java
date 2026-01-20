package org.amalitech.bloggingplatformspring.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class MongoDBConfig {

    @Value("${app.mongo.conn.string}")
    private String connectionString;

    @Value("${app.mongo.database}")
    private String databaseName;
}