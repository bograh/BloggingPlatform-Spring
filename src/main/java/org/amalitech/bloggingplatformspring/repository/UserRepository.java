package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.User;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User saveUser(String username, String email, String password) throws SQLException;

    User findUserByEmail(String email) throws SQLException;

    String getUsernameById(UUID id) throws SQLException;

    Optional<User> findUserById(UUID id) throws SQLException;

    Boolean userExistsByUsername(String username) throws SQLException;

    Boolean userExistsByEmail(String email) throws SQLException;

    Optional<User> findUserByUsername(String username) throws SQLException;
}