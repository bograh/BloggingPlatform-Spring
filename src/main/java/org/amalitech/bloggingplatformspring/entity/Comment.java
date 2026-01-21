package org.amalitech.bloggingplatformspring.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "comments")
public class Comment {

    @Id
    private String id;

    @Field("post_id")
    private Long postId;

    @Field("author_id")
    private String authorId;

    @Field("author")
    private String author;

    @Field("content")
    private String content;

    @Field("commented_at")
    private LocalDateTime commentedAt;
}