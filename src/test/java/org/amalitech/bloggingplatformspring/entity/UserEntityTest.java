package org.amalitech.bloggingplatformspring.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class UserEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password123");
    }

    @Test
    void testUserEntity_shouldPersistSuccessfully() {
        User savedUser = entityManager.persistAndFlush(user);

        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("password123", savedUser.getPassword());
        assertNotNull(savedUser.getCreatedAt());
    }

    @Test
    void testUserEntity_shouldGenerateUUID() {
        User savedUser = entityManager.persistAndFlush(user);

        assertNotNull(savedUser.getId());
        assertInstanceOf(UUID.class, savedUser.getId());
    }

    @Test
    void testUserEntity_shouldSetCreatedAtOnPersist() {
        LocalDateTime before = LocalDateTime.now();

        User savedUser = entityManager.persistAndFlush(user);

        LocalDateTime after = LocalDateTime.now();
        assertNotNull(savedUser.getCreatedAt());
        assertTrue(savedUser.getCreatedAt().isAfter(before.minusSeconds(1)));
        assertTrue(savedUser.getCreatedAt().isBefore(after.plusSeconds(1)));
    }

    @Test
    void testUserEntity_shouldEnforceUniqueUsername() {
        User user1 = new User();
        user1.setUsername("duplicate");
        user1.setEmail("user1@example.com");
        user1.setPassword("password123");

        User user2 = new User();
        user2.setUsername("duplicate");
        user2.setEmail("user2@example.com");
        user2.setPassword("password456");

        entityManager.persistAndFlush(user1);

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(user2);
        });
    }

    @Test
    void testUserEntity_shouldEnforceUniqueEmail() {
        User user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("duplicate@example.com");
        user1.setPassword("password123");

        User user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("duplicate@example.com");
        user2.setPassword("password456");

        entityManager.persistAndFlush(user1);

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(user2);
        });
    }

    @Test
    void testUserEntity_shouldNotAllowNullUsername() {
        user.setUsername(null);

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(user);
        });
    }

    @Test
    void testUserEntity_shouldNotAllowNullEmail() {
        user.setEmail(null);

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(user);
        });
    }

    @Test
    void testUserEntity_shouldNotAllowNullPassword() {
        user.setPassword(null);

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(user);
        });
    }

    @Test
    void testUserEntity_shouldInitializePostsAsEmptySet() {
        User savedUser = entityManager.persistAndFlush(user);

        assertNotNull(savedUser.getPosts());
        assertTrue(savedUser.getPosts().isEmpty());
    }

    @Test
    void testUserEntity_shouldCascadePostsOnSave() {
        Post post = new Post();
        post.setTitle("Test Post");
        post.setBody("Test Content");
        post.setAuthor(user);

        user.getPosts().add(post);

        User savedUser = entityManager.persistAndFlush(user);
        entityManager.clear();

        User foundUser = entityManager.find(User.class, savedUser.getId());
        assertNotNull(foundUser);
        assertEquals(1, foundUser.getPosts().size());
    }

    @Test
    void testUserEntity_shouldRemoveOrphanPostsOnDelete() {
        Post post = new Post();
        post.setTitle("Test Post");
        post.setBody("Test Content");
        post.setAuthor(user);

        user.getPosts().add(post);
        User savedUser = entityManager.persistAndFlush(user);
        entityManager.clear();

        User foundUser = entityManager.find(User.class, savedUser.getId());
        foundUser.getPosts().clear();
        entityManager.persistAndFlush(foundUser);
        entityManager.clear();

        User updatedUser = entityManager.find(User.class, savedUser.getId());
        assertTrue(updatedUser.getPosts().isEmpty());
    }

    @Test
    void testUserEntity_shouldFindByUsername() {
        entityManager.persistAndFlush(user);
        entityManager.clear();

        User foundUser = entityManager.getEntityManager()
                .createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", "testuser")
                .getSingleResult();

        assertNotNull(foundUser);
        assertEquals("testuser", foundUser.getUsername());
        assertEquals("test@example.com", foundUser.getEmail());
    }

    @Test
    void testUserEntity_shouldFindByEmail() {
        entityManager.persistAndFlush(user);
        entityManager.clear();

        User foundUser = entityManager.getEntityManager()
                .createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", "test@example.com")
                .getSingleResult();

        assertNotNull(foundUser);
        assertEquals("testuser", foundUser.getUsername());
        assertEquals("test@example.com", foundUser.getEmail());
    }

    @Test
    void testUserEntity_shouldUpdateUser() {
        User savedUser = entityManager.persistAndFlush(user);
        entityManager.clear();

        User foundUser = entityManager.find(User.class, savedUser.getId());
        foundUser.setUsername("updateduser");
        foundUser.setEmail("updated@example.com");
        entityManager.persistAndFlush(foundUser);
        entityManager.clear();

        User updatedUser = entityManager.find(User.class, savedUser.getId());
        assertEquals("updateduser", updatedUser.getUsername());
        assertEquals("updated@example.com", updatedUser.getEmail());
    }

    @Test
    void testUserEntity_shouldNotUpdateCreatedAt() {
        User savedUser = entityManager.persistAndFlush(user);
        entityManager.clear();

        User foundUser = entityManager.find(User.class, savedUser.getId());
        LocalDateTime originalCreatedAt = foundUser.getCreatedAt();

        foundUser.setUsername("updateduser");
        entityManager.persistAndFlush(foundUser);
        entityManager.clear();

        User updatedUser = entityManager.find(User.class, savedUser.getId());
        assertEquals(originalCreatedAt, updatedUser.getCreatedAt());
    }

    @Test
    void testUserEntity_shouldDeleteUser() {
        User savedUser = entityManager.persistAndFlush(user);
        UUID userId = savedUser.getId();
        entityManager.clear();

        User foundUser = entityManager.find(User.class, userId);
        entityManager.remove(foundUser);
        entityManager.flush();

        User deletedUser = entityManager.find(User.class, userId);
        assertNull(deletedUser);
    }

    @Test
    void testUserEntity_shouldHaveCorrectTableName() {
        User savedUser = entityManager.persistAndFlush(user);

        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
    }
}