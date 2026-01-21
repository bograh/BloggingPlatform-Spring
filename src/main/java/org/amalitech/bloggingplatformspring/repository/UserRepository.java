package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    User findUserByEmailAndPassword(String email, String password);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    Optional<User> findUserByUsername(String username);
}