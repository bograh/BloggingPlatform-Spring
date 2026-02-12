package org.amalitech.bloggingplatformspring.dtos.responses;

public record AuthResponse(
        UserResponseDTO user,
        String accessToken
) {
}