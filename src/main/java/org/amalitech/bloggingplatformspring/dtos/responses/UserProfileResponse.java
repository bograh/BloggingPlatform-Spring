package org.amalitech.bloggingplatformspring.dtos.responses;

import org.amalitech.bloggingplatformspring.enums.UserRoles;

import java.util.List;

public record UserProfileResponse(
        String userId,
        String username,
        String email,
        Long totalPosts,
        Long totalComments,
        List<UserRoles> roles,
        List<PostResponseDTO> recentPosts,
        List<CommentResponse> recentComments
) {
}