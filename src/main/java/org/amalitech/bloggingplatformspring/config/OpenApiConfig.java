package org.amalitech.bloggingplatformspring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration for the Blogging Platform API.
 * Provides automatic API documentation accessible via Swagger UI.
 * <p>
 * Access points:
 * - Swagger UI: <a href="http://localhost:8080/swagger-ui.html">...</a>
 * - API Docs JSON: <a href="http://localhost:8080/v3/api-docs">...</a>
 * - API Docs YAML: <a href="http://localhost:8080/v3/api-docs.yaml">...</a>
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI blogPlatformOpenAPI() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Development Server");

        Contact contact = new Contact()
                .name("Blogging Platform API Team");

        License mitLicense = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Blogging Platform API")
                .version("1.0.0")
                .description("""
                        A comprehensive RESTful API for managing a blogging platform with advanced features.
                        
                        ## Features
                        - **User Management**: Register and authenticate users
                        - **Post Management**: Create, read, update, and delete blog posts with pagination and filtering
                        - **Comment System**: Add and manage comments on posts (MongoDB-backed)
                        - **Performance Metrics**: Monitor API performance with detailed metrics
                        - **GraphQL Support**: Alternative GraphQL API available at /graphql
                        
                        ## Authentication
                        Currently using basic authentication. Include user credentials in request bodies.
                        
                        ## Data Stores
                        - PostgreSQL: Users, Posts, Tags
                        - MongoDB: Comments
                        """)
                .contact(contact)
                .license(mitLicense);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}