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

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(MongoDBConfig.CONNECTION_STRING);
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient) {
        log.info("Mongo DB Connection Established");
        return mongoClient.getDatabase(MongoDBConfig.DATABASE_NAME);
    }

}