package org.amalitech.bloggingplatformspring.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostDTO {

    @NotBlank(message = "Title for post should not be blank")
    private String title;

    @NotBlank(message = "Body for post should not be blank")
    private String body;

    @NotBlank(message = "Author ID should not be blank")
    private String authorId;

    private List<String> tags;
}