package org.amalitech.bloggingplatformspring.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final UUID userID = UUID.randomUUID();
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void registerUser_Success_Returns201() throws Exception {
        RegisterUserDTO request = new RegisterUserDTO(
                "John Doe",
                "john@example.com",
                "password123"
        );

        UserResponseDTO responseDTO = new UserResponseDTO(
                String.valueOf(userID),
                "John Doe",
                "john@example.com"
        );

        when(userService.registerUser(any(RegisterUserDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User registration successful"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }


    @Test
    void registerUser_InvalidInput_Returns400() throws Exception {
        RegisterUserDTO invalidRequest = new RegisterUserDTO(
                "", "invalid-email", "");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signInUser_Success_Returns200() throws Exception {
        SignInUserDTO request = new SignInUserDTO(
                "john@example.com",
                "password123"
        );

        UserResponseDTO responseDTO = new UserResponseDTO(
                String.valueOf(userID),
                "John Doe",
                "john@example.com"
        );

        when(userService.signInUser(request)).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/users/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User sign in successful"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }

    @Test
    void signInUser_InvalidInput_Returns400() throws Exception {
        SignInUserDTO invalidRequest = new SignInUserDTO(
                "", ""
        );

        mockMvc.perform(post("/api/v1/users/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}