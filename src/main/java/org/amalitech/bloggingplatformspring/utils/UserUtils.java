package org.amalitech.bloggingplatformspring.utils;

import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserProfileResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserUtils {

    public UserResponseDTO mapUserToUserResponse(User user) {
        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(String.valueOf(user.getId()));
        userResponseDTO.setUsername(user.getUsername());
        userResponseDTO.setEmail(user.getEmail());
        return userResponseDTO;
    }

    public UserProfileResponse createUserProfileResponse(
            User user, List<PostResponseDTO> recentPosts, List<CommentResponse> recentComments,
            Long totalPosts, Long totalComments
    ) {
        return new UserProfileResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getEmail(),
                totalPosts,
                totalComments,
                recentPosts,
                recentComments
        );

    }
}