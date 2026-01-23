package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.UnauthorizedException;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserUtils userUtils;

    public UserService(UserRepository userRepository, UserUtils userUtils) {
        this.userRepository = userRepository;
        this.userUtils = userUtils;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UserResponseDTO registerUser(RegisterUserDTO registerUserDTO) {
        String username = registerUserDTO.getUsername();
        String email = registerUserDTO.getEmail();
        String password = registerUserDTO.getPassword();

        if (password.toLowerCase().contains(username.toLowerCase())) {
            throw new BadRequestException("Password must not contain username");
        }

        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username is taken");
        }

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is taken");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);

        userRepository.save(user);

        return userUtils.mapUserToUserResponse(user);
    }

    public UserResponseDTO signInUser(SignInUserDTO signInUserDTO) {
        String email = signInUserDTO.getEmail();
        String password = signInUserDTO.getPassword();

        User user = userRepository.findUserByEmailAndPassword(email, password).orElseThrow(
                () -> new UnauthorizedException("Invalid email or password")
        );

        return userUtils.mapUserToUserResponse(user);
    }
}