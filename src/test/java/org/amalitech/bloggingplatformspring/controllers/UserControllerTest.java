package org.amalitech.bloggingplatformspring.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.*;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.InvalidUserIdFormatException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;
    private UUID userID;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
        userID = UUID.randomUUID();
    }

    @Test
    void registerUser_WithValidData_ShouldReturnCreatedStatus() throws Exception {
        RegisterUserDTO registerDTO = new RegisterUserDTO();
        registerDTO.setEmail("test@example.com");
        registerDTO.setPassword("password123");
        registerDTO.setUsername("testuser");

        UserResponseDTO userResponse = new UserResponseDTO();
        userResponse.setId(String.valueOf(userID));
        userResponse.setEmail("test@example.com");
        userResponse.setUsername("testuser");

        when(userService.registerUser(any(RegisterUserDTO.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registration successful"))
                .andExpect(jsonPath("$.data.id").value(String.valueOf(userID)))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void registerUser_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        RegisterUserDTO registerDTO = new RegisterUserDTO();

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signInUser_WithValidCredentials_ShouldReturnOkStatus() throws Exception {
        SignInUserDTO signInDTO = new SignInUserDTO();
        signInDTO.setEmail("test@example.com");
        signInDTO.setPassword("password123");

        UserResponseDTO userResponse = new UserResponseDTO();
        userResponse.setId(String.valueOf(userID));
        userResponse.setEmail("test@example.com");
        userResponse.setUsername("testuser");

        when(userService.signInUser(any(SignInUserDTO.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/users/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User sign in successful"))
                .andExpect(jsonPath("$.data.id").value(String.valueOf(userID)))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void signInUser_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        SignInUserDTO signInDTO = new SignInUserDTO();

        mockMvc.perform(post("/api/v1/users/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signInUser_WithInvalidCredentials_ShouldThrowException() throws Exception {
        SignInUserDTO signInDTO = new SignInUserDTO();
        signInDTO.setEmail("test@example.com");
        signInDTO.setPassword("wrongpassword");

        when(userService.signInUser(any(SignInUserDTO.class)))
                .thenThrow(new BadRequestException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/users/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInDTO)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void registerUser_WhenUserAlreadyExists_ShouldThrowException() throws Exception {
        RegisterUserDTO registerDTO = new RegisterUserDTO();
        registerDTO.setEmail("existing@example.com");
        registerDTO.setPassword("password123");
        registerDTO.setUsername("existinguser");

        when(userService.registerUser(any(RegisterUserDTO.class)))
                .thenThrow(new BadRequestException("User already exists"));

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getUserProfile_shouldReturnOkWithUserProfile_whenUserExists() {
        String userId = "123e4567-e89b-12d3-a456-426614174000";

        PostResponseDTO post1 = new PostResponseDTO();
        PostResponseDTO post2 = new PostResponseDTO();
        List<PostResponseDTO> recentPosts = Arrays.asList(post1, post2);

        CommentResponse comment1 = new CommentResponse();
        CommentResponse comment2 = new CommentResponse();
        List<CommentResponse> recentComments = Arrays.asList(comment1, comment2);

        UserProfileResponse userProfile = new UserProfileResponse(
                userId,
                "john_doe",
                "john@example.com",
                10L,
                25L,
                recentPosts,
                recentComments
        );

        when(userService.getUserProfile(userId)).thenReturn(userProfile);

        ResponseEntity<ApiResponseGeneric<UserProfileResponse>> response =
                userController.getUserProfile(userId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getStatusCode().value());

        ApiResponseGeneric<UserProfileResponse> body = response.getBody();
        assertNotNull(body);
        assertEquals("User profile retrieved successfully", body.getMessage());
        assertNotNull(body.getData());
        assertEquals(userId, body.getData().userId());
        assertEquals("john_doe", body.getData().username());
        assertEquals("john@example.com", body.getData().email());
        assertEquals(10L, body.getData().totalPosts());
        assertEquals(25L, body.getData().totalComments());
        assertEquals(2, body.getData().recentPosts().size());
        assertEquals(2, body.getData().recentComments().size());

        verify(userService, times(1)).getUserProfile(userId);
    }

    @Test
    void getUserProfile_shouldCallServiceWithCorrectUserId() {
        String userId = "test-user-123";
        UserProfileResponse userProfile = new UserProfileResponse(
                userId, "user", "user@example.com", 0L, 0L,
                Collections.emptyList(), Collections.emptyList()
        );

        when(userService.getUserProfile(userId)).thenReturn(userProfile);

        userController.getUserProfile(userId);

        verify(userService, times(1)).getUserProfile(eq(userId));
        verifyNoMoreInteractions(userService);
    }

    @Test
    void getUserProfile_shouldReturnCompleteUserProfile_withAllFields() {
        String userId = "user-456";

        PostResponseDTO post = new PostResponseDTO();
        List<PostResponseDTO> recentPosts = Collections.singletonList(post);

        CommentResponse comment = new CommentResponse();
        List<CommentResponse> recentComments = Collections.singletonList(comment);

        UserProfileResponse userProfile = new UserProfileResponse(
                userId,
                "jane_smith",
                "jane@example.com",
                50L,
                100L,
                recentPosts,
                recentComments
        );

        when(userService.getUserProfile(userId)).thenReturn(userProfile);

        ResponseEntity<ApiResponseGeneric<UserProfileResponse>> response =
                userController.getUserProfile(userId);

        assertNotNull(response);
        assertNotNull(response.getBody());
        UserProfileResponse data = response.getBody().getData();
        assertEquals("jane_smith", data.username());
        assertEquals("jane@example.com", data.email());
        assertEquals(50L, data.totalPosts());
        assertEquals(100L, data.totalComments());
        assertNotNull(data.recentPosts());
        assertEquals(1, data.recentPosts().size());
        assertNotNull(data.recentComments());
        assertEquals(1, data.recentComments().size());

        verify(userService, times(1)).getUserProfile(userId);
    }

    @Test
    void getUserProfile_shouldHandleUserWithNoPosts() {
        String userId = "user-789";

        UserProfileResponse userProfile = new UserProfileResponse(
                userId,
                "new_user",
                "newuser@example.com",
                0L,
                0L,
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(userService.getUserProfile(userId)).thenReturn(userProfile);

        ResponseEntity<ApiResponseGeneric<UserProfileResponse>> response =
                userController.getUserProfile(userId);

        assertNotNull(response);
        assertNotNull(response.getBody());
        UserProfileResponse data = response.getBody().getData();
        assertEquals(0L, data.totalPosts());
        assertEquals(0L, data.totalComments());
        assertTrue(data.recentPosts().isEmpty());
        assertTrue(data.recentComments().isEmpty());

        verify(userService, times(1)).getUserProfile(userId);
    }

    @Test
    void getUserProfile_shouldReturnHttpStatus200() {
        String userId = "user-abc";
        UserProfileResponse userProfile = new UserProfileResponse(
                userId, "user", "user@example.com", 5L, 10L,
                Collections.emptyList(), Collections.emptyList()
        );

        when(userService.getUserProfile(userId)).thenReturn(userProfile);

        ResponseEntity<ApiResponseGeneric<UserProfileResponse>> response =
                userController.getUserProfile(userId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getUserProfile_shouldReturnApiResponseGeneric_withCorrectStructure() {
        String userId = "user-xyz";
        UserProfileResponse userProfile = new UserProfileResponse(
                userId, "user", "user@example.com", 0L, 0L,
                Collections.emptyList(), Collections.emptyList()
        );

        when(userService.getUserProfile(userId)).thenReturn(userProfile);

        ResponseEntity<ApiResponseGeneric<UserProfileResponse>> response =
                userController.getUserProfile(userId);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
    }

    @Test
    void getUserProfile_shouldPropagateException_whenUserNotFound() {
        String userId = "nonexistent-user";

        when(userService.getUserProfile(userId))
                .thenThrow(new ResourceNotFoundException("User not found"));

        assertThrows(ResourceNotFoundException.class,
                () -> userController.getUserProfile(userId));

        verify(userService, times(1)).getUserProfile(userId);
    }

    @Test
    void getUserProfile_shouldPropagateException_whenInvalidUserIdFormat() {
        String userId = "invalid-format";

        when(userService.getUserProfile(userId))
                .thenThrow(new InvalidUserIdFormatException("Invalid user ID format"));

        assertThrows(InvalidUserIdFormatException.class,
                () -> userController.getUserProfile(userId));

        verify(userService, times(1)).getUserProfile(userId);
    }

    @Test
    void getUserProfile_shouldHandleUserWithMultiplePosts() {
        String userId = "user-123";

        PostResponseDTO post1 = new PostResponseDTO(/* post1 data */);
        PostResponseDTO post2 = new PostResponseDTO(/* post2 data */);
        PostResponseDTO post3 = new PostResponseDTO(/* post3 data */);
        List<PostResponseDTO> recentPosts = Arrays.asList(post1, post2, post3);

        CommentResponse comment1 = new CommentResponse(/* comment1 data */);
        CommentResponse comment2 = new CommentResponse(/* comment2 data */);
        List<CommentResponse> recentComments = Arrays.asList(comment1, comment2);

        UserProfileResponse userProfile = new UserProfileResponse(
                userId,
                "active_user",
                "active@example.com",
                100L,
                250L,
                recentPosts,
                recentComments
        );

        when(userService.getUserProfile(userId)).thenReturn(userProfile);

        ResponseEntity<ApiResponseGeneric<UserProfileResponse>> response =
                userController.getUserProfile(userId);

        assertNotNull(response);
        assertNotNull(response.getBody());
        UserProfileResponse data = response.getBody().getData();
        assertEquals(100L, data.totalPosts());
        assertEquals(250L, data.totalComments());
        assertEquals(3, data.recentPosts().size());
        assertEquals(2, data.recentComments().size());

        verify(userService, times(1)).getUserProfile(userId);
    }

    @Test
    void getUserProfile_shouldReturnSuccessMessage() {
        String userId = "user-success";
        UserProfileResponse userProfile = new UserProfileResponse(
                userId, "user", "user@example.com", 0L, 0L,
                Collections.emptyList(), Collections.emptyList()
        );

        when(userService.getUserProfile(userId)).thenReturn(userProfile);

        ResponseEntity<ApiResponseGeneric<UserProfileResponse>> response =
                userController.getUserProfile(userId);

        assertNotNull(response.getBody());
        assertEquals("User profile retrieved successfully", response.getBody().getMessage());
    }

    @Test
    void getUserProfile_shouldHandleDifferentUserIdFormats() {
        String uuidUserId = "123e4567-e89b-12d3-a456-426614174000";
        String numericUserId = "12345";
        String alphanumericUserId = "user-abc-123";

        UserProfileResponse profile1 = new UserProfileResponse(
                uuidUserId, "user1", "user1@example.com", 1L, 1L,
                Collections.emptyList(), Collections.emptyList()
        );
        UserProfileResponse profile2 = new UserProfileResponse(
                numericUserId, "user2", "user2@example.com", 2L, 2L,
                Collections.emptyList(), Collections.emptyList()
        );
        UserProfileResponse profile3 = new UserProfileResponse(
                alphanumericUserId, "user3", "user3@example.com", 3L, 3L,
                Collections.emptyList(), Collections.emptyList()
        );

        when(userService.getUserProfile(uuidUserId)).thenReturn(profile1);
        when(userService.getUserProfile(numericUserId)).thenReturn(profile2);
        when(userService.getUserProfile(alphanumericUserId)).thenReturn(profile3);

        ResponseEntity<ApiResponseGeneric<UserProfileResponse>> response1 =
                userController.getUserProfile(uuidUserId);
        ResponseEntity<ApiResponseGeneric<UserProfileResponse>> response2 =
                userController.getUserProfile(numericUserId);
        ResponseEntity<ApiResponseGeneric<UserProfileResponse>> response3 =
                userController.getUserProfile(alphanumericUserId);

        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(HttpStatus.OK, response3.getStatusCode());

        verify(userService, times(1)).getUserProfile(uuidUserId);
        verify(userService, times(1)).getUserProfile(numericUserId);
        verify(userService, times(1)).getUserProfile(alphanumericUserId);
    }
}