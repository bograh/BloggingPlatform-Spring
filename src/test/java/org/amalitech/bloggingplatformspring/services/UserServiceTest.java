package org.amalitech.bloggingplatformspring.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.amalitech.bloggingplatformspring.dtos.requests.RegisterUserDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.SignInUserDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserProfileResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.UserResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.InvalidUserIdFormatException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.exceptions.UnauthorizedException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.CommentUtils;
import org.amalitech.bloggingplatformspring.utils.PostUtils;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostUtils postUtils;

    @Mock
    private UserUtils userUtils;

    @InjectMocks
    private UserService userService;

    private RegisterUserDTO registerUserDTO;
    private SignInUserDTO signInUserDTO;
    private User user;
    private UserResponseDTO userResponseDTO;
    private UUID validUserId;
    private Post testPost1;
    private Post testPost2;
    private Post testPost3;
    private Comment testComment1;
    private Comment testComment2;
    private Comment testComment3;

    @BeforeEach
    void setUp() {
        registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setUsername("testuser");
        registerUserDTO.setEmail("test@example.com");
        registerUserDTO.setPassword("SecurePass123!");

        signInUserDTO = new SignInUserDTO();
        signInUserDTO.setEmail("test@example.com");
        signInUserDTO.setPassword("SecurePass123!");
        UUID userID = UUID.randomUUID();
        user = new User();
        user.setId(userID);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword(BCrypt.withDefaults().hashToString(12, "SecurePass123!".toCharArray()));

        userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(String.valueOf(userID));
        userResponseDTO.setUsername("testuser");
        userResponseDTO.setEmail("test@example.com");

        validUserId = userID;
        testPost1 = createPost(1L, "Post 1", user);
        testPost2 = createPost(2L, "Post 2", user);
        testPost3 = createPost(3L, "Post 3", user);

        testComment1 = createComment("Comment 1", user);
        testComment2 = createComment("Comment 2", user);
        testComment3 = createComment("Comment 3", user);
    }

    private Post createPost(Long id, String title, User author) {
        Post post = new Post();
        post.setId(id);
        post.setTitle(title);
        post.setAuthor(author);
        post.setUpdatedAt(LocalDateTime.now());
        return post;
    }

    private Comment createComment(String content, User author) {
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setAuthor(author.getUsername());
        comment.setCommentedAt(LocalDateTime.now());
        return comment;
    }

    @Test
    void registerUser_Success() {
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userUtils.mapUserToUserResponse(any(User.class))).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.registerUser(registerUserDTO);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
        verify(userUtils).mapUserToUserResponse(any(User.class));
    }

    @Test
    void registerUser_PasswordContainsUsername_ThrowsException() {
        registerUserDTO.setPassword("testuser123");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Password must not contain username", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_UsernameTaken_ThrowsException() {
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Username is taken", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_EmailTaken_ThrowsException() {
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Email is taken", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_PasswordContainsUsernameUpperCase_ThrowsException() {
        registerUserDTO.setPassword("TESTUSER123");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.registerUser(registerUserDTO));

        assertEquals("Password must not contain username", exception.getMessage());
    }

    @Test
    void signInUser_Success() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);
        when(userRepository.findUserByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));
        when(userUtils.mapUserToUserResponse(any(User.class))).thenReturn(userResponseDTO);

        UserResponseDTO result = userService.signInUser(signInUserDTO);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).findUserByEmailIgnoreCase(anyString());
        verify(userUtils).mapUserToUserResponse(any(User.class));
    }

    @Test
    void signInUser_EmailNotExists_ThrowsException() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> userService.signInUser(signInUserDTO));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository, never()).findUserByEmailIgnoreCase(anyString());
    }

    @Test
    void signInUser_UserNotFound_ThrowsException() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);
        when(userRepository.findUserByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> userService.signInUser(signInUserDTO));

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void signInUser_InvalidPassword_ThrowsException() {
        signInUserDTO.setPassword("WrongPassword123!");

        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);
        when(userRepository.findUserByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> userService.signInUser(signInUserDTO));

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void registerUser_SavedUserHasHashedPassword() {
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertNotEquals(registerUserDTO.getPassword(), savedUser.getPassword());
            assertTrue(savedUser.getPassword().startsWith("$2a$"));
            return savedUser;
        });
        when(userUtils.mapUserToUserResponse(any(User.class))).thenReturn(userResponseDTO);

        userService.registerUser(registerUserDTO);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUserProfile_ShouldReturnUserProfile_WhenValidUserIdProvided() {
        // Arrange
        String userIdString = validUserId.toString();
        List<Post> recentPosts = List.of(testPost1, testPost2, testPost3);
        List<Comment> recentComments = List.of(testComment1, testComment2, testComment3);

        PostResponseDTO postResponse1 = new PostResponseDTO();
        PostResponseDTO postResponse2 = new PostResponseDTO();
        PostResponseDTO postResponse3 = new PostResponseDTO();

        CommentResponse commentResponse1 = new CommentResponse();
        CommentResponse commentResponse2 = new CommentResponse();
        CommentResponse commentResponse3 = new CommentResponse();

        UserProfileResponse expectedResponse = new UserProfileResponse(
                userIdString,
                user.getUsername(),
                user.getEmail(),
                0L,
                0L,
                List.of(), List.of()
        );

        when(userRepository.findById(validUserId)).thenReturn(Optional.of(user));
        when(postRepository.findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4)))
                .thenReturn(recentPosts);
        when(commentRepository.countByPostId(testPost1.getId())).thenReturn(5L);
        when(commentRepository.countByPostId(testPost2.getId())).thenReturn(3L);
        when(commentRepository.countByPostId(testPost3.getId())).thenReturn(7L);
        when(postUtils.createPostResponseFromPost(testPost1, 5L)).thenReturn(postResponse1);
        when(postUtils.createPostResponseFromPost(testPost2, 3L)).thenReturn(postResponse2);
        when(postUtils.createPostResponseFromPost(testPost3, 7L)).thenReturn(postResponse3);

        when(commentRepository.findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5)))
                .thenReturn(recentComments);

        when(postRepository.countByAuthor(user)).thenReturn(10L);
        when(commentRepository.countByAuthor(user.getUsername())).thenReturn(25L);

        when(userUtils.createUserProfileResponse(eq(user), anyList(), anyList(), eq(10L), eq(25L)))
                .thenReturn(expectedResponse);

        try (MockedStatic<CommentUtils> commentUtilsMock = mockStatic(CommentUtils.class)) {
            commentUtilsMock.when(() -> CommentUtils.createCommentResponseFromComment(testComment1))
                    .thenReturn(commentResponse1);
            commentUtilsMock.when(() -> CommentUtils.createCommentResponseFromComment(testComment2))
                    .thenReturn(commentResponse2);
            commentUtilsMock.when(() -> CommentUtils.createCommentResponseFromComment(testComment3))
                    .thenReturn(commentResponse3);

            // Act
            UserProfileResponse result = userService.getUserProfile(userIdString);

            // Assert
            assertThat(result).isEqualTo(expectedResponse);
            verify(userRepository).findById(validUserId);
            verify(postRepository).findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4));
            verify(commentRepository).findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5));
            verify(postRepository).countByAuthor(user);
            verify(commentRepository).countByAuthor(user.getUsername());
        }
    }

    @Test
    void getUserProfile_ShouldThrowBadRequestException_WhenUserIdIsBlank() {
        // Act & Assert
        assertThatThrownBy(() -> userService.getUserProfile(""))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User ID cannot be empty");

        verifyNoInteractions(userRepository, postRepository, commentRepository);
    }

    @Test
    void getUserProfile_ShouldThrowBadRequestException_WhenUserIdIsWhitespace() {
        // Act & Assert
        assertThatThrownBy(() -> userService.getUserProfile("   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User ID cannot be empty");

        verifyNoInteractions(userRepository, postRepository, commentRepository);
    }

    @Test
    void getUserProfile_ShouldThrowInvalidUserIdFormatException_WhenUserIdIsNotValidUUID() {
        // Arrange
        String invalidUserId = "invalid-uuid-format";

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserProfile(invalidUserId))
                .isInstanceOf(InvalidUserIdFormatException.class)
                .hasMessage("Invalid UUID format for userID");

        verifyNoInteractions(userRepository, postRepository, commentRepository);
    }

    @Test
    void getUserProfile_ShouldThrowInvalidUserIdFormatException_WhenUserIdIsRandomString() {
        // Act & Assert
        assertThatThrownBy(() -> userService.getUserProfile("not-a-uuid"))
                .isInstanceOf(InvalidUserIdFormatException.class)
                .hasMessage("Invalid UUID format for userID");

        verifyNoInteractions(userRepository, postRepository, commentRepository);
    }

    @Test
    void getUserProfile_ShouldThrowResourceNotFoundException_WhenUserNotFound() {
        // Arrange
        String userIdString = validUserId.toString();
        when(userRepository.findById(validUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserProfile(userIdString))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: " + validUserId);

        verify(userRepository).findById(validUserId);
        verifyNoInteractions(postRepository, commentRepository);
    }

    @Test
    void getUserProfile_ShouldHandleUserWithNoPosts() {
        // Arrange
        String userIdString = validUserId.toString();
        List<Post> emptyPosts = new ArrayList<>();
        List<Comment> recentComments = List.of(testComment1);

        CommentResponse commentResponse1 = new CommentResponse();
        UserProfileResponse expectedResponse = new UserProfileResponse(
                userIdString,
                user.getUsername(),
                user.getEmail(),
                0L,
                0L,
                List.of(), List.of()
        );

        when(userRepository.findById(validUserId)).thenReturn(Optional.of(user));
        when(postRepository.findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4)))
                .thenReturn(emptyPosts);
        when(commentRepository.findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5)))
                .thenReturn(recentComments);
        when(postRepository.countByAuthor(user)).thenReturn(0L);
        when(commentRepository.countByAuthor(user.getUsername())).thenReturn(1L);
        when(userUtils.createUserProfileResponse(eq(user), anyList(), anyList(), eq(0L), eq(1L)))
                .thenReturn(expectedResponse);

        try (MockedStatic<CommentUtils> commentUtilsMock = mockStatic(CommentUtils.class)) {
            commentUtilsMock.when(() -> CommentUtils.createCommentResponseFromComment(testComment1))
                    .thenReturn(commentResponse1);

            // Act
            UserProfileResponse result = userService.getUserProfile(userIdString);

            // Assert
            assertThat(result).isEqualTo(expectedResponse);
            verify(postRepository).findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4));
            verify(postRepository).countByAuthor(user);
        }
    }

    @Test
    void getUserProfile_ShouldHandleUserWithNoComments() {
        // Arrange
        String userIdString = validUserId.toString();
        List<Post> recentPosts = List.of(testPost1);
        List<Comment> emptyComments = new ArrayList<>();

        PostResponseDTO postResponse1 = new PostResponseDTO();
        UserProfileResponse expectedResponse = new UserProfileResponse(
                userIdString,
                user.getUsername(),
                user.getEmail(),
                0L,
                0L,
                List.of(), List.of()
        );

        when(userRepository.findById(validUserId)).thenReturn(Optional.of(user));
        when(postRepository.findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4)))
                .thenReturn(recentPosts);
        when(commentRepository.countByPostId(testPost1.getId())).thenReturn(2L);
        when(postUtils.createPostResponseFromPost(testPost1, 2L)).thenReturn(postResponse1);
        when(commentRepository.findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5)))
                .thenReturn(emptyComments);
        when(postRepository.countByAuthor(user)).thenReturn(1L);
        when(commentRepository.countByAuthor(user.getUsername())).thenReturn(0L);
        when(userUtils.createUserProfileResponse(eq(user), anyList(), anyList(), eq(1L), eq(0L)))
                .thenReturn(expectedResponse);

        // Act
        UserProfileResponse result = userService.getUserProfile(userIdString);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(commentRepository).findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5));
        verify(commentRepository).countByAuthor(user.getUsername());
    }

    @Test
    void getUserProfile_ShouldHandleUserWithNoPostsAndNoComments() {
        // Arrange
        String userIdString = validUserId.toString();
        List<Post> emptyPosts = new ArrayList<>();
        List<Comment> emptyComments = new ArrayList<>();
        UserProfileResponse expectedResponse = new UserProfileResponse(
                userIdString,
                user.getUsername(),
                user.getEmail(),
                0L,
                0L,
                List.of(), List.of()
        );

        when(userRepository.findById(validUserId)).thenReturn(Optional.of(user));
        when(postRepository.findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4)))
                .thenReturn(emptyPosts);
        when(commentRepository.findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5)))
                .thenReturn(emptyComments);
        when(postRepository.countByAuthor(user)).thenReturn(0L);
        when(commentRepository.countByAuthor(user.getUsername())).thenReturn(0L);
        when(userUtils.createUserProfileResponse(eq(user), anyList(), anyList(), eq(0L), eq(0L)))
                .thenReturn(expectedResponse);

        // Act
        UserProfileResponse result = userService.getUserProfile(userIdString);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(postRepository).countByAuthor(user);
        verify(commentRepository).countByAuthor(user.getUsername());
    }

    @Test
    void getUserProfile_ShouldLimitRecentPostsToFour() {
        // Arrange
        String userIdString = validUserId.toString();
        List<Post> recentPosts = List.of(testPost1, testPost2, testPost3);

        when(userRepository.findById(validUserId)).thenReturn(Optional.of(user));
        when(postRepository.findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4)))
                .thenReturn(recentPosts);
        when(commentRepository.countByPostId(any())).thenReturn(0L);
        when(postUtils.createPostResponseFromPost(any(), anyLong())).thenReturn(new PostResponseDTO());
        when(commentRepository.findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5)))
                .thenReturn(new ArrayList<>());
        when(postRepository.countByAuthor(user)).thenReturn(10L);
        when(commentRepository.countByAuthor(user.getUsername())).thenReturn(0L);
        when(userUtils.createUserProfileResponse(any(), anyList(), anyList(), anyLong(), anyLong()))
                .thenReturn(new UserProfileResponse(
                        userIdString,
                        user.getUsername(),
                        user.getEmail(),
                        0L,
                        0L,
                        List.of(), List.of()
                ));

        // Act
        userService.getUserProfile(userIdString);

        // Assert
        verify(postRepository).findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4));
        verify(postUtils, times(3)).createPostResponseFromPost(any(), anyLong());
    }

    @Test
    void getUserProfile_ShouldLimitRecentCommentsToThree() {
        // Arrange
        String userIdString = validUserId.toString();
        List<Comment> recentComments = List.of(testComment1, testComment2, testComment3);

        CommentResponse commentResponse = new CommentResponse();

        when(userRepository.findById(validUserId)).thenReturn(Optional.of(user));
        when(postRepository.findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4)))
                .thenReturn(new ArrayList<>());
        when(commentRepository.findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5)))
                .thenReturn(recentComments);
        when(postRepository.countByAuthor(user)).thenReturn(0L);
        when(commentRepository.countByAuthor(user.getUsername())).thenReturn(15L);
        when(userUtils.createUserProfileResponse(any(), anyList(), anyList(), anyLong(), anyLong()))
                .thenReturn(new UserProfileResponse(
                        userIdString,
                        user.getUsername(),
                        user.getEmail(),
                        0L,
                        0L,
                        List.of(), List.of()
                ));

        try (MockedStatic<CommentUtils> commentUtilsMock = mockStatic(CommentUtils.class)) {
            commentUtilsMock.when(() -> CommentUtils.createCommentResponseFromComment(any()))
                    .thenReturn(commentResponse);

            // Act
            userService.getUserProfile(userIdString);

            // Assert
            verify(commentRepository).findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5));
            commentUtilsMock.verify(() -> CommentUtils.createCommentResponseFromComment(any()), times(3));
        }
    }

    @Test
    void getUserProfile_ShouldCountCommentsForEachPost() {
        // Arrange
        String userIdString = validUserId.toString();
        List<Post> recentPosts = List.of(testPost1, testPost2);

        when(userRepository.findById(validUserId)).thenReturn(Optional.of(user));
        when(postRepository.findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4)))
                .thenReturn(recentPosts);
        when(commentRepository.countByPostId(testPost1.getId())).thenReturn(5L);
        when(commentRepository.countByPostId(testPost2.getId())).thenReturn(3L);
        when(postUtils.createPostResponseFromPost(any(), anyLong())).thenReturn(new PostResponseDTO());
        when(commentRepository.findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5)))
                .thenReturn(new ArrayList<>());
        when(postRepository.countByAuthor(user)).thenReturn(2L);
        when(commentRepository.countByAuthor(user.getUsername())).thenReturn(0L);
        when(userUtils.createUserProfileResponse(any(), anyList(), anyList(), anyLong(), anyLong()))
                .thenReturn(new UserProfileResponse(
                        userIdString,
                        user.getUsername(),
                        user.getEmail(),
                        0L,
                        0L,
                        List.of(), List.of()
                ));

        // Act
        userService.getUserProfile(userIdString);

        // Assert
        verify(commentRepository).countByPostId(testPost1.getId());
        verify(commentRepository).countByPostId(testPost2.getId());
        verify(postUtils).createPostResponseFromPost(testPost1, 5L);
        verify(postUtils).createPostResponseFromPost(testPost2, 3L);
    }

    @Test
    void getUserProfile_ShouldHandleUUIDWithDifferentFormats() {
        // Arrange
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String userIdString = uuid.toString();

        when(userRepository.findById(uuid)).thenReturn(Optional.of(user));
        when(postRepository.findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4)))
                .thenReturn(new ArrayList<>());
        when(commentRepository.findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5)))
                .thenReturn(new ArrayList<>());
        when(postRepository.countByAuthor(user)).thenReturn(0L);
        when(commentRepository.countByAuthor(user.getUsername())).thenReturn(0L);
        when(userUtils.createUserProfileResponse(any(), anyList(), anyList(), anyLong(), anyLong()))
                .thenReturn(new UserProfileResponse(
                        userIdString,
                        user.getUsername(),
                        user.getEmail(),
                        0L,
                        0L,
                        List.of(), List.of()
                ));

        // Act
        userService.getUserProfile(userIdString);

        // Assert
        verify(userRepository).findById(uuid);
    }

    @Test
    void getUserProfile_ShouldPassCorrectParametersToUserUtils() {
        // Arrange
        String userIdString = validUserId.toString();
        List<Post> recentPosts = List.of(testPost1);
        List<Comment> recentComments = List.of(testComment1);

        PostResponseDTO postResponse = new PostResponseDTO();
        CommentResponse commentResponse = new CommentResponse();
        UserProfileResponse expectedResponse = new UserProfileResponse(
                userIdString,
                user.getUsername(),
                user.getEmail(),
                0L,
                0L,
                List.of(), List.of()
        );

        when(userRepository.findById(validUserId)).thenReturn(Optional.of(user));
        when(postRepository.findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4)))
                .thenReturn(recentPosts);
        when(commentRepository.countByPostId(testPost1.getId())).thenReturn(5L);
        when(postUtils.createPostResponseFromPost(testPost1, 5L)).thenReturn(postResponse);
        when(commentRepository.findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5)))
                .thenReturn(recentComments);
        when(postRepository.countByAuthor(user)).thenReturn(10L);
        when(commentRepository.countByAuthor(user.getUsername())).thenReturn(25L);

        try (MockedStatic<CommentUtils> commentUtilsMock = mockStatic(CommentUtils.class)) {
            commentUtilsMock.when(() -> CommentUtils.createCommentResponseFromComment(testComment1))
                    .thenReturn(commentResponse);

            when(userUtils.createUserProfileResponse(
                    eq(user),
                    argThat(list -> list.size() == 1 && list.getFirst() == postResponse),
                    argThat(list -> list.size() == 1 && list.getFirst() == commentResponse),
                    eq(10L),
                    eq(25L)
            )).thenReturn(expectedResponse);

            // Act
            UserProfileResponse result = userService.getUserProfile(userIdString);

            // Assert
            assertThat(result).isEqualTo(expectedResponse);
            verify(userUtils).createUserProfileResponse(
                    eq(user),
                    argThat(list -> list.size() == 1),
                    argThat(list -> list.size() == 1),
                    eq(10L),
                    eq(25L)
            );
        }
    }
}