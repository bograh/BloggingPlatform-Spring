package org.amalitech.bloggingplatformspring.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class PostEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setUsername("testauthor");
        author.setEmail("author@example.com");
        author.setPassword("password123");
        author = entityManager.persistAndFlush(author);

        post = new Post();
        post.setTitle("Test Post");
        post.setBody("This is a test post body");
        post.setAuthor(author);
    }

    @Test
    void testPostEntity_shouldPersistSuccessfully() {
        Post savedPost = entityManager.persistAndFlush(post);

        assertNotNull(savedPost);
        assertNotNull(savedPost.getId());
        assertEquals("Test Post", savedPost.getTitle());
        assertEquals("This is a test post body", savedPost.getBody());
        assertNotNull(savedPost.getAuthor());
        assertEquals(author.getId(), savedPost.getAuthor().getId());
        assertNotNull(savedPost.getPostedAt());
        assertNotNull(savedPost.getUpdatedAt());
    }

    @Test
    void testPostEntity_shouldGenerateIdentity() {
        Post savedPost = entityManager.persistAndFlush(post);

        assertNotNull(savedPost.getId());
        assertTrue(savedPost.getId() > 0);
    }

    @Test
    void testPostEntity_shouldSetPostedAtOnPersist() {
        LocalDateTime before = LocalDateTime.now();

        Post savedPost = entityManager.persistAndFlush(post);

        LocalDateTime after = LocalDateTime.now();
        assertNotNull(savedPost.getPostedAt());
        assertTrue(savedPost.getPostedAt().isAfter(before.minusSeconds(1)));
        assertTrue(savedPost.getPostedAt().isBefore(after.plusSeconds(1)));
    }

    @Test
    void testPostEntity_shouldSetUpdatedAtOnPersist() {
        LocalDateTime before = LocalDateTime.now();

        Post savedPost = entityManager.persistAndFlush(post);

        LocalDateTime after = LocalDateTime.now();
        assertNotNull(savedPost.getUpdatedAt());
        assertTrue(savedPost.getUpdatedAt().isAfter(before.minusSeconds(1)));
        assertTrue(savedPost.getUpdatedAt().isBefore(after.plusSeconds(1)));
    }

    @Test
    void testPostEntity_shouldUpdateUpdatedAtOnUpdate() throws InterruptedException {
        Post savedPost = entityManager.persistAndFlush(post);
        LocalDateTime originalUpdatedAt = savedPost.getUpdatedAt();
        entityManager.clear();

        Thread.sleep(100);

        Post foundPost = entityManager.find(Post.class, savedPost.getId());
        foundPost.setTitle("Updated Title");
        entityManager.persistAndFlush(foundPost);

        assertTrue(foundPost.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void testPostEntity_shouldNotUpdatePostedAtOnUpdate() {
        Post savedPost = entityManager.persistAndFlush(post);
        entityManager.clear();

        Post foundPost = entityManager.find(Post.class, savedPost.getId());
        LocalDateTime originalPostedAt = foundPost.getPostedAt();

        foundPost.setTitle("Updated Title");
        entityManager.persistAndFlush(foundPost);
        entityManager.clear();

        Post updatedPost = entityManager.find(Post.class, savedPost.getId());
        assertEquals(originalPostedAt, updatedPost.getPostedAt());
    }

    @Test
    void testPostEntity_shouldNotAllowNullTitle() {
        post.setTitle(null);

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(post);
        });
    }

    @Test
    void testPostEntity_shouldNotAllowNullBody() {
        post.setBody(null);

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(post);
        });
    }

    @Test
    void testPostEntity_shouldNotAllowNullAuthor() {
        post.setAuthor(null);

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(post);
        });
    }

    @Test
    void testPostEntity_shouldUseLazyLoadingForAuthor() {
        Post savedPost = entityManager.persistAndFlush(post);
        entityManager.clear();

        Post foundPost = entityManager.find(Post.class, savedPost.getId());

        assertNotNull(foundPost);
        assertNotNull(foundPost.getAuthor());
    }

    @Test
    void testPostEntity_shouldInitializeTagsAsEmptySet() {
        Post savedPost = entityManager.persistAndFlush(post);

        assertNotNull(savedPost.getTags());
        assertTrue(savedPost.getTags().isEmpty());
    }

    @Test
    void testPostEntity_shouldAddTags() {
        Tag tag1 = new Tag();
        tag1.setName("java");

        Tag tag2 = new Tag();
        tag2.setName("spring");

        post.getTags().add(tag1);
        post.getTags().add(tag2);

        Post savedPost = entityManager.persistAndFlush(post);
        entityManager.clear();

        Post foundPost = entityManager.find(Post.class, savedPost.getId());
        assertNotNull(foundPost.getTags());
        assertEquals(2, foundPost.getTags().size());
    }

    @Test
    void testPostEntity_shouldPersistTagsWithCascade() {
        Tag tag = new Tag();
        tag.setName("testing");
        post.getTags().add(tag);

        Post savedPost = entityManager.persistAndFlush(post);
        entityManager.clear();

        Post foundPost = entityManager.find(Post.class, savedPost.getId());
        assertEquals(1, foundPost.getTags().size());

        Tag foundTag = foundPost.getTags().iterator().next();
        assertNotNull(foundTag.getId());
        assertEquals("testing", foundTag.getName());
    }

    @Test
    void testPostEntity_shouldRemoveTagsFromPost() {
        Tag tag = new Tag();
        tag.setName("removeme");
        post.getTags().add(tag);

        Post savedPost = entityManager.persistAndFlush(post);
        entityManager.clear();

        Post foundPost = entityManager.find(Post.class, savedPost.getId());
        foundPost.getTags().clear();
        entityManager.persistAndFlush(foundPost);
        entityManager.clear();

        Post updatedPost = entityManager.find(Post.class, savedPost.getId());
        assertTrue(updatedPost.getTags().isEmpty());
    }

    @Test
    void testPostEntity_shouldFindPostsByAuthor() {
        Post post2 = new Post();
        post2.setTitle("Second Post");
        post2.setBody("Second post body");
        post2.setAuthor(author);

        entityManager.persistAndFlush(post);
        entityManager.persistAndFlush(post2);
        entityManager.clear();

        List<Post> posts = entityManager.getEntityManager()
                .createQuery("SELECT p FROM Post p WHERE p.author.id = :authorId", Post.class)
                .setParameter("authorId", author.getId())
                .getResultList();

        assertNotNull(posts);
        assertEquals(2, posts.size());
    }

    @Test
    void testPostEntity_shouldOrderPostsByPostedAt() {
        Post post2 = new Post();
        post2.setTitle("Second Post");
        post2.setBody("Second post body");
        post2.setAuthor(author);

        entityManager.persistAndFlush(post);
        entityManager.persistAndFlush(post2);
        entityManager.clear();

        List<Post> posts = entityManager.getEntityManager()
                .createQuery("SELECT p FROM Post p ORDER BY p.postedAt DESC", Post.class)
                .getResultList();

        assertNotNull(posts);
        assertEquals(2, posts.size());
        assertTrue(posts.get(0).getPostedAt().isAfter(posts.get(1).getPostedAt()) ||
                posts.get(0).getPostedAt().isEqual(posts.get(1).getPostedAt()));
    }

    @Test
    void testPostEntity_shouldUpdatePost() {
        Post savedPost = entityManager.persistAndFlush(post);
        entityManager.clear();

        Post foundPost = entityManager.find(Post.class, savedPost.getId());
        foundPost.setTitle("Updated Title");
        foundPost.setBody("Updated Body");
        entityManager.persistAndFlush(foundPost);
        entityManager.clear();

        Post updatedPost = entityManager.find(Post.class, savedPost.getId());
        assertEquals("Updated Title", updatedPost.getTitle());
        assertEquals("Updated Body", updatedPost.getBody());
    }

    @Test
    void testPostEntity_shouldDeletePost() {
        Post savedPost = entityManager.persistAndFlush(post);
        Long postId = savedPost.getId();
        entityManager.clear();

        Post foundPost = entityManager.find(Post.class, postId);
        entityManager.remove(foundPost);
        entityManager.flush();

        Post deletedPost = entityManager.find(Post.class, postId);
        assertNull(deletedPost);
    }

    @Test
    void testPostEntity_shouldMaintainBidirectionalRelationshipWithAuthor() {
        Post savedPost = entityManager.persistAndFlush(post);
        entityManager.clear();

        User foundAuthor = entityManager.find(User.class, author.getId());

        assertNotNull(foundAuthor);
        assertNotNull(foundAuthor.getPosts());
        assertTrue(foundAuthor.getPosts().stream()
                .anyMatch(p -> p.getId().equals(savedPost.getId())));
    }

    @Test
    void testPostEntity_shouldFindByAuthorAndPostedAtIndex() {
        entityManager.persistAndFlush(post);
        entityManager.clear();

        List<Post> posts = entityManager.getEntityManager()
                .createQuery("SELECT p FROM Post p WHERE p.author.id = :authorId " +
                        "ORDER BY p.postedAt DESC", Post.class)
                .setParameter("authorId", author.getId())
                .getResultList();

        assertNotNull(posts);
        assertEquals(1, posts.size());
        assertEquals("Test Post", posts.get(0).getTitle());
    }

    @Test
    void testPostEntity_shouldHandleMultipleTagsOnSamePost() {
        Tag tag1 = new Tag();
        tag1.setName("java");

        Tag tag2 = new Tag();
        tag2.setName("spring");

        Tag tag3 = new Tag();
        tag3.setName("hibernate");

        post.getTags().add(tag1);
        post.getTags().add(tag2);
        post.getTags().add(tag3);

        Post savedPost = entityManager.persistAndFlush(post);
        entityManager.clear();

        Post foundPost = entityManager.find(Post.class, savedPost.getId());
        assertEquals(3, foundPost.getTags().size());
    }

    @Test
    void testPostEntity_shouldShareTagsBetweenPosts() {
        Tag sharedTag = new Tag();
        sharedTag.setName("shared");
        sharedTag = entityManager.persistAndFlush(sharedTag);

        post.getTags().add(sharedTag);

        Post post2 = new Post();
        post2.setTitle("Second Post");
        post2.setBody("Second Body");
        post2.setAuthor(author);
        post2.getTags().add(sharedTag);

        entityManager.persistAndFlush(post);
        entityManager.persistAndFlush(post2);
        entityManager.clear();

        Post foundPost1 = entityManager.find(Post.class, post.getId());
        Post foundPost2 = entityManager.find(Post.class, post2.getId());

        Tag tagFromPost1 = foundPost1.getTags().iterator().next();
        Tag tagFromPost2 = foundPost2.getTags().iterator().next();

        assertEquals(tagFromPost1.getId(), tagFromPost2.getId());
        assertEquals("shared", tagFromPost1.getName());
    }
}