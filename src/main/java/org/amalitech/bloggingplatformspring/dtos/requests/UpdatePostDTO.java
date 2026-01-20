package org.amalitech.bloggingplatformspring.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdatePostDTO {
    private String title;
    private String body;
    private List<String> tags;

    @NotBlank(message = "Author ID is required")
    private String authorId;
}