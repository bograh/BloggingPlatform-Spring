package org.amalitech.bloggingplatformspring.dtos.responses;

public record StatsResponse(
        long totalUsers,
        long totalPosts,
        long totalComments,
        SessionStats sessionStats
) {
}