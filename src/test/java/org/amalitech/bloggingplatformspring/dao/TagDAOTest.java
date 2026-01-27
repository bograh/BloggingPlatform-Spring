package org.amalitech.bloggingplatformspring.dao;

import org.amalitech.bloggingplatformspring.config.ConnectionProvider;
import org.amalitech.bloggingplatformspring.entity.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagDAOTest {

    @Mock
    private ConnectionProvider connectionProvider;

    @Mock
    private Connection connection;

    @InjectMocks
    private TagDAO tagDAO;

    private String tagName;
    private int tagId;

    @BeforeEach
    void setUp() throws SQLException {
        tagName = "java";
        tagId = 1;

        when(connectionProvider.getConnection()).thenReturn(connection);
    }

    @Test
    void saveTag_Success() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(connection.prepareStatement(contains("INSERT INTO tags")))
                .thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("id")).thenReturn(tagId);
        when(rs.getString("name")).thenReturn(tagName);

        Tag result = tagDAO.saveTag(tagName);

        assertNotNull(result);
        assertEquals(tagId, result.getId());
        assertEquals(tagName, result.getName());

        verify(stmt).setString(1, tagName);
        verify(stmt).executeQuery();
    }

    @Test
    void saveTag_InsertFails_ThrowsSQLException() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(connection.prepareStatement(contains("INSERT INTO tags")))
                .thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        SQLException ex = assertThrows(
                SQLException.class,
                () -> tagDAO.saveTag(tagName)
        );

        assertEquals("Failed to save Tag", ex.getMessage());
        verify(stmt).setString(1, tagName);
    }

    @Test
    void getAllTags_ReturnsListOfTags() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(connection.prepareStatement(contains("SELECT")))
                .thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, true, false);
        when(rs.getInt("id")).thenReturn(1, 2, 3);
        when(rs.getString("name")).thenReturn("java", "spring", "testing");

        List<Tag> result = tagDAO.getAllTags();

        assertEquals(3, result.size());
        assertEquals("java", result.getFirst().getName());
        assertEquals("spring", result.get(1).getName());
        assertEquals("testing", result.get(2).getName());
    }

    @Test
    void getAllTags_EmptyDatabase_ReturnsEmptyList() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(connection.prepareStatement(contains("SELECT")))
                .thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        List<Tag> result = tagDAO.getAllTags();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllTagsFromNamesList_WithValidNames_ReturnsTags() throws SQLException {
        List<String> tagNames = List.of("java", "spring");

        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(connection.prepareStatement(contains("name IN")))
                .thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getInt("id")).thenReturn(1, 2);
        when(rs.getString("name")).thenReturn("java", "spring");

        List<Tag> result = tagDAO.getAllTagsFromNamesList(tagNames);

        assertEquals(2, result.size());
        verify(stmt).setString(1, "java");
        verify(stmt).setString(2, "spring");
    }

    @Test
    void existsByName_TagExists_ReturnsTrue() throws SQLException {
        mockExistsByName(true);

        Boolean result = tagDAO.existsByName(tagName);

        assertTrue(result);
    }

    @Test
    void existsByName_TagDoesNotExist_ReturnsFalse() throws SQLException {
        mockExistsByName(false);

        Boolean result = tagDAO.existsByName(tagName);

        assertFalse(result);
    }

    private void mockExistsByName(boolean exists) throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(connection.prepareStatement(contains("SELECT 1 FROM tags")))
                .thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(exists);
    }

    @Test
    void getIdByName_TagExists_ReturnsId() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(connection.prepareStatement(contains("SELECT id FROM tags")))
                .thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("id")).thenReturn(tagId);

        Optional<Integer> result = tagDAO.getIdByName(tagName);

        assertTrue(result.isPresent());
        assertEquals(tagId, result.get());
    }

    @Test
    void getIdByName_TagDoesNotExist_ReturnsEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(connection.prepareStatement(contains("SELECT id FROM tags")))
                .thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        Optional<Integer> result = tagDAO.getIdByName(tagName);

        assertTrue(result.isEmpty());
    }
}