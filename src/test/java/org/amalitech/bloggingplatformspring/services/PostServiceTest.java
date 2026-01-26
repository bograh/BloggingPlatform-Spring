package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.dtos.requests.CreatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.DeletePostRequestDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.PostFilterRequest;
import org.amalitech.bloggingplatformspring.dtos.requests.UpdatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.Tag;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.ForbiddenException;
import org.amalitech.bloggingplatformspring.exceptions.InvalidUserIdFormatException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.PostUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostUtils postUtils;

    @Mock
    private TagService tagService;

    @InjectMocks
    private PostService postService;

    private UUID userId;
    private User user;
    private Post post;
    private CreatePostDTO createPostDTO;
    private UpdatePostDTO updatePostDTO;
    private DeletePostRequestDTO deletePostRequestDTO;
    private PostResponseDTO postResponseDTO;
    private Set<Tag> tags;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        post = new Post();
        post.setId(1L);
        post.setTitle("Test Post");
        post.setBody("Test Body");
        post.setAuthor(user);
        post.setTags(new HashSet<>());

        createPostDTO = new CreatePostDTO();
        createPostDTO.setAuthorId(userId.toString());
        createPostDTO.setTitle("Test Post");
        createPostDTO.setBody("Test Body");
        createPostDTO.setTags(Arrays.asList("tag1", "tag2"));

        updatePostDTO = new UpdatePostDTO();
        updatePostDTO.setAuthorId(userId.toString());
        updatePostDTO.setTitle("Updated Title");
        updatePostDTO.setBody("Updated Body");
        updatePostDTO.setTags(List.of("tag3"));

        deletePostRequestDTO = new DeletePostRequestDTO();
        deletePostRequestDTO.setAuthorId(userId.toString());

        postResponseDTO = new PostResponseDTO();
        postResponseDTO.setId(1L);
        postResponseDTO.setTitle("Test Post");
        postResponseDTO.setBody("Test Body");
        postResponseDTO.setAuthor("testuser");

        Tag tag1 = new Tag();
        tag1.setName("tag1");
        Tag tag2 = new Tag();
        tag2.setName("tag2");
        tags = new HashSet<>(Arrays.asList(tag1, tag2));
    }


    @Test
    void createPost_WithValidData_ShouldReturnPostResponseDTO() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tagService.getOrCreateTags(anyList())).thenReturn(tags);
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(postUtils.createResponseFromPostAndTags(any(Post.class), anyString(), anyList(), anyLong()))
                .thenReturn(postResponseDTO);

        PostResponseDTO result = postService.createPost(createPostDTO);

        assertNotNull(result);
        assertEquals(postResponseDTO.getId(), result.getId());
        assertEquals(postResponseDTO.getTitle(), result.getTitle());

        verify(userRepository).findById(userId);
        verify(tagService).getOrCreateTags(createPostDTO.getTags());
        verify(postRepository).save(any(Post.class));
        verify(postUtils).createResponseFromPostAndTags(any(Post.class), eq("testuser"), eq(createPostDTO.getTags()), eq(0L));
    }

    @Test
    void createPost_WithNoTags_ShouldCreatePostWithEmptyTags() {
        createPostDTO.setTags(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(postUtils.createResponseFromPostAndTags(any(Post.class), anyString(), isNull(), anyLong()))
                .thenReturn(postResponseDTO);

        PostResponseDTO result = postService.createPost(createPostDTO);

        assertNotNull(result);
        verify(tagService, never()).getOrCreateTags(anyList());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void createPost_WithEmptyTagsList_ShouldCreatePostWithEmptyTags() {
        createPostDTO.setTags(Collections.emptyList());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(postUtils.createResponseFromPostAndTags(any(Post.class), anyString(), anyList(), anyLong()))
                .thenReturn(postResponseDTO);

        PostResponseDTO result = postService.createPost(createPostDTO);

        assertNotNull(result);
        verify(tagService, never()).getOrCreateTags(anyList());
    }

    @Test
    void createPost_WithInvalidUUID_ShouldThrowBadRequestException() {
        createPostDTO.setAuthorId("invalid-uuid");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> postService.createPost(createPostDTO)
        );

        assertEquals("Invalid authorId UUID format", exception.getMessage());
        verify(userRepository, never()).findById(any());
        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_WithNonExistentUser_ShouldThrowResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> postService.createPost(createPostDTO)
        );

        assertTrue(exception.getMessage().contains("User not found with ID"));
        verify(userRepository).findById(userId);
        verify(postRepository, never()).save(any());
    }


    @Test
    void getAllPosts_WithValidParameters_ShouldReturnPageResponse() {
        int page = 0;
        int size = 10;
        String sortBy = "createdAt";
        String order = "desc";
        PostFilterRequest filterRequest = new PostFilterRequest(
                user.getUsername(),
                "search",
                List.of()
        );

        List<Post> posts = Collections.singletonList(post);
        Page<Post> postPage = new PageImpl<>(posts, PageRequest.of(page, size), 1);
        PageResponse<PostResponseDTO> expectedResponse = new PageResponse<>(
                Collections.singletonList(postResponseDTO),
                postPage.getNumber(),
                postPage.getNumberOfElements(),
                postPage.getSort().toString(),
                postPage.getTotalElements()
        );

        when(postUtils.mapSortField(sortBy)).thenReturn("createdAt");
        when(postUtils.mapOrderField(order)).thenReturn("DESC");
        when(postUtils.buildSpecification(filterRequest)).thenReturn(Specification.unrestricted());
        when(postRepository.findAll(ArgumentMatchers.<Specification<Post>>any(), any(Pageable.class))).thenReturn(postPage);
        when(postUtils.mapPostPageToPostResponsePage(postPage)).thenReturn(expectedResponse);

        PageResponse<PostResponseDTO> result = postService.getAllPosts(page, size, sortBy, order, filterRequest);

        assertNotNull(result);
        verify(postUtils).mapSortField(sortBy);
        verify(postUtils).mapOrderField(order);
        verify(postUtils).buildSpecification(filterRequest);
        verify(postRepository).findAll(ArgumentMatchers.<Specification<Post>>any(), any(Pageable.class));
        verify(postUtils).mapPostPageToPostResponsePage(postPage);
    }

    @Test
    void getAllPosts_WithSizeGreaterThan30_ShouldLimitTo30() {
        int page = 0;
        int size = 50;
        PostFilterRequest filterRequest = new PostFilterRequest(null, null, List.of());

        when(postUtils.mapSortField(anyString())).thenReturn("createdAt");
        when(postUtils.mapOrderField(anyString())).thenReturn("DESC");
        when(postUtils.buildSpecification(any())).thenReturn(Specification.unrestricted());
        when(postRepository.findAll(ArgumentMatchers.<Specification<Post>>any(), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(postUtils.mapPostPageToPostResponsePage(any())).thenReturn(new PageResponse<>(List.of(), 0, 0, "", 0));

        postService.getAllPosts(page, size, "createdAt", "desc", filterRequest);

        verify(postRepository).findAll(ArgumentMatchers.<Specification<Post>>any(), argThat(
                (Pageable p) -> p.getPageSize() == 30
        ));
    }


    @Test
    void getPostById_WithValidId_ShouldReturnPostResponseDTO() {
        Long postId = 1L;
        when(postRepository.findPostById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.countByPostId(postId)).thenReturn(5L);
        when(postUtils.createPostResponseFromPost(post, 5L)).thenReturn(postResponseDTO);

        PostResponseDTO result = postService.getPostById(postId);

        assertNotNull(result);
        assertEquals(postResponseDTO.getId(), result.getId());
        verify(postRepository).findPostById(postId);
        verify(commentRepository).countByPostId(postId);
        verify(postUtils).createPostResponseFromPost(post, 5L);
    }

    @Test
    void getPostById_WithNegativeId_ShouldThrowBadRequestException() {
        Long postId = -1L;

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> postService.getPostById(postId)
        );

        assertEquals("Post ID must be a positive number", exception.getMessage());
        verify(postRepository, never()).findPostById(anyLong());
    }

    @Test
    void getPostById_WithZeroId_ShouldThrowBadRequestException() {
        Long postId = 0L;

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> postService.getPostById(postId)
        );

        assertEquals("Post ID must be a positive number", exception.getMessage());
    }

    @Test
    void getPostById_WithNonExistentId_ShouldThrowResourceNotFoundException() {
        Long postId = 999L;
        when(postRepository.findPostById(postId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> postService.getPostById(postId)
        );

        assertTrue(exception.getMessage().contains("Post not found with id"));
        verify(postRepository).findPostById(postId);
    }


    @Test
    void updatePost_WithValidData_ShouldReturnUpdatedPost() {
        Long postId = 1L;
        when(postRepository.findPostById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tagService.getOrCreateTags(anyList())).thenReturn(tags);
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(commentRepository.countByPostId(postId)).thenReturn(3L);
        when(postUtils.createPostResponseFromPost(any(Post.class), anyLong())).thenReturn(postResponseDTO);

        PostResponseDTO result = postService.updatePost(postId, updatePostDTO);

        assertNotNull(result);
        verify(postRepository).findPostById(postId);
        verify(userRepository).findById(userId);
        verify(tagService).getOrCreateTags(updatePostDTO.getTags());
        verify(postRepository).save(any(Post.class));
        verify(commentRepository).countByPostId(postId);
    }

    @Test
    void updatePost_WithBlankTitle_ShouldNotUpdateTitle() {
        Long postId = 1L;
        updatePostDTO.setTitle("");
        String originalTitle = post.getTitle();

        when(postRepository.findPostById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tagService.getOrCreateTags(anyList())).thenReturn(tags);
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(commentRepository.countByPostId(postId)).thenReturn(0L);
        when(postUtils.createPostResponseFromPost(any(Post.class), anyLong())).thenReturn(postResponseDTO);

        postService.updatePost(postId, updatePostDTO);

        assertEquals(originalTitle, post.getTitle());
    }

    @Test
    void updatePost_WithBlankBody_ShouldNotUpdateBody() {
        Long postId = 1L;
        updatePostDTO.setBody("   ");
        String originalBody = post.getBody();

        when(postRepository.findPostById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tagService.getOrCreateTags(anyList())).thenReturn(tags);
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(commentRepository.countByPostId(postId)).thenReturn(0L);
        when(postUtils.createPostResponseFromPost(any(Post.class), anyLong())).thenReturn(postResponseDTO);

        postService.updatePost(postId, updatePostDTO);

        assertEquals(originalBody, post.getBody());
    }

    @Test
    void updatePost_WithEmptyTags_ShouldNotUpdateTags() {
        Long postId = 1L;
        updatePostDTO.setTags(Collections.emptyList());

        when(postRepository.findPostById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(commentRepository.countByPostId(postId)).thenReturn(0L);
        when(postUtils.createPostResponseFromPost(any(Post.class), anyLong())).thenReturn(postResponseDTO);

        postService.updatePost(postId, updatePostDTO);

        verify(tagService, never()).getOrCreateTags(anyList());
    }

    @Test
    void updatePost_WithNonExistentPost_ShouldThrowResourceNotFoundException() {
        Long postId = 999L;
        when(postRepository.findPostById(postId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> postService.updatePost(postId, updatePostDTO)
        );

        assertTrue(exception.getMessage().contains("Post with ID: " + postId + " not found"));
        verify(postRepository).findPostById(postId);
        verify(postRepository, never()).save(any());
    }

    @Test
    void updatePost_WithNonExistentUser_ShouldThrowResourceNotFoundException() {
        Long postId = 1L;
        when(postRepository.findPostById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> postService.updatePost(postId, updatePostDTO)
        );

        assertTrue(exception.getMessage().contains("User not found with ID"));
        verify(postRepository, never()).save(any());
    }

    @Test
    void updatePost_WithUnauthorizedUser_ShouldThrowForbiddenException() {
        Long postId = 1L;
        UUID differentUserId = UUID.randomUUID();
        User differentUser = new User();
        differentUser.setId(differentUserId);

        when(postRepository.findPostById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(differentUserId)).thenReturn(Optional.of(differentUser));
        updatePostDTO.setAuthorId(differentUserId.toString());

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> postService.updatePost(postId, updatePostDTO)
        );

        assertEquals("You are not permitted to edit this post.", exception.getMessage());
        verify(postRepository, never()).save(any());
    }

    @Test
    void updatePost_WithInvalidUUID_ShouldThrowInvalidUserIdFormatException() {
        Long postId = 1L;
        updatePostDTO.setAuthorId("invalid-uuid");

        InvalidUserIdFormatException exception = assertThrows(
                InvalidUserIdFormatException.class,
                () -> postService.updatePost(postId, updatePostDTO)
        );

        assertTrue(exception.getMessage().contains("Invalid user ID format"));
        verify(postRepository, never()).save(any());
    }


    @Test
    void deletePost_WithValidData_ShouldDeletePost() {
        Long postId = 1L;
        when(postRepository.findPostById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        postService.deletePost(postId, deletePostRequestDTO);

        verify(postRepository).findPostById(postId);
        verify(userRepository).findById(userId);
        verify(postRepository).delete(post);
    }

    @Test
    void deletePost_WithNonExistentPost_ShouldThrowResourceNotFoundException() {
        Long postId = 999L;
        when(postRepository.findPostById(postId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> postService.deletePost(postId, deletePostRequestDTO)
        );

        assertTrue(exception.getMessage().contains("Post with ID: " + postId + " not found"));
        verify(postRepository, never()).delete((Post) any());
    }

    @Test
    void deletePost_WithNonExistentUser_ShouldThrowResourceNotFoundException() {
        Long postId = 1L;
        when(postRepository.findPostById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> postService.deletePost(postId, deletePostRequestDTO)
        );

        assertTrue(exception.getMessage().contains("User not found with username"));
        verify(postRepository, never()).delete((Post) any());
    }

    @Test
    void deletePost_WithUnauthorizedUser_ShouldThrowForbiddenException() {
        Long postId = 1L;
        UUID differentUserId = UUID.randomUUID();
        User differentUser = new User();
        differentUser.setId(differentUserId);

        when(postRepository.findPostById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(differentUserId)).thenReturn(Optional.of(differentUser));
        deletePostRequestDTO.setAuthorId(differentUserId.toString());

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> postService.deletePost(postId, deletePostRequestDTO)
        );

        assertEquals("You are not permitted to delete this post.", exception.getMessage());
        verify(postRepository, never()).delete((Post) any());
    }

    @Test
    void deletePost_WithInvalidUUID_ShouldThrowInvalidUserIdFormatException() {
        Long postId = 1L;
        deletePostRequestDTO.setAuthorId("invalid-uuid");

        InvalidUserIdFormatException exception = assertThrows(
                InvalidUserIdFormatException.class,
                () -> postService.deletePost(postId, deletePostRequestDTO)
        );

        assertTrue(exception.getMessage().contains("Invalid user ID format"));
        verify(postRepository, never()).delete((Post) any());
    }
}