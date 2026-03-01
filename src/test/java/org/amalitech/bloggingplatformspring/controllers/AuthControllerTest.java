package org.amalitech.bloggingplatformspring.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.AuthResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.AuthResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.security.JwtTokenProvider;
import org.amalitech.bloggingplatformspring.security.RefreshCookieService;
import org.amalitech.bloggingplatformspring.security.TokenSessionService;
import org.amalitech.bloggingplatformspring.services.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshCookieService refreshCookieService;

    @MockitoBean
    private TokenSessionService tokenSessionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // Mock security components
    @MockitoBean
    private org.amalitech.bloggingplatformspring.services.CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private org.amalitech.bloggingplatformspring.services.SecurityAuditService securityAuditService;

    @Test
    void registerUser_shouldReturnCreatedStatus_whenValidData() throws Exception {
        RegisterUserDTO registerDTO = new RegisterUserDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setEmail("test@example.com");
        registerDTO.setPassword("Password123!");

        UserResponseDTO userResponse = new UserResponseDTO();
        userResponse.setId(UUID.randomUUID().toString());
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@example.com");

        String accessToken = "access-token-123";
        String refreshToken = "refresh-token-123";

        AuthResponse authResponse = new AuthResponse(userResponse, accessToken);
        AuthResponseDTO authResponseDTO = new AuthResponseDTO(refreshToken, authResponse);

        when(authService.registerUser(any(RegisterUserDTO.class))).thenReturn(authResponseDTO);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User registration successful"))
                .andExpect(jsonPath("$.data.user.username").value("testuser"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.accessToken").value(accessToken));

        verify(authService).registerUser(any(RegisterUserDTO.class));
        verify(tokenSessionService).createSession(eq("test@example.com"), eq(accessToken), eq(refreshToken));
        verify(refreshCookieService).setRefreshTokenCookie(eq(refreshToken), any());
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenInvalidData() throws Exception {
        RegisterUserDTO registerDTO = new RegisterUserDTO();
        registerDTO.setUsername("");
        registerDTO.setEmail("invalid-email");
        registerDTO.setPassword("weak");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerUser(any(RegisterUserDTO.class));
    }

    @Test
    void signInUser_shouldReturnOkStatus_whenValidCredentials() throws Exception {
        SignInUserDTO signInDTO = new SignInUserDTO();
        signInDTO.setEmail("test@example.com");
        signInDTO.setPassword("Password123!");

        UserResponseDTO userResponse = new UserResponseDTO();
        userResponse.setId(UUID.randomUUID().toString());
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@example.com");

        String accessToken = "access-token-123";
        String refreshToken = "refresh-token-123";

        AuthResponse authResponse = new AuthResponse(userResponse, accessToken);
        AuthResponseDTO authResponseDTO = new AuthResponseDTO(refreshToken, authResponse);

        when(authService.signInUser(any(SignInUserDTO.class))).thenReturn(authResponseDTO);

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User sign in successful"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.accessToken").value(accessToken));

        verify(authService).signInUser(any(SignInUserDTO.class));
        verify(tokenSessionService).createSession(eq("test@example.com"), eq(accessToken), eq(refreshToken));
        verify(refreshCookieService).setRefreshTokenCookie(eq(refreshToken), any());
    }

    @Test
    void signInUser_shouldReturnBadRequest_whenInvalidData() throws Exception {
        SignInUserDTO signInDTO = new SignInUserDTO();
        signInDTO.setEmail("");
        signInDTO.setPassword("");

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInDTO)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).signInUser(any(SignInUserDTO.class));
    }

    @Test
    void refreshAccessToken_shouldReturnNewToken_whenValidRefreshToken() throws Exception {
        String refreshToken = "valid-refresh-token";

        UserResponseDTO userResponse = new UserResponseDTO();
        userResponse.setId(UUID.randomUUID().toString());
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@example.com");

        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        AuthResponse authResponse = new AuthResponse(userResponse, newAccessToken);
        AuthResponseDTO authResponseDTO = new AuthResponseDTO(newRefreshToken, authResponse);

        when(tokenSessionService.isTokenRevoked(refreshToken)).thenReturn(false);
        when(authService.refreshAccessToken(refreshToken)).thenReturn(authResponseDTO);

        mockMvc.perform(post("/api/auth/refresh-token")
                        .cookie(new Cookie("refreshToken", refreshToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Access token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").value(newAccessToken));

        verify(tokenSessionService).isTokenRevoked(refreshToken);
        verify(authService).refreshAccessToken(refreshToken);
        verify(tokenSessionService).createSession(eq("test@example.com"), eq(newAccessToken), eq(newRefreshToken));
        verify(refreshCookieService).setRefreshTokenCookie(eq(newRefreshToken), any());
    }

    @Test
    void refreshAccessToken_shouldThrowUnauthorized_whenTokenRevoked() throws Exception {
        String revokedToken = "revoked-token";

        when(tokenSessionService.isTokenRevoked(revokedToken)).thenReturn(true);

        mockMvc.perform(post("/api/auth/refresh-token")
                        .cookie(new Cookie("refreshToken", revokedToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(tokenSessionService).isTokenRevoked(revokedToken);
        verify(authService, never()).refreshAccessToken(anyString());
    }

    @Test
    void signOutUser_shouldRevokeTokensAndClearCookie_whenValidTokens() throws Exception {
        String accessToken = "valid-access-token";
        String refreshToken = "valid-refresh-token";
        String email = "test@example.com";

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(jwtTokenProvider.parseAccessToken(accessToken)).thenReturn(claims);

        mockMvc.perform(post("/api/auth/sign-out")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new Cookie("refreshToken", refreshToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message")
                        .value("User signed out successfully"));

        verify(jwtTokenProvider).parseAccessToken(accessToken);
        verify(tokenSessionService).removeSession(email);
        verify(tokenSessionService).revokeToken(accessToken);
        verify(tokenSessionService).revokeToken(refreshToken);
        verify(refreshCookieService).clearRefreshTokenCookie(any());
    }

    @Test
    void signOutUser_shouldStillWork_whenNoTokensProvided() throws Exception {

        mockMvc.perform(post("/api/auth/sign-out")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message")
                        .value("User signed out successfully"));

        verify(jwtTokenProvider, never()).parseAccessToken(anyString());
        verify(tokenSessionService, never()).removeSession(anyString());
        verify(tokenSessionService, never()).revokeToken(anyString());
        verify(tokenSessionService, never()).revokeToken(anyString());
        verify(refreshCookieService).clearRefreshTokenCookie(any());
    }

    @Test
    void signOutUser_shouldRevokeOnlyRefreshToken_whenNoAccessToken() throws Exception {
        String refreshToken = "valid-refresh-token";
        mockMvc.perform(post("/api/auth/sign-out")
                        .cookie(new Cookie("refreshToken", refreshToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(jwtTokenProvider, never()).parseAccessToken(anyString());
        verify(tokenSessionService, never()).removeSession(anyString());
        verify(tokenSessionService, never()).revokeToken(anyString());
        verify(tokenSessionService).revokeToken(refreshToken);
        verify(refreshCookieService).clearRefreshTokenCookie(any());
    }
}