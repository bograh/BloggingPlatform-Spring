package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.UnauthorizedException;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserUtils userUtils;

    @InjectMocks
    private UserService userService;

    private RegisterUserDTO registerUserDTO;
    private SignInUserDTO signInUserDTO;
    private User user;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setUsername("testuser");
        registerUserDTO.setEmail("test@example.com");
        registerUserDTO.setPassword("SecurePass123");

        signInUserDTO = new SignInUserDTO();
        signInUserDTO.setEmail("test@example.com");
        signInUserDTO.setPassword("SecurePass123");

        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("SecurePass123");

        userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(String.valueOf(UUID.randomUUID()));
        userResponseDTO.setUsername("testuser");
        userResponseDTO.setEmail("test@example.com");
    }


    @Test
    void registerUser_WithValidData_ShouldReturnUserResponseDTO() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userUtils.mapUserToUserResponse(any(User.class))).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.registerUser(registerUserDTO);

        assertNotNull(result);
        assertEquals(userResponseDTO.getId(), result.getId());
        assertEquals(userResponseDTO.getUsername(), result.getUsername());
        assertEquals(userResponseDTO.getEmail(), result.getEmail());

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(userUtils).mapUserToUserResponse(any(User.class));
    }

    @Test
    void registerUser_WithPasswordContainingUsername_ShouldThrowBadRequestException() {
        registerUserDTO.setPassword("testuser123");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userService.registerUser(registerUserDTO)
        );

        assertEquals("Password must not contain username", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_WithPasswordContainingUsernameCaseInsensitive_ShouldThrowBadRequestException() {
        registerUserDTO.setPassword("TESTUSER123");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userService.registerUser(registerUserDTO)
        );

        assertEquals("Password must not contain username", exception.getMessage());
    }

    @Test
    void registerUser_WithExistingUsername_ShouldThrowBadRequestException() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userService.registerUser(registerUserDTO)
        );

        assertEquals("Username is taken", exception.getMessage());
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_WithExistingEmail_ShouldThrowBadRequestException() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userService.registerUser(registerUserDTO)
        );

        assertEquals("Email is taken", exception.getMessage());
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void signInUser_WithValidCredentials_ShouldReturnUserResponseDTO() {
        when(userRepository.findUserByEmailAndPassword(anyString(), anyString()))
                .thenReturn(Optional.of(user));
        when(userUtils.mapUserToUserResponse(any(User.class))).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.signInUser(signInUserDTO);

        assertNotNull(result);
        assertEquals(userResponseDTO.getId(), result.getId());
        assertEquals(userResponseDTO.getUsername(), result.getUsername());
        assertEquals(userResponseDTO.getEmail(), result.getEmail());

        verify(userRepository).findUserByEmailAndPassword("test@example.com", "SecurePass123");
        verify(userUtils).mapUserToUserResponse(user);
    }

    @Test
    void signInUser_WithInvalidCredentials_ShouldThrowUnauthorizedException() {
        when(userRepository.findUserByEmailAndPassword(anyString(), anyString()))
                .thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> userService.signInUser(signInUserDTO)
        );

        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository).findUserByEmailAndPassword("test@example.com", "SecurePass123");
        verify(userUtils, never()).mapUserToUserResponse(any(User.class));
    }

    @Test
    void signInUser_WithNonExistentEmail_ShouldThrowUnauthorizedException() {
        signInUserDTO.setEmail("nonexistent@example.com");
        when(userRepository.findUserByEmailAndPassword(anyString(), anyString()))
                .thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> userService.signInUser(signInUserDTO)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void signInUser_WithWrongPassword_ShouldThrowUnauthorizedException() {
        signInUserDTO.setPassword("WrongPassword123");
        when(userRepository.findUserByEmailAndPassword(anyString(), anyString()))
                .thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> userService.signInUser(signInUserDTO)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }
}