package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.User;

import java.sql.SQLException;

public interface UserRepository {
    User saveUser(String username, String email, String password) throws SQLException;

    User getUserByEmailAndPassword(String email, String password) throws SQLException;
}