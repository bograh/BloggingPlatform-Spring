package org.amalitech.bloggingplatformspring.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
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
        registerUserDTO.setPassword("SecurePass123!");

        signInUserDTO = new SignInUserDTO();
        signInUserDTO.setEmail("test@example.com");
        signInUserDTO.setPassword("SecurePass123!");
        UUID userID = UUID.randomUUID();
        user = new User();
        user.setId(userID);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword(BCrypt.withDefaults().hashToString(12, "SecurePass123!".toCharArray()));

        userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(String.valueOf(userID));
        userResponseDTO.setUsername("testuser");
        userResponseDTO.setEmail("test@example.com");
    }

    @Test
    void registerUser_Success() {
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userUtils.mapUserToUserResponse(any(User.class))).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.registerUser(registerUserDTO);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
        verify(userUtils).mapUserToUserResponse(any(User.class));
    }

    @Test
    void registerUser_PasswordContainsUsername_ThrowsException() {
        registerUserDTO.setPassword("testuser123");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Password must not contain username", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_UsernameTaken_ThrowsException() {
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Username is taken", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_EmailTaken_ThrowsException() {
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Email is taken", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_PasswordContainsUsernameUpperCase_ThrowsException() {
        registerUserDTO.setPassword("TESTUSER123");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Password must not contain username", exception.getMessage());
    }

    @Test
    void signInUser_Success() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);
        when(userRepository.findUserByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));
        when(userUtils.mapUserToUserResponse(any(User.class))).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.signInUser(signInUserDTO);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).findUserByEmailIgnoreCase(anyString());
        verify(userUtils).mapUserToUserResponse(any(User.class));
    }

    @Test
    void signInUser_EmailNotExists_ThrowsException() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> userService.signInUser(signInUserDTO));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository, never()).findUserByEmailIgnoreCase(anyString());
    }

    @Test
    void signInUser_UserNotFound_ThrowsException() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);
        when(userRepository.findUserByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> userService.signInUser(signInUserDTO));

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void signInUser_InvalidPassword_ThrowsException() {
        signInUserDTO.setPassword("WrongPassword123!");

        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);
        when(userRepository.findUserByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> userService.signInUser(signInUserDTO));

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void registerUser_SavedUserHasHashedPassword() {
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertNotEquals(registerUserDTO.getPassword(), savedUser.getPassword());
            assertTrue(savedUser.getPassword().startsWith("$2a$"));
            return savedUser;
        });
        when(userUtils.mapUserToUserResponse(any(User.class))).thenReturn(userResponseDTO);

        userService.registerUser(registerUserDTO);

        verify(userRepository).save(any(User.class));
    }
}