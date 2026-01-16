package org.amalitech.bloggingplatformspring.config;

import org.springframework.stereotype.Component;

@Component
public class MongoDBConfig {
    public static final String CONNECTION_STRING = "mongodb://localhost:27017";
    public static final String DATABASE_NAME = "blog_db";
}