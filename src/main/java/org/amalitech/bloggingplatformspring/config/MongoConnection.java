package org.amalitech.bloggingplatformspring.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MongoConnection {

    private final MongoDBConfig mongoDBConfig;

    public MongoConnection(MongoDBConfig mongoDBConfig) {
        this.mongoDBConfig = mongoDBConfig;
    }

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(mongoDBConfig.getConnectionString());
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient) {
        log.info("Mongo DB Connection Established");
        return mongoClient.getDatabase(mongoDBConfig.getDatabaseName());
    }

}