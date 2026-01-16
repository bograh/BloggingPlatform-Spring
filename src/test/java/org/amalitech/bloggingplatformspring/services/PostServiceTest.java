package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.dtos.requests.*;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.*;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService;

    private UUID userId;
    private Post post;
    private User user;
    private CreatePostDTO createPostDTO;
    private UpdatePostDTO updatePostDTO;
    private DeletePostRequestDTO deletePostRequestDTO;
    private PostResponseDTO postResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        post = new Post(
                1,
                "Test Title",
                "Test Body",
                userId,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        createPostDTO = new CreatePostDTO();
        createPostDTO.setAuthorId(userId.toString());
        createPostDTO.setTitle("Test Title");
        createPostDTO.setBody("Test Body");
        createPostDTO.setTags(Arrays.asList("tag1", "tag2"));

        updatePostDTO = new UpdatePostDTO();
        updatePostDTO.setAuthorId(userId.toString());
        updatePostDTO.setTitle("Updated Title");
        updatePostDTO.setBody("Updated Body");
        updatePostDTO.setTags(Arrays.asList("tag3", "tag4"));

        deletePostRequestDTO = new DeletePostRequestDTO();
        deletePostRequestDTO.setAuthorId(userId.toString());

        postResponseDTO = new PostResponseDTO();
        postResponseDTO.setId(1);
        postResponseDTO.setTitle("Test Title");
        postResponseDTO.setBody("Test Body");
        postResponseDTO.setAuthor("testuser");
        postResponseDTO.setTags(Arrays.asList("tag1", "tag2"));
    }

    @Test
    void createPost_Success() throws SQLException {
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(postRepository.savePost(createPostDTO)).thenReturn(post);

        PostResponseDTO result = postService.createPost(createPostDTO);

        assertNotNull(result);

        verify(userRepository).findUserById(userId);
        verify(postRepository).savePost(createPostDTO);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void createPost_UserNotFound_ThrowsResourceNotFoundException() throws SQLException {
        when(userRepository.findUserById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> postService.createPost(createPostDTO));
        verify(userRepository).findUserById(userId);
        verify(postRepository, never()).savePost(any());
    }

    @Test
    void createPost_InvalidUserId_ThrowsException() {
        createPostDTO.setAuthorId("invalid-uuid");

        assertThrows(BadRequestException.class, () -> postService.createPost(createPostDTO));
    }

    @Test
    void createPost_SQLException_ThrowsSQLQueryException() throws SQLException {
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(postRepository.savePost(createPostDTO)).thenThrow(new SQLException("Database error"));

        assertThrows(SQLQueryException.class, () -> postService.createPost(createPostDTO));
    }

    @Test
    void getPaginatedPosts_Success() throws SQLException {
        PageRequest pageRequest = new PageRequest(1, 10, "createdAt", "DESC");
        PostFilterRequest filterRequest = new PostFilterRequest(null, null, null);
        PageResponse<PostResponseDTO> expectedResponse = new PageResponse<>(
                Collections.singletonList(postResponseDTO),
                1,
                10,
                "createdAt",
                1
        );

        when(postRepository.getAllPosts(pageRequest, filterRequest)).thenReturn(expectedResponse);

        PageResponse<PostResponseDTO> result = postService.getPaginatedPosts(pageRequest, filterRequest);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(1, result.page());
        assertEquals(10, result.size());
        verify(postRepository).getAllPosts(pageRequest, filterRequest);
    }

    @Test
    void getPaginatedPosts_WithFilters_Success() throws SQLException {
        PageRequest pageRequest = new PageRequest(1, 10, "title", "ASC");
        PostFilterRequest filterRequest = new PostFilterRequest(
                "testuser",
                "test search",
                Arrays.asList("tag1", "tag2")
        );
        PageResponse<PostResponseDTO> expectedResponse = new PageResponse<>(
                Collections.singletonList(postResponseDTO),
                1,
                10,
                "title",
                1
        );

        when(postRepository.getAllPosts(pageRequest, filterRequest)).thenReturn(expectedResponse);

        PageResponse<PostResponseDTO> result = postService.getPaginatedPosts(pageRequest, filterRequest);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(postRepository).getAllPosts(pageRequest, filterRequest);
    }

    @Test
    void getPaginatedPosts_SQLException_ThrowsSQLQueryException() throws SQLException {
        PageRequest pageRequest = new PageRequest(1, 10, "createdAt", "DESC");
        PostFilterRequest filterRequest = new PostFilterRequest(null, null, null);

        when(postRepository.getAllPosts(pageRequest, filterRequest))
                .thenThrow(new SQLException("Database error"));

        assertThrows(SQLQueryException.class,
                () -> postService.getPaginatedPosts(pageRequest, filterRequest));
    }

    @Test
    void getAllPosts_Success() throws SQLException {
        List<PostResponseDTO> expectedPosts = Collections.singletonList(postResponseDTO);
        when(postRepository.getAllPosts()).thenReturn(expectedPosts);

        List<PostResponseDTO> result = postService.getAllPosts();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(postRepository).getAllPosts();
    }

    @Test
    void getAllPosts_SQLException_ThrowsSQLQueryException() throws SQLException {
        when(postRepository.getAllPosts()).thenThrow(new SQLException("Database error"));

        assertThrows(SQLQueryException.class, () -> postService.getAllPosts());
    }

    @Test
    void getPostById_Success() throws SQLException {
        when(postRepository.getPostResponseById(1)).thenReturn(Optional.of(postResponseDTO));

        PostResponseDTO result = postService.getPostById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(postRepository).getPostResponseById(1);
    }

    @Test
    void getPostById_InvalidId_ThrowsBadRequestException() throws SQLException {
        assertThrows(BadRequestException.class, () -> postService.getPostById(0));
        assertThrows(BadRequestException.class, () -> postService.getPostById(-1));
        verify(postRepository, never()).getPostResponseById(anyInt());
    }

    @Test
    void getPostById_NotFound_ThrowsResourceNotFoundException() throws SQLException {
        when(postRepository.getPostResponseById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> postService.getPostById(999));
    }

    @Test
    void getPostById_SQLException_ThrowsSQLQueryException() throws SQLException {
        when(postRepository.getPostResponseById(1)).thenThrow(new SQLException("Database error"));

        assertThrows(SQLQueryException.class, () -> postService.getPostById(1));
    }

    @Test
    void updatePost_Success() throws SQLException {
        when(postRepository.findPostById(1)).thenReturn(Optional.of(post));
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        doNothing().when(postRepository).updatePost(any(Post.class), anyList());

        PostResponseDTO result = postService.updatePost(1, updatePostDTO);

        assertNotNull(result);
        verify(postRepository).findPostById(1);
        verify(userRepository).findUserById(userId);
        verify(postRepository).updatePost(any(Post.class), anyList());
    }

    @Test
    void updatePost_PartialUpdate_Success() throws SQLException {
        UpdatePostDTO partialUpdate = new UpdatePostDTO();
        partialUpdate.setAuthorId(userId.toString());
        partialUpdate.setTitle("New Title");

        when(postRepository.findPostById(1)).thenReturn(Optional.of(post));
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(postRepository.getTagsByPostId(1)).thenReturn(Arrays.asList("tag1", "tag2"));
        doNothing().when(postRepository).updatePost(any(Post.class), anyList());

        PostResponseDTO result = postService.updatePost(1, partialUpdate);

        assertNotNull(result);
        verify(postRepository).getTagsByPostId(1);
    }

    @Test
    void updatePost_PostNotFound_ThrowsResourceNotFoundException() throws SQLException {
        when(postRepository.findPostById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> postService.updatePost(999, updatePostDTO));
        verify(userRepository, never()).findUserById(any());
    }

    @Test
    void updatePost_UserNotFound_ThrowsResourceNotFoundException() throws SQLException {
        when(postRepository.findPostById(1)).thenReturn(Optional.of(post));
        when(userRepository.findUserById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> postService.updatePost(1, updatePostDTO));
    }

    @Test
    void updatePost_UnauthorizedUser_ThrowsForbiddenException() throws SQLException {
        UUID differentUserId = UUID.randomUUID();
        updatePostDTO.setAuthorId(differentUserId.toString());

        User differentUser = new User();
        differentUser.setId(differentUserId);
        differentUser.setUsername("differentuser");

        when(postRepository.findPostById(1)).thenReturn(Optional.of(post));
        when(userRepository.findUserById(differentUserId)).thenReturn(Optional.of(differentUser));

        assertThrows(ForbiddenException.class,
                () -> postService.updatePost(1, updatePostDTO));
        verify(postRepository, never()).updatePost(any(), any());
    }

    @Test
    void updatePost_InvalidUserId_ThrowsInvalidUserIdFormatException() {
        updatePostDTO.setAuthorId("invalid-uuid");

        assertThrows(InvalidUserIdFormatException.class,
                () -> postService.updatePost(1, updatePostDTO));
    }

    @Test
    void updatePost_SQLException_ThrowsSQLQueryException() throws SQLException {
        when(postRepository.findPostById(1)).thenThrow(new SQLException("Database error"));

        assertThrows(SQLQueryException.class,
                () -> postService.updatePost(1, updatePostDTO));
    }

    @Test
    void updatePost_DuplicateTags_RemovesDuplicates() throws SQLException {
        updatePostDTO.setTags(Arrays.asList("tag1", "tag2", "tag1", "tag3", "tag2"));

        when(postRepository.findPostById(1)).thenReturn(Optional.of(post));
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        doNothing().when(postRepository).updatePost(any(Post.class), anyList());

        postService.updatePost(1, updatePostDTO);

        verify(postRepository).updatePost(any(Post.class), argThat(tags ->
                tags.size() == 3 && new HashSet<>(tags).size() == 3
        ));
    }

    @Test
    void deletePost_Success() throws SQLException {
        when(postRepository.findPostById(1)).thenReturn(Optional.of(post));
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        doNothing().when(postRepository).deletePost(1, userId);

        assertDoesNotThrow(() -> postService.deletePost(1, deletePostRequestDTO));

        verify(postRepository).findPostById(1);
        verify(userRepository).findUserById(userId);
        verify(postRepository).deletePost(1, userId);
    }

    @Test
    void deletePost_PostNotFound_ThrowsResourceNotFoundException() throws SQLException {
        when(postRepository.findPostById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> postService.deletePost(999, deletePostRequestDTO));
        verify(postRepository, never()).deletePost(anyInt(), any());
    }

    @Test
    void deletePost_UserNotFound_ThrowsResourceNotFoundException() throws SQLException {
        when(postRepository.findPostById(1)).thenReturn(Optional.of(post));
        when(userRepository.findUserById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> postService.deletePost(1, deletePostRequestDTO));
        verify(postRepository, never()).deletePost(anyInt(), any());
    }

    @Test
    void deletePost_UnauthorizedUser_ThrowsForbiddenException() throws SQLException {
        UUID differentUserId = UUID.randomUUID();
        deletePostRequestDTO.setAuthorId(differentUserId.toString());

        User differentUser = new User();
        differentUser.setId(differentUserId);

        when(postRepository.findPostById(1)).thenReturn(Optional.of(post));
        when(userRepository.findUserById(differentUserId)).thenReturn(Optional.of(differentUser));

        assertThrows(ForbiddenException.class,
                () -> postService.deletePost(1, deletePostRequestDTO));
        verify(postRepository, never()).deletePost(anyInt(), any());
    }

    @Test
    void deletePost_InvalidUserId_ThrowsInvalidUserIdFormatException() {
        deletePostRequestDTO.setAuthorId("invalid-uuid");

        assertThrows(InvalidUserIdFormatException.class,
                () -> postService.deletePost(1, deletePostRequestDTO));
    }

    @Test
    void deletePost_SQLException_ThrowsSQLQueryException() throws SQLException {
        when(postRepository.findPostById(1)).thenThrow(new SQLException("Database error"));

        assertThrows(SQLQueryException.class,
                () -> postService.deletePost(1, deletePostRequestDTO));
    }
}