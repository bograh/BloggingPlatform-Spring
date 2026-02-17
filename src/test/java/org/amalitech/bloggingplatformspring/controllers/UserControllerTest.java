package org.amalitech.bloggingplatformspring.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponseGeneric;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserProfileResponse;
import org.amalitech.bloggingplatformspring.enums.UserRoles;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private UserController userController;

    private UserProfileResponse userProfile;

    @BeforeEach
    void setUp() {
        userProfile = new UserProfileResponse(
                "123e4567-e89b-12d3-a456-426614174000",
                "john_doe",
                "john@example.com",
                10L,
                25L,
                List.of(UserRoles.AUTHOR, UserRoles.READER),
                Collections.singletonList(new PostResponseDTO()),
                Collections.singletonList(new CommentResponse()));
    }

    @Test
    void getUserProfile_shouldReturnOkWithUserProfile_whenUserExists() {
        when(userService.getUserProfile(any(HttpServletRequest.class))).thenReturn(userProfile);

        ResponseEntity<ApiResponseGeneric<UserProfileResponse>> response = userController.getUserProfile(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User profile retrieved successfully", response.getBody().getMessage());
        assertEquals("john_doe", response.getBody().getData().username());
        assertEquals("john@example.com", response.getBody().getData().email());
        assertEquals(10L, response.getBody().getData().totalPosts());
        assertEquals(25L, response.getBody().getData().totalComments());
        assertEquals(2, response.getBody().getData().roles().size());

        verify(userService, times(1)).getUserProfile(any(HttpServletRequest.class));
    }

    @Test
    void getUserProfile_shouldReturnEmptyCollections_whenUserHasNoActivity() {
        UserProfileResponse emptyProfile = new UserProfileResponse(
                "user-001",
                "new_user",
                "new@example.com",
                0L,
                0L,
                List.of(UserRoles.READER),
                Collections.emptyList(),
                Collections.emptyList());

        when(userService.getUserProfile(any(HttpServletRequest.class))).thenReturn(emptyProfile);

        ResponseEntity<ApiResponseGeneric<UserProfileResponse>> response = userController.getUserProfile(request);

        assertNotNull(response.getBody());
        UserProfileResponse data = response.getBody().getData();
        assertEquals(0L, data.totalPosts());
        assertEquals(0L, data.totalComments());
        assertTrue(data.recentPosts().isEmpty());
        assertTrue(data.recentComments().isEmpty());
    }

    @Test
    void getUserProfile_shouldPropagateException_whenUserNotFound() {
        when(userService.getUserProfile(any(HttpServletRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        assertThrows(ResourceNotFoundException.class, () -> userController.getUserProfile(request));

        verify(userService, times(1)).getUserProfile(any(HttpServletRequest.class));
    }
}