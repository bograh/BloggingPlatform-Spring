package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.dtos.requests.UserResponseDTO;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.UserRegistrationException;
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

    public UserResponseDTO registerUser() {
        try {
            User user = userRepository.registerUser();
            return userUtils.mapUserToUserResponse(user);

        } catch (SQLException e) {
            throw new UserRegistrationException("Failed to register user");
        }
    }

}