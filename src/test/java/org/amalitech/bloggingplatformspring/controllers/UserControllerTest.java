package org.amalitech.bloggingplatformspring.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
}