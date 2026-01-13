package org.amalitech.bloggingplatformspring.controllers;

import jakarta.validation.Valid;
import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.UserResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponse;
import org.amalitech.bloggingplatformspring.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> registerUser(@Valid @RequestBody RegisterUserDTO registerUserDTO) {
        UserResponseDTO userResponse = userService.registerUser(registerUserDTO);
        ApiResponse<UserResponseDTO> response = ApiResponse.success("User registration successful", userResponse);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<ApiResponse<UserResponseDTO>> signInUser(@RequestBody SignInUserDTO signInUserDTO) {
        UserResponseDTO userResponse = userService.signInUser();
        ApiResponse<UserResponseDTO> response = ApiResponse.success("User sign in successful", userResponse);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}