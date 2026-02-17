package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.AuthResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.AuthResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.enums.UserRoles;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.UnauthorizedException;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.security.JwtTokenProvider;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private CustomUserDetailsService customUserDetailsService;

  @Mock
  private UserUtils userUtils;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private AuthenticationManager authenticationManager;

  @InjectMocks
  private AuthService authService;

  private RegisterUserDTO validRegisterDTO;
  private SignInUserDTO validSignInDTO;
  private User mockUser;
  private UserResponseDTO mockUserResponse;
  private Authentication mockAuthentication;

  @BeforeEach
  void setUp() {
    validRegisterDTO = new RegisterUserDTO();
    validRegisterDTO.setUsername("testuser");
    validRegisterDTO.setEmail("test@example.com");
    validRegisterDTO.setPassword("Password123!");

    validSignInDTO = new SignInUserDTO();
    validSignInDTO.setEmail("test@example.com");
    validSignInDTO.setPassword("Password123!");

    mockUser = new User();
    mockUser.setId(UUID.randomUUID());
    mockUser.setUsername("testuser");
    mockUser.setEmail("test@example.com");
    mockUser.setPassword("hashed-password");
    mockUser.setUserRoles(List.of(UserRoles.READER, UserRoles.AUTHOR));

    mockUserResponse = new UserResponseDTO();
    mockUserResponse.setId(mockUser.getId().toString());
    mockUserResponse.setUsername("testuser");
    mockUserResponse.setEmail("test@example.com");

    mockAuthentication = new UsernamePasswordAuthenticationToken(
        "test@example.com",
        "Password123!",
        List.of(new SimpleGrantedAuthority("ROLE_READER")));
  }

  @Test
  void registerUser_shouldRegisterSuccessfully_whenValidData() {
    when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
    when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode("Password123!")).thenReturn("hashed-password");
    when(userRepository.save(any(User.class))).thenReturn(mockUser);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(mockAuthentication);
    when(jwtTokenProvider.createAccessToken(mockAuthentication)).thenReturn("access-token");
    when(jwtTokenProvider.createRefreshToken(mockAuthentication)).thenReturn("refresh-token");
    when(userUtils.mapUserToUserResponse(mockUser)).thenReturn(mockUserResponse);

    AuthResponseDTO result = authService.registerUser(validRegisterDTO);

    assertNotNull(result);
    assertNotNull(result.getAuthResponse());
    assertNotNull(result.getRefreshToken());
    assertEquals("access-token", result.getAuthResponse().accessToken());
    assertEquals("refresh-token", result.getRefreshToken());
    assertEquals("testuser", result.getAuthResponse().user().getUsername());

    verify(userRepository).existsByUsernameIgnoreCase("testuser");
    verify(userRepository).existsByEmailIgnoreCase("test@example.com");
    verify(passwordEncoder).encode("Password123!");
    verify(userRepository).save(any(User.class));
    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  void registerUser_shouldThrowBadRequestException_whenUsernameExists() {
    when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(true);

    BadRequestException exception = assertThrows(
        BadRequestException.class,
        () -> authService.registerUser(validRegisterDTO));

    assertEquals("Username is taken", exception.getMessage());
    verify(userRepository).existsByUsernameIgnoreCase("testuser");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void registerUser_shouldThrowBadRequestException_whenEmailExists() {
    when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
    when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);

    BadRequestException exception = assertThrows(
        BadRequestException.class,
        () -> authService.registerUser(validRegisterDTO));

    assertEquals("Email is taken", exception.getMessage());
    verify(userRepository).existsByEmailIgnoreCase("test@example.com");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void registerUser_shouldThrowBadRequestException_whenPasswordContainsUsername() {
    validRegisterDTO.setPassword("testuser123");

    BadRequestException exception = assertThrows(
        BadRequestException.class,
        () -> authService.registerUser(validRegisterDTO));

    assertEquals("Password must not contain username", exception.getMessage());
    verify(userRepository, never()).existsByUsernameIgnoreCase(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void registerUser_shouldTrimAndLowercaseEmailAndUsername() {
    validRegisterDTO.setUsername("  TestUser  ");
    validRegisterDTO.setEmail("  Test@Example.COM  ");

    when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
    when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
    when(userRepository.save(any(User.class))).thenReturn(mockUser);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(mockAuthentication);
    when(jwtTokenProvider.createAccessToken(any())).thenReturn("access-token");
    when(jwtTokenProvider.createRefreshToken(any())).thenReturn("refresh-token");
    when(userUtils.mapUserToUserResponse(any())).thenReturn(mockUserResponse);

    authService.registerUser(validRegisterDTO);

    verify(userRepository).existsByUsernameIgnoreCase("testuser");
    verify(userRepository).existsByEmailIgnoreCase("test@example.com");
  }

  @Test
  void signInUser_shouldSignInSuccessfully_whenValidCredentials() {
    when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);
    when(userRepository.findUserByEmailIgnoreCase("test@example.com"))
        .thenReturn(Optional.of(mockUser));
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(mockAuthentication);
    when(jwtTokenProvider.createAccessToken(mockAuthentication)).thenReturn("access-token");
    when(jwtTokenProvider.createRefreshToken(mockAuthentication)).thenReturn("refresh-token");
    when(userUtils.mapUserToUserResponse(mockUser)).thenReturn(mockUserResponse);

    AuthResponseDTO result = authService.signInUser(validSignInDTO);

    assertNotNull(result);
    assertNotNull(result.getAuthResponse());
    assertEquals("access-token", result.getAuthResponse().accessToken());
    assertEquals("refresh-token", result.getRefreshToken());

    verify(userRepository).existsByEmailIgnoreCase("test@example.com");
    verify(userRepository).findUserByEmailIgnoreCase("test@example.com");
    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  void signInUser_shouldThrowUnauthorizedException_whenEmailDoesNotExist() {
    when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);

    UnauthorizedException exception = assertThrows(
        UnauthorizedException.class,
        () -> authService.signInUser(validSignInDTO));

    assertEquals("Invalid email or password", exception.getMessage());
    verify(userRepository).existsByEmailIgnoreCase("test@example.com");
    verify(authenticationManager, never()).authenticate(any());
  }

  @Test
  void signInUser_shouldThrowUnauthorizedException_whenWrongPassword() {
    when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);
    when(userRepository.findUserByEmailIgnoreCase("test@example.com"))
        .thenReturn(Optional.of(mockUser));
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    UnauthorizedException exception = assertThrows(
        UnauthorizedException.class,
        () -> authService.signInUser(validSignInDTO));

    assertEquals("Invalid email or password", exception.getMessage());
    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  void signInUser_shouldTrimAndLowercaseEmail() {
    validSignInDTO.setEmail("  Test@Example.COM  ");

    when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);
    when(userRepository.findUserByEmailIgnoreCase("test@example.com"))
        .thenReturn(Optional.of(mockUser));
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(mockAuthentication);
    when(jwtTokenProvider.createAccessToken(any())).thenReturn("access-token");
    when(jwtTokenProvider.createRefreshToken(any())).thenReturn("refresh-token");
    when(userUtils.mapUserToUserResponse(any())).thenReturn(mockUserResponse);

    authService.signInUser(validSignInDTO);

    verify(userRepository).existsByEmailIgnoreCase("test@example.com");
  }

  @Test
  void refreshAccessToken_shouldReturnNewTokens_whenValidRefreshToken() {
    String refreshToken = "valid-refresh-token";
    String email = "test@example.com";

    UserDetails mockUserDetails = org.springframework.security.core.userdetails.User
        .withUsername(email)
        .password("password")
        .authorities("ROLE_READER")
        .build();

    when(jwtTokenProvider.validRefreshToken(refreshToken)).thenReturn(true);
    when(jwtTokenProvider.getEmailFromRefreshToken(refreshToken)).thenReturn(email);
    when(customUserDetailsService.loadUserByUsername(email)).thenReturn(mockUserDetails);
    when(userRepository.findUserByEmailIgnoreCase(email)).thenReturn(Optional.of(mockUser));
    when(jwtTokenProvider.createAccessToken(any(Authentication.class))).thenReturn("new-access-token");
    when(jwtTokenProvider.createRefreshToken(any(Authentication.class))).thenReturn("new-refresh-token");
    when(userUtils.mapUserToUserResponse(mockUser)).thenReturn(mockUserResponse);

    AuthResponseDTO result = authService.refreshAccessToken(refreshToken);

    assertNotNull(result);
    assertEquals("new-access-token", result.getAuthResponse().accessToken());
    assertEquals("new-refresh-token", result.getRefreshToken());

    verify(jwtTokenProvider).validRefreshToken(refreshToken);
    verify(jwtTokenProvider).getEmailFromRefreshToken(refreshToken);
    verify(customUserDetailsService).loadUserByUsername(email);
  }

  @Test
  void refreshAccessToken_shouldThrowUnauthorizedException_whenNullToken() {
    UnauthorizedException exception = assertThrows(
        UnauthorizedException.class,
        () -> authService.refreshAccessToken(null));

    assertEquals("Invalid refresh token", exception.getMessage());
    verify(jwtTokenProvider, never()).validRefreshToken(anyString());
  }

  @Test
  void refreshAccessToken_shouldThrowUnauthorizedException_whenInvalidToken() {
    String invalidToken = "invalid-token";

    when(jwtTokenProvider.validRefreshToken(invalidToken)).thenReturn(false);

    UnauthorizedException exception = assertThrows(
        UnauthorizedException.class,
        () -> authService.refreshAccessToken(invalidToken));

    assertEquals("Invalid refresh token", exception.getMessage());
    verify(jwtTokenProvider).validRefreshToken(invalidToken);
    verify(jwtTokenProvider, never()).getEmailFromRefreshToken(anyString());
  }

  @Test
  void refreshAccessToken_shouldThrowUnauthorizedException_whenUserNotFound() {
    String refreshToken = "valid-refresh-token";
    String email = "test@example.com";

    UserDetails mockUserDetails = org.springframework.security.core.userdetails.User
        .withUsername(email)
        .password("password")
        .authorities("ROLE_READER")
        .build();

    when(jwtTokenProvider.validRefreshToken(refreshToken)).thenReturn(true);
    when(jwtTokenProvider.getEmailFromRefreshToken(refreshToken)).thenReturn(email);
    when(customUserDetailsService.loadUserByUsername(email)).thenReturn(mockUserDetails);
    when(userRepository.findUserByEmailIgnoreCase(email)).thenReturn(Optional.empty());

    UnauthorizedException exception = assertThrows(
        UnauthorizedException.class,
        () -> authService.refreshAccessToken(refreshToken));

    assertEquals("Invalid email or password", exception.getMessage());
    verify(userRepository).findUserByEmailIgnoreCase(email);
  }
}
