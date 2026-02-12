package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Boolean existsByUsernameIgnoreCase(String username);

    Boolean existsByEmailIgnoreCase(String email);

    Optional<User> findUserByUsernameIgnoreCase(String username);

    Optional<User> findUserByEmailIgnoreCase(String email);

    @Query("""
                SELECT u FROM User u
                WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.email)  LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<User> search(@Param("query") String query, Pageable pageable);
}