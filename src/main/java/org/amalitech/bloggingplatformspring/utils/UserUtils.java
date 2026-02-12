package org.amalitech.bloggingplatformspring.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.*;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.enums.UserSortField;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.security.JwtTokenProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserUtils {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public UserUtils(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    public UserResponseDTO mapUserToUserResponse(User user) {
        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(String.valueOf(user.getId()));
        userResponseDTO.setUsername(user.getUsername());
        userResponseDTO.setEmail(user.getEmail());
        userResponseDTO.setRoles(user.getUserRoles());
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
                user.getUserRoles(),
                recentPosts,
                recentComments
        );
    }

    public User getUserFromRequest(HttpServletRequest request) {
        String token = jwtTokenProvider.getTokenFromRequest(request);
        String email = jwtTokenProvider.getEmailFromAccessToken(token);
        return userRepository.findUserByEmailIgnoreCase(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found with email: " + email)
        );
    }

    public PageResponse<UserResponseDTO> mapUserPageToUserResponsePage(Page<User> userPage) {
        List<UserResponseDTO> userResponseDTOS = userPage.getContent().stream()
                .map(this::mapUserToUserResponse)
                .toList();

        return new PageResponse<>(
                userResponseDTOS,
                userPage.getPageable().getPageNumber(),
                userPage.getSize(),
                userPage.getSort().toString(),
                userPage.getTotalElements(),
                userPage.isLast()
        );
    }

    public String mapOrderField(String order) {
        if (order == null || order.isBlank()) {
            return "DESC";
        }

        if (order.trim().equalsIgnoreCase("asc"))
            return "ASC";

        return "DESC";
    }

    public Pageable createPageable(int page, int size, String sortBy, String order) {
        String entitySortField = mapSortField(sortBy);
        Sort.Direction direction = Sort.Direction.fromString(mapOrderField(order));
        Sort sort = Sort.by(direction, entitySortField);
        return PageRequest.of(page, size, sort);
    }

    public String mapSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return UserSortField.CREATED_AT.getPropertyName();
        }

        return switch (sortBy.toLowerCase().trim()) {
            case "username" -> UserSortField.USERNAME.getPropertyName();
            case "email" -> UserSortField.EMAIL.getPropertyName();
            default -> UserSortField.CREATED_AT.getPropertyName();
        };
    }

    public UserProfileSummary createUserProfileSummary(User user, Long totalPosts, Long totalComments) {
        return new UserProfileSummary(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getEmail(),
                totalPosts,
                totalComments,
                user.getUserRoles(),
                user.getCreatedAt()
        );
    }
}