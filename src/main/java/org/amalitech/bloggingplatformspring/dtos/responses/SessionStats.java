package org.amalitech.bloggingplatformspring.dtos.responses;

public record SessionStats(
        long activeSessions,
        long revokedTokens
) {
}