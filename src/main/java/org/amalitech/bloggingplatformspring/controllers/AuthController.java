package org.amalitech.bloggingplatformspring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponseGeneric;
import org.amalitech.bloggingplatformspring.dtos.responses.AuthResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.AuthResponseDTO;
import org.amalitech.bloggingplatformspring.exceptions.ErrorResponse;
import org.amalitech.bloggingplatformspring.security.RefreshCookieService;
import org.amalitech.bloggingplatformspring.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "1. Authentication", description = "APIs for user registration and authentication")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieService refreshCookieService;

    public AuthController(AuthService authService, RefreshCookieService refreshCookieService) {
        this.authService = authService;
        this.refreshCookieService = refreshCookieService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided registration details. Returns the created user profile.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or user already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseGeneric<AuthResponse>> registerUser(
            @Valid @RequestBody RegisterUserDTO registerUserDTO,
            HttpServletResponse httpServletResponse) {
        AuthResponseDTO authResponseDTO = authService.registerUser(registerUserDTO);
        AuthResponse authResponse = authResponseDTO.getAuthResponse();
        refreshCookieService.setRefreshTokenCookie(authResponseDTO.getRefreshToken(), httpServletResponse);
        ApiResponseGeneric<AuthResponse> apiResponse =
                ApiResponseGeneric.success("User registration successful", authResponse);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @PostMapping("/sign-in")
    @Operation(
            summary = "Sign in a user",
            description = "Authenticates a user with email and password. Returns the user profile upon successful authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "User successfully signed in",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ApiResponseGeneric<AuthResponse>> signInUser(
            @Valid @RequestBody SignInUserDTO signInUserDTO,
            HttpServletResponse httpServletResponse) {
        AuthResponseDTO authResponseDTO = authService.signInUser(signInUserDTO);
        AuthResponse authResponse = authResponseDTO.getAuthResponse();
        refreshCookieService.setRefreshTokenCookie(authResponseDTO.getRefreshToken(), httpServletResponse);
        ApiResponseGeneric<AuthResponse> apiResponse =
                ApiResponseGeneric.success("User sign in successful", authResponse);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping("/refresh-toke}")
    @Operation(
            summary = "Refresh Access Token",
            description = "Returns new access token."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Access token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            )
    })
    public ResponseEntity<ApiResponseGeneric<AuthResponse>> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        AuthResponseDTO authResponseDTO = authService.refreshAccessToken(refreshToken);
        AuthResponse authResponse = authResponseDTO.getAuthResponse();
        refreshCookieService.setRefreshTokenCookie(authResponseDTO.getRefreshToken(), response);
        ApiResponseGeneric<AuthResponse> apiResponse =
                ApiResponseGeneric.success("Access token refreshed successfully", authResponse);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}