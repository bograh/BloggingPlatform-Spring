package org.amalitech.bloggingplatformspring.services;

import jakarta.servlet.http.HttpServletRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.*;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.enums.AuthProvider;
import org.amalitech.bloggingplatformspring.enums.UserRoles;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.exceptions.UnauthorizedException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.security.JwtTokenProvider;
import org.amalitech.bloggingplatformspring.utils.CommentUtils;
import org.amalitech.bloggingplatformspring.utils.PostUtils;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserUtils userUtils;

  @Mock
  private PostRepository postRepository;

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private PostUtils postUtils;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private CommentUtils commentUtils;

  @Mock
  private Executor applicationTaskExecutor;

  @Mock
  private HttpServletRequest request;

  @InjectMocks
  private UserService userService;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername("testuser");
    user.setEmail("test@example.com");
    user.setUserRoles(new ArrayList<>(List.of(UserRoles.AUTHOR, UserRoles.READER)));

    lenient().doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(0);
      runnable.run();
      return null;
    }).when(applicationTaskExecutor).execute(any(Runnable.class));
  }

  @Test
  void getUserProfile_shouldReturnMappedProfile() {
    Post post = new Post();
    post.setId(1L);

    Comment comment = new Comment();

    PostResponseDTO postResponse = new PostResponseDTO();
    CommentResponse commentResponse = new CommentResponse();

    UserProfileResponse profileResponse = new UserProfileResponse(
        String.valueOf(user.getId()),
        user.getUsername(),
        user.getEmail(),
        1L,
        1L,
        user.getUserRoles(),
        List.of(postResponse),
        List.of(commentResponse));

    when(userUtils.getUserFromRequest(request)).thenReturn(user);
    when(postRepository.findPostsByAuthorOrderByUpdatedAtDesc(eq(user), any(Limit.class))).thenReturn(List.of(post));
    when(commentRepository.countByPostId(1L)).thenReturn(1L);
    when(postUtils.createPostResponseFromPost(eq(post), eq(1L))).thenReturn(postResponse);
    when(commentRepository.findCommentsByAuthorOrderByCommentedAtDesc(eq(user.getUsername()), any(Limit.class)))
        .thenReturn(List.of(comment));
    when(commentUtils.createCommentResponseFromComment(comment)).thenReturn(commentResponse);
    when(postRepository.countByAuthor(user)).thenReturn(1L);
    when(commentRepository.countByAuthor(user.getUsername())).thenReturn(1L);
    when(userUtils.createUserProfileResponse(eq(user), anyList(), anyList(), eq(1L), eq(1L)))
        .thenReturn(profileResponse);

    UserProfileResponse result = userService.getUserProfile(request);

    assertNotNull(result);
    assertEquals(user.getUsername(), result.username());
    assertEquals(user.getEmail(), result.email());
    assertEquals(1L, result.totalPosts());
    assertEquals(1L, result.totalComments());
  }

  @Test
  void getAllUsers_shouldReturnMappedPageResponse() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<User> page = new PageImpl<>(List.of(user), pageable, 1);

    UserResponseDTO dto = new UserResponseDTO();
    dto.setId(String.valueOf(user.getId()));
    dto.setUsername(user.getUsername());
    dto.setEmail(user.getEmail());

    PageResponse<UserResponseDTO> mapped = new PageResponse<>(
        List.of(dto),
        0,
        10,
        "createdAt: DESC",
        1,
        true);

    when(userUtils.createPageable(0, 10, "createdAt", "desc")).thenReturn(pageable);
    when(userRepository.findAll(pageable)).thenReturn(page);
    when(userUtils.mapUserPageToUserResponsePage(page)).thenReturn(mapped);

    PageResponse<UserResponseDTO> result = userService.getAllUsers(0, 10, "createdAt", "desc", "");

    assertNotNull(result);
    assertEquals(1, result.content().size());
    assertEquals("testuser", result.content().getFirst().getUsername());
  }

  @Test
  void getUserSummary_shouldThrowWhenUserDoesNotExist() {
    String missingId = UUID.randomUUID().toString();
    when(userRepository.findById(UUID.fromString(missingId))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> userService.getUserSummary(missingId));
  }

  @Test
  void processOAuth2User_shouldThrowWhenEmailMissing() {
    OAuth2User oauth2User = mock(OAuth2User.class);
    when(oauth2User.getAttribute("email")).thenReturn(null);

    assertThrows(UnauthorizedException.class, () -> userService.processOAuth2User(oauth2User));
  }

  @Test
  void processOAuth2User_shouldCreateNewUserWhenNotFound() {
    OAuth2User oauth2User = mock(OAuth2User.class);
    when(oauth2User.getAttribute("email")).thenReturn("new@example.com");
    when(oauth2User.getAttribute("name")).thenReturn("New User");
    when(oauth2User.getAttribute("sub")).thenReturn("provider-123");

    when(userRepository.findUserByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User result = userService.processOAuth2User(oauth2User);

    assertNotNull(result);
    assertEquals("new@example.com", result.getEmail());
    assertEquals("New User", result.getUsername());
    assertEquals(AuthProvider.GOOGLE, result.getAuthProvider());
    assertNotNull(result.getUserRoles());
    assertTrue(result.getUserRoles().contains(UserRoles.READER));
    assertTrue(result.getUserRoles().contains(UserRoles.AUTHOR));
  }
}
