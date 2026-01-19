package org.amalitech.bloggingplatformspring.dtos.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentDTO {
    @NotBlank(message = "Comment should not be blank")
    private String commentContent;

    @NotNull(message = "Post ID is required")
    @Min(value = 1, message = "Post ID must be greater than 0")
    private Integer postId;

    @NotBlank(message = "Author ID should not be blank")
    private String authorId;
}