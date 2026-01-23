package org.amalitech.bloggingplatformspring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponseGeneric;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.exceptions.ErrorResponse;
import org.amalitech.bloggingplatformspring.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "1. User Management", description = "APIs for user registration and authentication")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided registration details. Returns the created user profile.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or user already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseGeneric<UserResponseDTO>> registerUser(
            @Valid @RequestBody RegisterUserDTO registerUserDTO) {
        UserResponseDTO userResponse = userService.registerUser(registerUserDTO);
        ApiResponseGeneric<UserResponseDTO> response = ApiResponseGeneric.success("User registration successful",
                userResponse);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/sign-in")
    @Operation(
            summary = "Sign in a user",
            description = "Authenticates a user with email and password. Returns the user profile upon successful authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "User successfully signed in",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
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

    public ResponseEntity<ApiResponseGeneric<UserResponseDTO>> signInUser(
            @Valid @RequestBody SignInUserDTO signInUserDTO) {
        UserResponseDTO userResponse = userService.signInUser(signInUserDTO);
        ApiResponseGeneric<UserResponseDTO> response = ApiResponseGeneric.success("User sign in successful",
                userResponse);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}