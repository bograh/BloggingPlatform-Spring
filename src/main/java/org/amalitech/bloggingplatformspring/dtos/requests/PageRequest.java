package org.amalitech.bloggingplatformspring.dtos.requests;

public record PageRequest(
        int page,
        int size,
        String sortBy,
        String sortDirection
) {
}