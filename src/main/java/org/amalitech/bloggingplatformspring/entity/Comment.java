package org.amalitech.bloggingplatformspring.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    private int postId;
    private String authorId;
    private String content;
    private LocalDateTime createdAt;
}