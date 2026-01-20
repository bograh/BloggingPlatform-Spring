package org.amalitech.bloggingplatformspring.dtos.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentDTO {
    @NotNull(message = "Post ID is required")
    @Min(value = 1, message = "Post ID must be greater than 0")
    private Integer postId;

    @NotBlank(message = "Comment should not be blank")
    private String commentContent;

    @NotBlank(message = "Author ID should not be blank")
    private String authorId;
}