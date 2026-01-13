package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.User;

import java.sql.SQLException;

public interface UserRepository {
    User registerUser() throws SQLException;

}