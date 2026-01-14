package org.amalitech.bloggingplatformspring.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.UUID;

import java.util.List;

@Getter
@Setter
public class CreatePostDTO {

    @NotBlank(message = "Title for post should not be blank")
    private String title;

    @NotBlank(message = "Body for post should not be blank")
    private String body;

    private List<String> tags;

    @UUID(message = "Invalid author ID format")
    @NotBlank(message = "Author ID should not be blank")
    private String authorId;
}