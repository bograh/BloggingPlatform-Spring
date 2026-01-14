package org.amalitech.bloggingplatformspring.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    private int id;
    private String title;
    private String body;
    private UUID authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}