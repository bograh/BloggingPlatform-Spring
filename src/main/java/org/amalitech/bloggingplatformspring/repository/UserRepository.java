package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.User;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User saveUser(String username, String email, String password) throws SQLException;

    User findUserByEmailAndPassword(String email, String password) throws SQLException;

    String getUsernameById(UUID id) throws SQLException;

    Optional<User> findUserById(UUID id) throws SQLException;

    Optional<User> findUserByUsername(String username) throws SQLException;
}