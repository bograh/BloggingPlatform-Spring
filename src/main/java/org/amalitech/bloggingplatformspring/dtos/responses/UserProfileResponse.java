package org.amalitech.bloggingplatformspring.dtos.responses;

import java.util.List;

public record UserProfileResponse(
        String userId,
        String username,
        String email,
        Long totalPosts,
        Long totalComments,
        List<PostResponseDTO> recentPosts,
        List<CommentResponse> recentComments
) {
}