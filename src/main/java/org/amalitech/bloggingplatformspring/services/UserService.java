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
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserUtils userUtils;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.userUtils = new UserUtils();
    }

    public UserResponseDTO registerUser(RegisterUserDTO registerUserDTO) {
        String username = registerUserDTO.getUsername();
        String email = registerUserDTO.getEmail();
        String password = registerUserDTO.getPassword();

        if (password.toLowerCase().contains(username.toLowerCase())) {
            throw new BadRequestException("Password must not contain username");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BadRequestException("Username is taken");
        }

        if (userRepository.existsByUsernameIgnoreCase(email)) {
            throw new BadRequestException("Email is taken");
        }

        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(hashedPassword);
        userRepository.save(user);

        return userUtils.mapUserToUserResponse(user);
    }

    public UserResponseDTO signInUser(SignInUserDTO signInUserDTO) {
        String email = signInUserDTO.getEmail();
        String password = signInUserDTO.getPassword();

        if (!userRepository.existsByEmailIgnoreCase(email)) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findUserByEmailIgnoreCase(email).orElseThrow(
                () -> new UnauthorizedException("Invalid email or password")
        );

        BCrypt.Result hashedPassword = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());

        if (!hashedPassword.verified) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return userUtils.mapUserToUserResponse(user);

    }
}