package org.amalitech.bloggingplatformspring.dao;

import org.amalitech.bloggingplatformspring.config.ConnectionProvider;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDAOTest {

    @Mock
    private ConnectionProvider connectionProvider;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private UserDAO userDAO;

    private UUID userId;
    private String username;
    private String email;
    private String password;
    private LocalDateTime createdAt;

    @BeforeEach
    void setUp() throws SQLException {
        userDAO = new UserDAO(connectionProvider);

        userId = UUID.randomUUID();
        username = "testuser";
        email = "test@example.com";
        password = "hashedPassword123";
        createdAt = LocalDateTime.now();

        when(connectionProvider.getConnection()).thenReturn(connection);
        doNothing().when(connection).close();
    }

    @Test
    void saveUser_Success() throws SQLException {
        PreparedStatement checkStmt = mock(PreparedStatement.class);
        ResultSet checkRs = mock(ResultSet.class);
        PreparedStatement insertStmt = mock(PreparedStatement.class);
        ResultSet insertRs = mock(ResultSet.class);

        doNothing().when(checkStmt).close();
        doNothing().when(checkRs).close();
        doNothing().when(insertStmt).close();
        doNothing().when(insertRs).close();

        when(connection.prepareStatement(contains("SELECT COUNT(*)")))
                .thenReturn(checkStmt);
        when(checkStmt.executeQuery()).thenReturn(checkRs);
        when(checkRs.next()).thenReturn(true);
        when(checkRs.getInt(1)).thenReturn(0);

        when(connection.prepareStatement(contains("INSERT INTO users")))
                .thenReturn(insertStmt);
        when(insertStmt.executeQuery()).thenReturn(insertRs);
        when(insertRs.next()).thenReturn(true);

        Timestamp timestamp = Timestamp.valueOf(createdAt);
        when(insertRs.getObject("id")).thenReturn(userId);
        when(insertRs.getString("username")).thenReturn(username);
        when(insertRs.getString("email")).thenReturn(email);
        when(insertRs.getTimestamp("created_at")).thenReturn(timestamp);

        User result = userDAO.saveUser(username, email, password);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());

        verify(connectionProvider).getConnection();
        verify(checkStmt).setString(1, username);
        verify(checkStmt).setString(2, email);
        verify(insertStmt).setObject(eq(1), any(UUID.class));
        verify(insertStmt).setString(2, username);
        verify(insertStmt).setString(3, email);
        verify(insertStmt).setString(4, password);
        verify(connection).close();
        verify(checkStmt).close();
        verify(insertStmt).close();
    }

    @Test
    void saveUser_UserAlreadyExists_ThrowsBadRequestException() throws SQLException {
        PreparedStatement checkStmt = mock(PreparedStatement.class);
        ResultSet checkRs = mock(ResultSet.class);

        when(connection.prepareStatement(contains("SELECT COUNT(*)"))).thenReturn(checkStmt);
        when(checkStmt.executeQuery()).thenReturn(checkRs);
        when(checkRs.next()).thenReturn(true);
        when(checkRs.getInt(1)).thenReturn(1); // User exists

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userDAO.saveUser(username, email, password));

        assertEquals("User with this username or email already exists", exception.getMessage());
        verify(checkStmt).setString(1, username);
        verify(checkStmt).setString(2, email);
        verify(connection, never()).prepareStatement(contains("INSERT INTO users"));
    }

    @Test
    void saveUser_InsertFails_ThrowsSQLException() throws SQLException {
        PreparedStatement checkStmt = mock(PreparedStatement.class);
        ResultSet checkRs = mock(ResultSet.class);

        when(connection.prepareStatement(contains("SELECT COUNT(*)"))).thenReturn(checkStmt);
        when(checkStmt.executeQuery()).thenReturn(checkRs);
        when(checkRs.next()).thenReturn(true);
        when(checkRs.getInt(1)).thenReturn(0);

        PreparedStatement insertStmt = mock(PreparedStatement.class);
        ResultSet insertRs = mock(ResultSet.class);

        when(connection.prepareStatement(contains("INSERT INTO users"))).thenReturn(insertStmt);
        when(insertStmt.executeQuery()).thenReturn(insertRs);
        when(insertRs.next()).thenReturn(false);

        SQLException exception = assertThrows(SQLException.class,
                () -> userDAO.saveUser(username, email, password));

        assertEquals("Failed to register user", exception.getMessage());
    }

    @Test
    void findUserByEmailAndPassword_Success() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Timestamp timestamp = Timestamp.valueOf(createdAt);
        when(resultSet.getObject("id")).thenReturn(userId);
        when(resultSet.getString("username")).thenReturn(username);
        when(resultSet.getString("email")).thenReturn(email);
        when(resultSet.getTimestamp("created_at")).thenReturn(timestamp);

        User result = userDAO.findUserByEmailAndPassword(email, password);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());

        verify(preparedStatement).setString(1, email);
        verify(preparedStatement).setString(2, password);
    }

    @Test
    void findUserByEmailAndPassword_InvalidCredentials_ThrowsForbiddenException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> userDAO.findUserByEmailAndPassword(email, "wrongPassword"));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(preparedStatement).setString(1, email);
        verify(preparedStatement).setString(2, "wrongPassword");
    }

    @Test
    void findUserByEmailAndPassword_DatabaseError_ThrowsSQLException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        assertThrows(SQLException.class,
                () -> userDAO.findUserByEmailAndPassword(email, password));
    }

    @Test
    void getUsernameById_Success() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("username")).thenReturn(username);

        String result = userDAO.getUsernameById(userId);

        assertEquals(username, result);
        verify(preparedStatement).setObject(1, userId);
    }

    @Test
    void getUsernameById_UserNotFound_ReturnsEmptyString() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        String result = userDAO.getUsernameById(userId);

        assertEquals("", result);
        verify(preparedStatement).setObject(1, userId);
    }

    @Test
    void getUsernameById_DatabaseError_ThrowsSQLException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));

        assertThrows(SQLException.class,
                () -> userDAO.getUsernameById(userId));
    }

    @Test
    void findUserById_Success() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Timestamp timestamp = Timestamp.valueOf(createdAt);
        when(resultSet.getObject("id")).thenReturn(userId);
        when(resultSet.getString("username")).thenReturn(username);
        when(resultSet.getString("email")).thenReturn(email);
        when(resultSet.getTimestamp("created_at")).thenReturn(timestamp);

        Optional<User> result = userDAO.findUserById(userId);

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getId());
        assertEquals(username, result.get().getUsername());
        assertEquals(email, result.get().getEmail());

        verify(preparedStatement).setObject(1, userId);
    }

    @Test
    void findUserById_UserNotFound_ReturnsEmpty() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<User> result = userDAO.findUserById(userId);

        assertFalse(result.isPresent());
        verify(preparedStatement).setObject(1, userId);
    }

    @Test
    void findUserById_DatabaseError_ThrowsSQLException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Connection lost"));

        assertThrows(SQLException.class,
                () -> userDAO.findUserById(userId));
    }

    @Test
    void userExistsByUsername_UserExists_ReturnsTrue() throws SQLException {
        when(connection.prepareStatement(contains("username"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        Boolean result = userDAO.userExistsByUsername(username);

        assertTrue(result);
        verify(preparedStatement).setString(1, username);
    }

    @Test
    void userExistsByUsername_UserDoesNotExist_ReturnsFalse() throws SQLException {
        when(connection.prepareStatement(contains("username"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);

        Boolean result = userDAO.userExistsByUsername(username);

        assertFalse(result);
        verify(preparedStatement).setString(1, username);
    }

    @Test
    void userExistsByUsername_DatabaseError_ThrowsSQLException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Query error"));

        assertThrows(SQLException.class,
                () -> userDAO.userExistsByUsername(username));
    }

    @Test
    void userExistsByEmail_UserExists_ReturnsTrue() throws SQLException {
        when(connection.prepareStatement(contains("email"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        Boolean result = userDAO.userExistsByEmail(email);

        assertTrue(result);
        verify(preparedStatement).setString(1, email);
    }

    @Test
    void userExistsByEmail_UserDoesNotExist_ReturnsFalse() throws SQLException {
        when(connection.prepareStatement(contains("email"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);

        Boolean result = userDAO.userExistsByEmail(email);

        assertFalse(result);
        verify(preparedStatement).setString(1, email);
    }

    @Test
    void userExistsByEmail_DatabaseError_ThrowsSQLException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Connection timeout"));

        assertThrows(SQLException.class,
                () -> userDAO.userExistsByEmail(email));
    }

    @Test
    void findUserByUsername_Success() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Timestamp timestamp = Timestamp.valueOf(createdAt);
        when(resultSet.getObject("id")).thenReturn(userId);
        when(resultSet.getString("username")).thenReturn(username);
        when(resultSet.getString("email")).thenReturn(email);
        when(resultSet.getTimestamp("created_at")).thenReturn(timestamp);

        Optional<User> result = userDAO.findUserByUsername(username);

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getId());
        assertEquals(username, result.get().getUsername());
        assertEquals(email, result.get().getEmail());

        verify(preparedStatement).setObject(1, username);
    }

    @Test
    void findUserByUsername_UserNotFound_ReturnsEmpty() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<User> result = userDAO.findUserByUsername("nonexistent");

        assertFalse(result.isPresent());
        verify(preparedStatement).setObject(1, "nonexistent");
    }

    @Test
    void findUserByUsername_DatabaseError_ThrowsSQLException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        assertThrows(SQLException.class,
                () -> userDAO.findUserByUsername(username));
    }

    @Test
    void findUserByEmailAndPassword_WithWhitespace_Success() throws SQLException {
        String emailWithSpaces = "  test@example.com  ";

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Timestamp timestamp = Timestamp.valueOf(createdAt);
        when(resultSet.getObject("id")).thenReturn(userId);
        when(resultSet.getTimestamp("created_at")).thenReturn(timestamp);

        User result = userDAO.findUserByEmailAndPassword(emailWithSpaces, password);

        assertNotNull(result);
        verify(preparedStatement).setString(1, emailWithSpaces);
    }

    @Test
    void userExistsByUsername_EmptyUsername_ReturnsFalse() throws SQLException {
        when(connection.prepareStatement(contains("username"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);

        Boolean result = userDAO.userExistsByUsername("");

        assertFalse(result);
        verify(preparedStatement).setString(1, "");
    }

    @Test
    void userExistsByEmail_NullResultSet_ReturnsFalse() throws SQLException {
        when(connection.prepareStatement(contains("email"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Boolean result = userDAO.userExistsByEmail(email);

        assertFalse(result);
    }

    @Test
    void getUsernameById_WithDifferentUUIDs_ReturnsCorrectUsername() throws SQLException {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true);
        when(resultSet.getString("username")).thenReturn("user1").thenReturn("user2");

        String result1 = userDAO.getUsernameById(userId1);
        String result2 = userDAO.getUsernameById(userId2);

        assertEquals("user1", result1);
        assertEquals("user2", result2);
    }
}