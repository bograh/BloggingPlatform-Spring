package org.amalitech.bloggingplatformspring.dtos.requests;

import java.util.List;

public record PostFilterRequest(
        String author,
        String search,
        List<String> tags
) {
}