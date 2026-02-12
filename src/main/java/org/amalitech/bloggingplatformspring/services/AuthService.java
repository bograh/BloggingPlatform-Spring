package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserUtils userUtils;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthResponseDTO registerUser(RegisterUserDTO registerUserDTO) {
        String username = registerUserDTO.getUsername();
        String email = registerUserDTO.getEmail();
        String password = registerUserDTO.getPassword();

        if (password.toLowerCase().contains(username.toLowerCase())) {
            throw new BadRequestException("Password must not contain username");
        }

        if (Boolean.TRUE.equals(userRepository.existsByUsernameIgnoreCase(username))) {
            throw new BadRequestException("Username is taken");
        }

        if (Boolean.TRUE.equals(userRepository.existsByEmailIgnoreCase(email))) {
            throw new BadRequestException("Email is taken");
        }

        String hashedPassword = passwordEncoder.encode(password);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(hashedPassword);
        user.setUserRoles(List.of(UserRoles.READER));
        userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        return authenticateUser(user, authentication);
    }

    public AuthResponseDTO signInUser(SignInUserDTO signInUserDTO) {
        String email = signInUserDTO.getEmail();
        String password = signInUserDTO.getPassword();

        if (Boolean.FALSE.equals(userRepository.existsByEmailIgnoreCase(email))) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findUserByEmailIgnoreCase(email).orElseThrow(
                () -> new UnauthorizedException("Invalid email or password")
        );

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        return authenticateUser(user, authentication);
    }

    public AuthResponseDTO refreshAccessToken(String refreshTokenFromCookie) {
        if (refreshTokenFromCookie == null) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        if (!jwtTokenProvider.validRefreshToken(refreshTokenFromCookie)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String email = jwtTokenProvider.getEmailFromRefreshToken(refreshTokenFromCookie);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        User user = userRepository.findUserByEmailIgnoreCase(email).orElseThrow(
                () -> new UnauthorizedException("Invalid email or password")
        );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(email, null, userDetails.getAuthorities());

        return authenticateUser(user, authentication);
    }

    private AuthResponseDTO authenticateUser(User user, Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

        UserResponseDTO userResponseDTO = userUtils.mapUserToUserResponse(user);
        AuthResponse authResponse = new AuthResponse(userResponseDTO, accessToken);
        return new AuthResponseDTO(refreshToken, authResponse);
    }

}