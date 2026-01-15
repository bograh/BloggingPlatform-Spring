package org.amalitech.bloggingplatformspring.dtos.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeletePostRequestDTO {
    private String authorId;
}