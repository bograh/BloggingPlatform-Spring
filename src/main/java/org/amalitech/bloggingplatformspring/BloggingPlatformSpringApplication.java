package org.amalitech.bloggingplatformspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BloggingPlatformSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(BloggingPlatformSpringApplication.class, args);
    }

}