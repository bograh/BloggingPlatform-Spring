package org.amalitech.bloggingplatformspring.services;

import lombok.extern.slf4j.Slf4j;
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
import org.amalitech.bloggingplatformspring.repository.TagRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.PostUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;
    private final PostUtils postUtils;
    private final TagService tagService;

    public PostService(PostRepository postRepository, UserRepository userRepository, CommentRepository commentRepository, TagRepository tagRepository, TagService tagService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.tagRepository = tagRepository;
        this.postUtils = new PostUtils();
        this.tagService = tagService;
    }

    public PostResponseDTO createPost(CreatePostDTO createPostDTO) {
        UUID userId;
        try {
            userId = UUID.fromString(createPostDTO.getAuthorId());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid authorId UUID format");
        }

        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found with ID: " + userId)
        );

        Post post = new Post();
        post.setTitle(createPostDTO.getTitle());
        post.setBody(createPostDTO.getBody());
        post.setAuthor(user);
        post.setTags(new HashSet<>());
        postRepository.save(post);

        return postUtils.createResponseFromPostAndTags(
                post,
                user.getUsername(),
                createPostDTO.getTags(),
                0L
        );

    }

    public PageResponse<PostResponseDTO> getAllPosts(int page, int size, String sortBy, String order, PostFilterRequest postFilterRequest) {
        size = Math.min(size, 30);
        String entitySortField = postUtils.mapSortField(sortBy);
        String orderBy = postUtils.mapOrderField(order);
        Sort sort = Sort.by(Sort.Direction.fromString(orderBy), entitySortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Post> postPage;

        if (postFilterRequest.author() != null) {
            postPage = postRepository.findByAuthor_UsernameIgnoreCase(postFilterRequest.author(), pageable);
            return postUtils.mapPostPageToPostResponsePage(postPage, commentRepository);
        }

        if (postFilterRequest.search() != null) {
            postPage = postRepository.search(postFilterRequest.search(), pageable);
            return postUtils.mapPostPageToPostResponsePage(postPage, commentRepository);
        }

        if (!postFilterRequest.tags().isEmpty()) {
            // TODO: Change this to iterate the tags List in the query
            postPage = postRepository.search(postFilterRequest.tags().getFirst(), pageable);
            return postUtils.mapPostPageToPostResponsePage(postPage, commentRepository);
        }

        postPage = postRepository.findAll(pageable);
        List<Post> posts = postPage.getContent();

        List<PostResponseDTO> postsResponse = posts.stream()
                .map(post -> {
                    Long totalComments = commentRepository.countByPostId(post.getId());
                    return postUtils.createPostResponseFromPost(post, totalComments);
                })
                .toList();

        return new PageResponse<>(
                postsResponse,
                page,
                size,
                sort.toString(),
                postPage.getTotalElements()
        );
    }

    public PostResponseDTO getPostById(Long postId) {
        if (postId <= 0) {
            throw new BadRequestException("Post ID must be a positive number");
        }

        Post post = postRepository.findPostById(postId).orElseThrow(
                () -> new ResourceNotFoundException("Post not found with id: " + postId)
        );

        Long totalComments = commentRepository.countByPostId(postId);
        return postUtils.createPostResponseFromPost(post, totalComments);
    }

    public PostResponseDTO updatePost(Long postId, UpdatePostDTO updatePostDTO) {
        try {
            UUID userID = UUID.fromString(updatePostDTO.getAuthorId());

            Post post = postRepository.findPostById(postId).orElseThrow(
                    () -> new ResourceNotFoundException("Post with ID: " + postId + " not found.")
            );

            User user = userRepository.findById(userID).orElseThrow(
                    () -> new ResourceNotFoundException("User not found with ID: " + userID)
            );

            if (!user.getId().equals(post.getAuthor().getId())) {
                throw new ForbiddenException("You are not permitted to edit this post.");
            }

            if (updatePostDTO.getTitle() != null) {
                post.setTitle(updatePostDTO.getTitle());
            }

            if (updatePostDTO.getBody() != null) {
                post.setBody(updatePostDTO.getBody());
            }

            if (updatePostDTO.getTags() != null) {
                Set<Tag> updatedTags = tagService.getOrCreateTags(updatePostDTO.getTags());
                post.setTags(updatedTags);
            }

            Post savedPost = postRepository.save(post);
            long totalComments = commentRepository.countByPostId(savedPost.getId());

            return postUtils.createPostResponseFromPost(savedPost, totalComments);

        } catch (IllegalArgumentException e) {
            throw new InvalidUserIdFormatException("Invalid user ID format: " + e.getMessage());
        }
    }

    public void deletePost(Long postId, DeletePostRequestDTO deletePostRequestDTO) {
        try {
            UUID userID = UUID.fromString(deletePostRequestDTO.getAuthorId());

            Post post = postRepository.findPostById(postId).orElseThrow(
                    () -> new ResourceNotFoundException("Post with ID: " + postId + " not found.")
            );

            User user = userRepository.findById(userID).orElseThrow(
                    () -> new ResourceNotFoundException("User not found with username: " + userID));

            if (!user.getId().equals(post.getAuthor().getId())) {
                throw new ForbiddenException("You are not permitted to delete this post.");
            }

            postRepository.delete(post);

        } catch (IllegalArgumentException e) {
            throw new InvalidUserIdFormatException("Invalid user ID format: " + e.getMessage());
        }
    }
}