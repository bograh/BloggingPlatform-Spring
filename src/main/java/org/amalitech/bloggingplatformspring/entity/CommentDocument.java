package org.amalitech.bloggingplatformspring.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentDocument {
    private String id;
    private int postId;
    private String author;
    private String content;
    private String createdAt;
}