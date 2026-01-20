package org.amalitech.bloggingplatformspring.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentDTO {
    @NotBlank(message = "Comment should not be blank")
    private String commentContent;

    @NotBlank(message = "Post id is required")
    private int postId;

    @NotBlank(message = "Author ID should not be blank")
    private String authorId;
}