package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.SQLQueryException;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private RegisterUserDTO registerUserDTO;
    private SignInUserDTO signInUserDTO;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword123");
        user.setCreatedAt(LocalDateTime.now());

        registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setUsername("testuser");
        registerUserDTO.setEmail("test@example.com");
        registerUserDTO.setPassword("SecurePass123!");

        signInUserDTO = new SignInUserDTO();
        signInUserDTO.setEmail("test@example.com");
        signInUserDTO.setPassword("SecurePass123!");

        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(String.valueOf(userId));
        userResponseDTO.setUsername("testuser");
        userResponseDTO.setEmail("test@example.com");
    }

    @Test
    void registerUser_Success() throws SQLException {
        when(userRepository.userExistsByUsername("testuser")).thenReturn(false);
        when(userRepository.userExistsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.saveUser("testuser", "test@example.com", "SecurePass123!"))
                .thenReturn(user);

        UserResponseDTO result = userService.registerUser(registerUserDTO);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).userExistsByUsername("testuser");
        verify(userRepository).userExistsByEmail("test@example.com");
        verify(userRepository).saveUser("testuser", "test@example.com", "SecurePass123!");
    }

    @Test
    void registerUser_PasswordContainsUsername_ThrowsBadRequestException() throws SQLException {
        registerUserDTO.setPassword("testuser123");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Password must not contain username", exception.getMessage());
        verify(userRepository, never()).userExistsByUsername(anyString());
        verify(userRepository, never()).userExistsByEmail(anyString());
        verify(userRepository, never()).saveUser(anyString(), anyString(), anyString());
    }

    @Test
    void registerUser_PasswordContainsUsernameCaseInsensitive_ThrowsBadRequestException() throws SQLException {
        registerUserDTO.setUsername("TestUser");
        registerUserDTO.setPassword("MyTestUser123!");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Password must not contain username", exception.getMessage());
        verify(userRepository, never()).userExistsByUsername(anyString());
    }

    @Test
    void registerUser_UsernameAlreadyExists_ThrowsBadRequestException() throws SQLException {
        when(userRepository.userExistsByUsername("testuser")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Username is taken", exception.getMessage());
        verify(userRepository).userExistsByUsername("testuser");
        verify(userRepository, never()).userExistsByEmail(anyString());
        verify(userRepository, never()).saveUser(anyString(), anyString(), anyString());
    }

    @Test
    void registerUser_EmailAlreadyExists_ThrowsBadRequestException() throws SQLException {
        when(userRepository.userExistsByUsername("testuser")).thenReturn(false);
        when(userRepository.userExistsByEmail("test@example.com")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Email is taken", exception.getMessage());
        verify(userRepository).userExistsByUsername("testuser");
        verify(userRepository).userExistsByEmail("test@example.com");
        verify(userRepository, never()).saveUser(anyString(), anyString(), anyString());
    }

    @Test
    void registerUser_UsernameCheckThrowsSQLException_ThrowsSQLQueryException() throws SQLException {
        when(userRepository.userExistsByUsername("testuser"))
                .thenThrow(new SQLException("Database connection error"));

        SQLQueryException exception = assertThrows(SQLQueryException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Failed to register user", exception.getMessage());
        verify(userRepository).userExistsByUsername("testuser");
        verify(userRepository, never()).saveUser(anyString(), anyString(), anyString());
    }

    @Test
    void registerUser_EmailCheckThrowsSQLException_ThrowsSQLQueryException() throws SQLException {
        when(userRepository.userExistsByUsername("testuser")).thenReturn(false);
        when(userRepository.userExistsByEmail("test@example.com"))
                .thenThrow(new SQLException("Database connection error"));

        SQLQueryException exception = assertThrows(SQLQueryException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Failed to register user", exception.getMessage());
        verify(userRepository).userExistsByUsername("testuser");
        verify(userRepository).userExistsByEmail("test@example.com");
        verify(userRepository, never()).saveUser(anyString(), anyString(), anyString());
    }

    @Test
    void registerUser_SaveUserThrowsSQLException_ThrowsSQLQueryException() throws SQLException {
        when(userRepository.userExistsByUsername("testuser")).thenReturn(false);
        when(userRepository.userExistsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.saveUser("testuser", "test@example.com", "SecurePass123!"))
                .thenThrow(new SQLException("Insert failed"));

        SQLQueryException exception = assertThrows(SQLQueryException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Failed to register user", exception.getMessage());
        verify(userRepository).saveUser("testuser", "test@example.com", "SecurePass123!");
    }

    @Test
    void registerUser_WithValidPasswordNotContainingUsername_Success() throws SQLException {
        registerUserDTO.setUsername("john");
        registerUserDTO.setPassword("MySecurePassword123!");

        when(userRepository.userExistsByUsername("john")).thenReturn(false);
        when(userRepository.userExistsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.saveUser("john", "test@example.com", "MySecurePassword123!"))
                .thenReturn(user);

        UserResponseDTO result = userService.registerUser(registerUserDTO);

        assertNotNull(result);
        verify(userRepository).saveUser("john", "test@example.com", "MySecurePassword123!");
    }

    @Test
    void registerUser_PasswordContainsUsernameInMiddle_ThrowsBadRequestException() {
        registerUserDTO.setUsername("user");
        registerUserDTO.setPassword("Myuser123!");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Password must not contain username", exception.getMessage());
    }

    @Test
    void signInUser_Success() throws SQLException {
        when(userRepository.findUserByEmailAndPassword("test@example.com", "SecurePass123!"))
                .thenReturn(user);

        UserResponseDTO result = userService.signInUser(signInUserDTO);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).findUserByEmailAndPassword("test@example.com", "SecurePass123!");
    }

    @Test
    void signInUser_InvalidCredentials_ThrowsException() throws SQLException {
        when(userRepository.findUserByEmailAndPassword("test@example.com", "WrongPassword"))
                .thenThrow(new SQLException("User not found"));

        signInUserDTO.setPassword("WrongPassword");

        SQLQueryException exception = assertThrows(SQLQueryException.class,
                () -> userService.signInUser(signInUserDTO));

        assertEquals("Failed to sign user", exception.getMessage());
        verify(userRepository).findUserByEmailAndPassword("test@example.com", "WrongPassword");
    }

    @Test
    void signInUser_DatabaseError_ThrowsSQLQueryException() throws SQLException {
        when(userRepository.findUserByEmailAndPassword("test@example.com", "SecurePass123!"))
                .thenThrow(new SQLException("Database connection error"));

        SQLQueryException exception = assertThrows(SQLQueryException.class,
                () -> userService.signInUser(signInUserDTO));

        assertEquals("Failed to sign user", exception.getMessage());
        verify(userRepository).findUserByEmailAndPassword("test@example.com", "SecurePass123!");
    }

    @Test
    void signInUser_WithDifferentEmail_Success() throws SQLException {
        signInUserDTO.setEmail("different@example.com");
        User differentUser = new User();
        differentUser.setId(UUID.randomUUID());
        differentUser.setUsername("differentuser");
        differentUser.setEmail("different@example.com");

        when(userRepository.findUserByEmailAndPassword("different@example.com", "SecurePass123!"))
                .thenReturn(differentUser);

        UserResponseDTO result = userService.signInUser(signInUserDTO);

        assertNotNull(result);
        verify(userRepository).findUserByEmailAndPassword("different@example.com", "SecurePass123!");
    }

    @Test
    void signInUser_NullReturnFromRepository_HandledProperly() throws SQLException {
        when(userRepository.findUserByEmailAndPassword("test@example.com", "SecurePass123!"))
                .thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> userService.signInUser(signInUserDTO));

        verify(userRepository).findUserByEmailAndPassword("test@example.com", "SecurePass123!");
    }

    @Test
    void registerUser_UsernameWithSpecialCharacters_PasswordValidation() throws SQLException {
        registerUserDTO.setUsername("test_user-123");
        registerUserDTO.setPassword("test_user-123456");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Password must not contain username", exception.getMessage());
    }

    @Test
    void registerUser_EmptyPassword_StillChecksUsername() {
        registerUserDTO.setPassword("");

        assertDoesNotThrow(() -> {
            try {
                when(userRepository.userExistsByUsername("testuser")).thenReturn(false);
                when(userRepository.userExistsByEmail("test@example.com")).thenReturn(false);
                when(userRepository.saveUser("testuser", "test@example.com", ""))
                        .thenReturn(user);
                userService.registerUser(registerUserDTO);
            } catch (SQLException e) {
                fail("Should not throw SQLException in this test");
            }
        });
    }

    @Test
    void registerUser_UpperCaseUsername_LowerCaseInPassword_ThrowsBadRequestException() {
        registerUserDTO.setUsername("TESTUSER");
        registerUserDTO.setPassword("mytestuser123");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Password must not contain username", exception.getMessage());
    }

}