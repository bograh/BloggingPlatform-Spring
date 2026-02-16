package org.amalitech.bloggingplatformspring.dtos.requests;

import java.util.List;

public record PostFilterRequest(
        String author,
        String search,
        List<String> tags
) {
    public boolean hasFilters() {
        return author != null || search != null || (tags != null && !tags.isEmpty());
    }
}