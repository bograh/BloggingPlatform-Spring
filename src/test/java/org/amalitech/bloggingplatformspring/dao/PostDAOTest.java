package org.amalitech.bloggingplatformspring.dao;

import org.amalitech.bloggingplatformspring.config.ConnectionProvider;
import org.amalitech.bloggingplatformspring.dtos.requests.CreatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.PageRequest;
import org.amalitech.bloggingplatformspring.dtos.requests.PostFilterRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.exceptions.ForbiddenException;
import org.amalitech.bloggingplatformspring.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostDAOTest {

    @Mock
    private ConnectionProvider connectionProvider;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private CommentDAO commentRepository;

    @Mock
    private Connection connection;

    @InjectMocks
    private PostDAO postDAO;

    private UUID authorId;
    private int postId;
    private String title;
    private String body;
    private LocalDateTime postedAt;
    private LocalDateTime updatedAt;

    @BeforeEach
    void setUp() throws SQLException {
        authorId = UUID.randomUUID();
        postId = 1;
        title = "Test Post Title";
        body = "Test post body content";
        postedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        when(connectionProvider.getConnection()).thenReturn(connection);
    }

    @Test
    void savePost_Success() throws SQLException {
        CreatePostDTO createPostDTO = new CreatePostDTO();
        createPostDTO.setTitle(title);
        createPostDTO.setBody(body);
        createPostDTO.setAuthorId(authorId.toString());
        createPostDTO.setTags(Arrays.asList("java", "spring"));

        PreparedStatement insertStmt = mock(PreparedStatement.class);
        ResultSet insertRs = mock(ResultSet.class);
        PreparedStatement tagStmt = mock(PreparedStatement.class);

        doNothing().when(connection).setAutoCommit(false);
        doNothing().when(connection).commit();
        doNothing().when(connection).close();
        doNothing().when(insertStmt).close();
        doNothing().when(insertRs).close();
        doNothing().when(tagStmt).close();

        when(connection.prepareStatement(contains("INSERT INTO posts")))
                .thenReturn(insertStmt);
        when(insertStmt.executeQuery()).thenReturn(insertRs);
        when(insertRs.next()).thenReturn(true);
        when(insertRs.getInt("id")).thenReturn(postId);
        when(insertRs.getString("title")).thenReturn(title);
        when(insertRs.getString("body")).thenReturn(body);
        when(insertRs.getObject("author_id")).thenReturn(authorId);
        when(insertRs.getTimestamp("posted_at")).thenReturn(Timestamp.valueOf(postedAt));
        when(insertRs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(updatedAt));

        when(connection.prepareStatement(contains("INSERT INTO post_tags")))
                .thenReturn(tagStmt);
        when(tagRepository.findOrCreate(eq("java"), eq(connection))).thenReturn(1);
        when(tagRepository.findOrCreate(eq("spring"), eq(connection))).thenReturn(2);
        doNothing().when(tagStmt).addBatch();
        when(tagStmt.executeBatch()).thenReturn(new int[] { 1, 1 });

        Post result = postDAO.savePost(createPostDTO);

        assertNotNull(result);
        assertEquals(postId, result.getId());
        assertEquals(title, result.getTitle());
        assertEquals(body, result.getBody());
        assertEquals(authorId, result.getAuthorId());

        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(insertStmt).setString(1, title);
        verify(insertStmt).setString(2, body);
        verify(insertStmt).setObject(3, authorId);
        verify(tagRepository).findOrCreate("java", connection);
        verify(tagRepository).findOrCreate("spring", connection);
        verify(tagStmt, times(2)).addBatch();
        verify(tagStmt).executeBatch();
    }

    @Test
    void savePost_WithoutTags_Success() throws SQLException {
        CreatePostDTO createPostDTO = new CreatePostDTO();
        createPostDTO.setTitle(title);
        createPostDTO.setBody(body);
        createPostDTO.setAuthorId(authorId.toString());
        createPostDTO.setTags(new ArrayList<>());

        PreparedStatement insertStmt = mock(PreparedStatement.class);
        ResultSet insertRs = mock(ResultSet.class);

        doNothing().when(connection).setAutoCommit(false);
        doNothing().when(connection).commit();
        doNothing().when(connection).close();
        doNothing().when(insertStmt).close();
        doNothing().when(insertRs).close();

        when(connection.prepareStatement(contains("INSERT INTO posts")))
                .thenReturn(insertStmt);
        when(insertStmt.executeQuery()).thenReturn(insertRs);
        when(insertRs.next()).thenReturn(true);
        when(insertRs.getInt("id")).thenReturn(postId);
        when(insertRs.getString("title")).thenReturn(title);
        when(insertRs.getString("body")).thenReturn(body);
        when(insertRs.getObject("author_id")).thenReturn(authorId);
        when(insertRs.getTimestamp("posted_at")).thenReturn(Timestamp.valueOf(postedAt));
        when(insertRs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(updatedAt));

        Post result = postDAO.savePost(createPostDTO);

        assertNotNull(result);
        assertEquals(postId, result.getId());
        verify(connection).commit();
        verify(tagRepository, never()).findOrCreate(anyString(), any(Connection.class));
    }

    @Test
    void savePost_InsertFails_ThrowsSQLException() throws SQLException {
        CreatePostDTO createPostDTO = new CreatePostDTO();
        createPostDTO.setTitle(title);
        createPostDTO.setBody(body);
        createPostDTO.setAuthorId(authorId.toString());
        createPostDTO.setTags(List.of("java"));

        PreparedStatement insertStmt = mock(PreparedStatement.class);
        ResultSet insertRs = mock(ResultSet.class);

        doNothing().when(connection).setAutoCommit(false);
        doNothing().when(connection).close();
        doNothing().when(insertStmt).close();
        doNothing().when(insertRs).close();

        when(connection.prepareStatement(contains("INSERT INTO posts")))
                .thenReturn(insertStmt);
        when(insertStmt.executeQuery()).thenReturn(insertRs);
        when(insertRs.next()).thenReturn(false);

        SQLException exception = assertThrows(SQLException.class,
                () -> postDAO.savePost(createPostDTO));

        assertEquals("Failed to insert post", exception.getMessage());
        verify(connection).setAutoCommit(false);
        verify(connection, times(2)).close();
        verify(connection, never()).commit();
    }

    @Test
    void getAllPosts_ReturnsListOfPosts() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        Array tagsArray = mock(Array.class);

        doNothing().when(connection).close();
        doNothing().when(stmt).close();
        doNothing().when(rs).close();

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(commentRepository.getTotalCommentsByPostId(anyInt())).thenReturn(0L);

        when(rs.getInt("id")).thenReturn(1, 2);
        when(rs.getString("title")).thenReturn("Post 1", "Post 2");
        when(rs.getString("body")).thenReturn("Body 1", "Body 2");
        when(rs.getTimestamp("updated_at"))
                .thenReturn(Timestamp.valueOf(updatedAt), Timestamp.valueOf(updatedAt));
        when(rs.getString("author")).thenReturn("author1", "author2");
        when(rs.getArray("tags")).thenReturn(tagsArray);
        when(tagsArray.getArray()).thenReturn(new String[] { "java", "spring" });

        List<PostResponseDTO> result = postDAO.getAllPosts();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(stmt).executeQuery();
    }

    @Test
    void getAllPosts_WithPagination_ReturnsPageResponse() throws SQLException {
        PageRequest pageRequest = new PageRequest(0, 10, "updated_at", "desc");
        PostFilterRequest filterRequest = null;

        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        Array tagsArray = mock(Array.class);

        doNothing().when(connection).close();
        doNothing().when(stmt).close();
        doNothing().when(rs).close();

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(commentRepository.getTotalCommentsByPostId(anyInt())).thenReturn(0L);

        when(rs.getInt("id")).thenReturn(1);
        when(rs.getString("title")).thenReturn("Post 1");
        when(rs.getString("body")).thenReturn("Body 1");
        when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(updatedAt));
        when(rs.getString("author")).thenReturn("author1");
        when(rs.getArray("tags")).thenReturn(tagsArray);
        when(tagsArray.getArray()).thenReturn(new String[] { "java" });
        when(rs.getInt("total_count")).thenReturn(1);

        PageResponse<PostResponseDTO> result = postDAO.getAllPosts(pageRequest, filterRequest);

        assertNotNull(result);
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.content().size());
        verify(stmt).setInt(1, 10);
        verify(stmt).setInt(2, 0);
    }

    @Test
    void findPostById_PostExists_ReturnsPost() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        doNothing().when(connection).close();
        doNothing().when(stmt).close();

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("id")).thenReturn(postId);
        when(rs.getString("title")).thenReturn(title);
        when(rs.getString("body")).thenReturn(body);
        when(rs.getObject("author_id")).thenReturn(authorId);
        when(rs.getTimestamp("posted_at")).thenReturn(Timestamp.valueOf(postedAt));
        when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(updatedAt));

        Optional<Post> result = postDAO.findPostById(postId);

        assertTrue(result.isPresent());
        assertEquals(postId, result.get().getId());
        assertEquals(title, result.get().getTitle());
        verify(stmt).setInt(1, postId);
    }

    @Test
    void findPostById_PostNotFound_ReturnsEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        doNothing().when(connection).close();
        doNothing().when(stmt).close();

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        Optional<Post> result = postDAO.findPostById(999);

        assertFalse(result.isPresent());
        verify(stmt).setInt(1, 999);
    }

    @Test
    void getPostResponseById_PostExists_ReturnsPostResponse() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        Array tagsArray = mock(Array.class);

        doNothing().when(connection).close();
        doNothing().when(stmt).close();

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(commentRepository.getTotalCommentsByPostId(anyInt())).thenReturn(0L);
        when(rs.getInt("id")).thenReturn(postId);
        when(rs.getString("title")).thenReturn(title);
        when(rs.getString("body")).thenReturn(body);
        when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(updatedAt));
        when(rs.getString("author")).thenReturn("author1");
        when(rs.getArray("tags")).thenReturn(tagsArray);
        when(tagsArray.getArray()).thenReturn(new String[] { "java", "spring" });

        Optional<PostResponseDTO> result = postDAO.getPostResponseById(postId);

        assertTrue(result.isPresent());
        verify(stmt).setInt(1, postId);
    }

    @Test
    void updatePost_Success() throws SQLException {
        Post post = new Post();
        post.setId(postId);
        post.setTitle("Updated Title");
        post.setBody("Updated Body");
        post.setAuthorId(authorId);

        List<String> tags = Arrays.asList("java", "testing");

        PreparedStatement updateStmt = mock(PreparedStatement.class);
        PreparedStatement deleteStmt = mock(PreparedStatement.class);
        PreparedStatement insertStmt = mock(PreparedStatement.class);

        doNothing().when(connection).setAutoCommit(false);
        doNothing().when(connection).commit();
        doNothing().when(connection).close();
        doNothing().when(updateStmt).close();
        doNothing().when(deleteStmt).close();
        doNothing().when(insertStmt).close();

        when(connection.prepareStatement(contains("UPDATE posts")))
                .thenReturn(updateStmt);
        when(updateStmt.executeUpdate()).thenReturn(1);

        when(connection.prepareStatement(contains("DELETE FROM post_tags")))
                .thenReturn(deleteStmt);
        when(deleteStmt.executeUpdate()).thenReturn(1);

        when(connection.prepareStatement(contains("INSERT INTO post_tags")))
                .thenReturn(insertStmt);
        when(tagRepository.findOrCreate(anyString(), eq(connection))).thenReturn(1);
        doNothing().when(insertStmt).addBatch();
        when(insertStmt.executeBatch()).thenReturn(new int[] { 1, 1 });

        postDAO.updatePost(post, tags);

        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(updateStmt).setString(1, "Updated Title");
        verify(updateStmt).setString(2, "Updated Body");
        verify(updateStmt).setInt(3, postId);
        verify(updateStmt).setObject(4, authorId);
        verify(updateStmt).executeUpdate();
        verify(deleteStmt).setInt(1, postId);
        verify(deleteStmt).executeUpdate();
    }

    @Test
    void updatePost_NotAuthorized_ThrowsForbiddenException() throws SQLException {
        Post post = new Post();
        post.setId(postId);
        post.setTitle("Updated Title");
        post.setBody("Updated Body");
        post.setAuthorId(authorId);

        PreparedStatement updateStmt = mock(PreparedStatement.class);

        doNothing().when(connection).setAutoCommit(false);
        doNothing().when(connection).close();
        doNothing().when(updateStmt).close();

        when(connection.prepareStatement(contains("UPDATE posts")))
                .thenReturn(updateStmt);
        when(updateStmt.executeUpdate()).thenReturn(0);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> postDAO.updatePost(post, List.of("java")));

        assertEquals("You are not permitted to update this post", exception.getMessage());
        verify(updateStmt).executeUpdate();
        verify(connection, never()).commit();
    }

    @Test
    void deletePost_Success() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        doNothing().when(connection).close();
        doNothing().when(stmt).close();

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeUpdate()).thenReturn(1);

        postDAO.deletePost(postId, authorId);

        verify(stmt).setInt(1, postId);
        verify(stmt).setObject(2, authorId);
        verify(stmt).executeUpdate();
    }

    @Test
    void getTagsByPostId_ReturnsListOfTags() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        doNothing().when(connection).close();
        doNothing().when(stmt).close();

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("name")).thenReturn("java", "spring");

        List<String> result = postDAO.getTagsByPostId(postId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("java", result.get(0));
        assertEquals("spring", result.get(1));
        verify(stmt).setInt(1, postId);
    }

    @Test
    void getTagsByPostId_NoTags_ReturnsEmptyList() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        doNothing().when(connection).close();
        doNothing().when(stmt).close();

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        List<String> result = postDAO.getTagsByPostId(postId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(stmt).setInt(1, postId);
    }
}