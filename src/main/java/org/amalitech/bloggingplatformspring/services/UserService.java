package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.SQLQueryException;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

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

        if (password.toLowerCase().contains(username)) {
            throw new BadRequestException("Password must not contain username");
        }

        try {

            if (userRepository.userExistsByUsername(username)) {
                throw new BadRequestException("Username is taken");
            }

            if (userRepository.userExistsByEmail(email)) {
                throw new BadRequestException("Email is taken");
            }

            User user = userRepository.saveUser(username, email, password);
            return userUtils.mapUserToUserResponse(user);

        } catch (SQLException e) {
            throw new SQLQueryException("Failed to register user");
        }
    }

    public UserResponseDTO signInUser(SignInUserDTO signInUserDTO) {
        try {

            User user = userRepository.findUserByEmailAndPassword(signInUserDTO.getEmail(), signInUserDTO.getPassword());
            return userUtils.mapUserToUserResponse(user);

        } catch (SQLException e) {
            throw new SQLQueryException("Failed to sign user");
        }
    }
}