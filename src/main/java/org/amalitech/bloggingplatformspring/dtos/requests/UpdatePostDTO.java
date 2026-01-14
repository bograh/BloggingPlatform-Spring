package org.amalitech.bloggingplatformspring.dtos.requests;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UpdatePostDTO {
    private String title;
    private String body;
    private List<String> tags;
    private UUID authorId;
}