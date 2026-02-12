package org.amalitech.bloggingplatformspring.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.responses.*;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.security.JwtTokenProvider;
import org.amalitech.bloggingplatformspring.utils.CommentUtils;
import org.amalitech.bloggingplatformspring.utils.PostUtils;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserUtils userUtils;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostUtils postUtils;
    private final JwtTokenProvider jwtTokenProvider;
    private final CommentUtils commentUtils;

    public UserProfileResponse getUserProfile(HttpServletRequest httpServletRequest) {
        User user = userUtils.getUserFromRequest(httpServletRequest);
        String userID = String.valueOf(user.getId());

        if (userID.isBlank())
            throw new BadRequestException("User ID cannot be empty");

        List<Post> recentPosts = postRepository.findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4));
        List<PostResponseDTO> recentPostsResponse = recentPosts.stream()
                .map(post -> {
                    Long totalComments = commentRepository.countByPostId(post.getId());
                    return postUtils.createPostResponseFromPost(post, totalComments);
                }).toList();

        List<Comment> recentComments = commentRepository.findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5));
        List<CommentResponse> recentCommentsResponse = recentComments.stream()
                .map(commentUtils::createCommentResponseFromComment).toList();

        Long totalPosts = postRepository.countByAuthor(user);
        Long totalComments = commentRepository.countByAuthor(user.getUsername());

        return userUtils.createUserProfileResponse(user, recentPostsResponse, recentCommentsResponse, totalPosts, totalComments);

    }

    public PageResponse<UserResponseDTO> getAllUsers(int page, int size, String sortBy, String order, String search) {
        Pageable pageable = userUtils.createPageable(page, Math.min(size, 30), sortBy, order);

        Page<User> users = hasSearchTerm(search)
                ? userRepository.search(search, pageable)
                : userRepository.findAll(pageable);

        return userUtils.mapUserPageToUserResponsePage(users);
    }

    public UserProfileSummary getUserSummary(String userId) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow(
                () -> new ResourceNotFoundException("User not found with id: " + userId)
        );

        Long totalPosts = postRepository.countByAuthor(user);
        Long totalComments = commentRepository.countByAuthor(user.getUsername());
        return userUtils.createUserProfileSummary(user, totalPosts, totalComments);
    }


    private boolean hasSearchTerm(String search) {
        return search != null && !search.isBlank();
    }
}