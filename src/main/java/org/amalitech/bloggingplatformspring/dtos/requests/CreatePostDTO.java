package org.amalitech.bloggingplatformspring.dtos.requests;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreatePostDTO {
    private String title;
    private String body;
    private List<String> tags;
    private String authorId;
}