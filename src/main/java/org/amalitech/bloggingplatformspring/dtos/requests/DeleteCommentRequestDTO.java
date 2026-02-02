package org.amalitech.bloggingplatformspring.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteCommentRequestDTO {
    private String authorId;
    private Long postId;
}