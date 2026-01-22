package org.amalitech.bloggingplatformspring.dtos.responses;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        String sort,
        long totalElements
) {
}