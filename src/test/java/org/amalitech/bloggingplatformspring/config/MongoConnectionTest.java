package org.amalitech.bloggingplatformspring.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoConnectionTest {
    private static MongoClient mongoClient;

    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            mongoClient = MongoClients.create(MongoTestConfig.CONNECTION_STRING);
        }

        return mongoClient.getDatabase(MongoTestConfig.DATABASE_NAME);
    }
}