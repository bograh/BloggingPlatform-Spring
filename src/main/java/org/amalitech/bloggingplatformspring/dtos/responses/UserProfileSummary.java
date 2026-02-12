package org.amalitech.bloggingplatformspring.dtos.responses;

import org.amalitech.bloggingplatformspring.enums.UserRoles;

import java.time.LocalDateTime;
import java.util.List;

public record UserProfileSummary(
        String userId,
        String username,
        String email,
        Long totalPosts,
        Long totalComments,
        List<UserRoles> roles,
        LocalDateTime createdAt
) {
}