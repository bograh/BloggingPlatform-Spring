package org.amalitech.bloggingplatformspring.dao;

import com.mongodb.client.MongoDatabase;
import org.amalitech.bloggingplatformspring.config.MongoConnectionTest;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.entity.CommentDocument;
import org.amalitech.bloggingplatformspring.exceptions.ForbiddenException;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CommentDAOTest {

  private CommentDAO commentDAO;
  private MongoDatabase mongoDatabase;

  @BeforeEach
  void setUp() {
    mongoDatabase = MongoConnectionTest.getDatabase();
    commentDAO = new CommentDAO(mongoDatabase);
    cleanupCollection();
  }

  @AfterEach
  void tearDown() {
    cleanupCollection();
  }

  private void cleanupCollection() {
    mongoDatabase.getCollection(Constants.CommentsMongoCollection).drop();
  }

  @Test
  void createComment_Success() {
    Comment comment = new Comment();
    comment.setContent("This is a test comment");
    comment.setPostId(1);
    comment.setAuthorId("user123");

    CommentDocument result = commentDAO.createComment(comment, "testuser");

    assertNotNull(result);
    assertNotNull(result.getId());
    assertEquals("This is a test comment", result.getContent());
    assertEquals(1, result.getPostId());
    assertEquals("testuser", result.getAuthor());
    assertNotNull(result.getCreatedAt());
  }

  @Test
  void createComment_WithDifferentAuthor_Success() {
    Comment comment = new Comment();
    comment.setContent("Another comment");
    comment.setPostId(2);
    comment.setAuthorId("user456");

    CommentDocument result = commentDAO.createComment(comment, "anotheruser");

    assertNotNull(result);
    assertEquals("Another comment", result.getContent());
    assertEquals(2, result.getPostId());
    assertEquals("anotheruser", result.getAuthor());
  }

  @Test
  void getAllCommentsByPostId_Success() {
    Comment comment1 = new Comment();
    comment1.setContent("Comment 1");
    comment1.setPostId(1);
    comment1.setAuthorId("user123");
    commentDAO.createComment(comment1, "user1");

    Comment comment2 = new Comment();
    comment2.setContent("Comment 2");
    comment2.setPostId(1);
    comment2.setAuthorId("user456");
    commentDAO.createComment(comment2, "user2");

    Comment comment3 = new Comment();
    comment3.setContent("Comment 3");
    comment3.setPostId(2);
    comment3.setAuthorId("user789");
    commentDAO.createComment(comment3, "user3");

    List<CommentDocument> results = commentDAO.getAllCommentsByPostId(1);

    assertNotNull(results);
    assertEquals(2, results.size());
    assertTrue(results.stream().anyMatch(c -> c.getContent().equals("Comment 1")));
    assertTrue(results.stream().anyMatch(c -> c.getContent().equals("Comment 2")));
  }

  @Test
  void getAllCommentsByPostId_NoComments_ReturnsEmptyList() {
    List<CommentDocument> results = commentDAO.getAllCommentsByPostId(999);

    assertNotNull(results);
    assertEquals(0, results.size());
  }

  @Test
  void getAllCommentsByPostId_MultiplePostsWithComments() {
    for (int i = 1; i <= 3; i++) {
      Comment comment = new Comment();
      comment.setContent("Comment for post 1 - " + i);
      comment.setPostId(1);
      comment.setAuthorId("user" + i);
      commentDAO.createComment(comment, "user" + i);
    }

    for (int i = 1; i <= 2; i++) {
      Comment comment = new Comment();
      comment.setContent("Comment for post 2 - " + i);
      comment.setPostId(2);
      comment.setAuthorId("user" + i);
      commentDAO.createComment(comment, "user" + i);
    }

    List<CommentDocument> post1Comments = commentDAO.getAllCommentsByPostId(1);
    List<CommentDocument> post2Comments = commentDAO.getAllCommentsByPostId(2);

    assertEquals(3, post1Comments.size());
    assertEquals(2, post2Comments.size());
  }

  @Test
  void getCommentById_Success() {
    Comment comment = new Comment();
    comment.setContent("Test comment");
    comment.setPostId(1);
    comment.setAuthorId("user123");

    CommentDocument savedComment = commentDAO.createComment(comment, "testuser");

    Optional<CommentDocument> result = commentDAO.getCommentById(savedComment.getId());

    assertTrue(result.isPresent());
    assertEquals(savedComment.getId(), result.get().getId());
    assertEquals("Test comment", result.get().getContent());
    assertEquals(1, result.get().getPostId());
    assertEquals("testuser", result.get().getAuthor());
  }

  @Test
  void getCommentById_NonExistent_ReturnsEmpty() {
    Optional<CommentDocument> result = commentDAO.getCommentById("507f1f77bcf86cd799439011");

    assertFalse(result.isPresent());
  }

  @Test
  void deleteComment_Success() {
    Comment comment = new Comment();
    comment.setContent("Test comment");
    comment.setPostId(1);
    comment.setAuthorId("user123");

    CommentDocument savedComment = commentDAO.createComment(comment, "testuser");

    assertDoesNotThrow(() -> commentDAO.deleteComment(savedComment.getId(), "user123"));

    Optional<CommentDocument> result = commentDAO.getCommentById(savedComment.getId());
    assertFalse(result.isPresent());
  }

  @Test
  void deleteComment_WrongAuthor_ThrowsForbiddenException() {
    Comment comment = new Comment();
    comment.setContent("Test comment");
    comment.setPostId(1);
    comment.setAuthorId("user123");

    CommentDocument savedComment = commentDAO.createComment(comment, "testuser");

    ForbiddenException exception = assertThrows(ForbiddenException.class,
        () -> commentDAO.deleteComment(savedComment.getId(), "wronguser"));

    assertEquals("You are not allowed to delete this comment.", exception.getMessage());

    Optional<CommentDocument> result = commentDAO.getCommentById(savedComment.getId());
    assertTrue(result.isPresent());
  }

  @Test
  void deleteComment_NonExistent_ThrowsForbiddenException() {
    ForbiddenException exception = assertThrows(ForbiddenException.class,
        () -> commentDAO.deleteComment("507f1f77bcf86cd799439011", "user123"));

    assertEquals("You are not allowed to delete this comment.", exception.getMessage());
  }

  @Test
  void createMultipleComments_AllSaved() {
    for (int i = 1; i <= 5; i++) {
      Comment comment = new Comment();
      comment.setContent("Comment " + i);
      comment.setPostId(1);
      comment.setAuthorId("user" + i);
      commentDAO.createComment(comment, "user" + i);
    }

    List<CommentDocument> results = commentDAO.getAllCommentsByPostId(1);

    assertEquals(5, results.size());
  }

  @Test
  void createComment_WithLongContent_Success() {
    String longContent = "This is a very long comment. ".repeat(50);

    Comment comment = new Comment();
    comment.setContent(longContent);
    comment.setPostId(1);
    comment.setAuthorId("user123");

    CommentDocument result = commentDAO.createComment(comment, "testuser");

    assertNotNull(result);
    assertEquals(longContent, result.getContent());
  }

  @Test
  void createComment_WithSpecialCharacters_Success() {
    String specialContent = "Comment with special chars: @#$%^&*()_+-={}[]|\\:\";<>?,./";

    Comment comment = new Comment();
    comment.setContent(specialContent);
    comment.setPostId(1);
    comment.setAuthorId("user123");

    CommentDocument result = commentDAO.createComment(comment, "testuser");

    assertNotNull(result);
    assertEquals(specialContent, result.getContent());
  }
}
