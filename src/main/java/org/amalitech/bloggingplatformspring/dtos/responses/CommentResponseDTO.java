package org.amalitech.bloggingplatformspring.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CommentResponseDTO {
    private String id;
    private int postId;
    private String author;
    private String content;
    private String createdAt;
}