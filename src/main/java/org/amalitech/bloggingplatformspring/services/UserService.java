package org.amalitech.bloggingplatformspring.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.responses.*;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.enums.AuthProvider;
import org.amalitech.bloggingplatformspring.enums.UserRoles;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.exceptions.UnauthorizedException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostCommentCountProjection;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.CommentUtils;
import org.amalitech.bloggingplatformspring.utils.PostUtils;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserUtils userUtils;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostUtils postUtils;
    private final CommentUtils commentUtils;
    @Qualifier("applicationTaskExecutor")
    private final Executor applicationTaskExecutor;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(HttpServletRequest httpServletRequest) {
        User user = userUtils.getUserFromRequest(httpServletRequest);
        String userId = String.valueOf(user.getId());

        if (userId.isBlank())
            throw new BadRequestException("User ID cannot be empty");

        CompletableFuture<List<PostResponseDTO>> recentPostsFuture = getRecentPostsResponseAsync(user);
        CompletableFuture<List<CommentResponse>> recentCommentsFuture = getRecentCommentsResponseAsync(user);
        CompletableFuture<Long> totalPostsFuture = getTotalPostsAsync(user);
        CompletableFuture<Long> totalCommentsFuture = getTotalCommentsAsync(user.getUsername());

        CompletableFuture.allOf(
                recentPostsFuture,
                recentCommentsFuture,
                totalPostsFuture,
                totalCommentsFuture).join();

        List<PostResponseDTO> recentPostsResponse = recentPostsFuture.join();
        List<CommentResponse> recentCommentsResponse = recentCommentsFuture.join();
        Long totalPosts = totalPostsFuture.join();
        Long totalComments = totalCommentsFuture.join();

        return userUtils.createUserProfileResponse(user, recentPostsResponse, recentCommentsResponse, totalPosts,
                totalComments);

    }

    private CompletableFuture<List<PostResponseDTO>> getRecentPostsResponseAsync(User user) {
        return CompletableFuture.supplyAsync(() -> {
            List<Post> recentPosts = postRepository.findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4));
            if (recentPosts.isEmpty()) {
                return List.of();
            }

            List<Long> postIds = recentPosts.stream()
                    .map(Post::getId)
                    .toList();

            Map<Long, Long> commentCountsByPostId = commentRepository.countCommentsByPostIds(postIds)
                    .stream()
                    .collect(Collectors.toMap(
                            PostCommentCountProjection::getPostId,
                            PostCommentCountProjection::getTotalComments));

            return recentPosts.stream()
                    .map(post -> postUtils.createPostResponseFromPost(
                            post,
                            commentCountsByPostId.getOrDefault(post.getId(), 0L)))
                    .toList();
        }, applicationTaskExecutor);
    }

    private CompletableFuture<List<CommentResponse>> getRecentCommentsResponseAsync(User user) {
        return CompletableFuture.supplyAsync(() -> {
            List<Comment> recentComments = commentRepository
                    .findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5));
            return recentComments.stream()
                    .map(commentUtils::createCommentResponseFromComment)
                    .toList();
        }, applicationTaskExecutor);
    }

    private CompletableFuture<Long> getTotalPostsAsync(User user) {
        return CompletableFuture.supplyAsync(() -> postRepository.countByAuthor(user), applicationTaskExecutor);
    }

    private CompletableFuture<Long> getTotalCommentsAsync(String username) {
        return CompletableFuture.supplyAsync(() -> commentRepository.countByAuthor(username), applicationTaskExecutor);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponseDTO> getAllUsers(int page, int size, String sortBy, String order, String search) {
        Pageable pageable = userUtils.createPageable(page, Math.min(size, 30), sortBy, order);

        Page<User> users = hasSearchTerm(search)
                ? userRepository.search(search, pageable)
                : userRepository.findAll(pageable);

        return userUtils.mapUserPageToUserResponsePage(users);
    }

    @Transactional(readOnly = true)
    public UserProfileSummary getUserSummary(String userId) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow(
                () -> new ResourceNotFoundException("User not found with id: " + userId));

        Long totalPosts = postRepository.countByAuthor(user);
        Long totalComments = commentRepository.countByAuthor(user.getUsername());
        return userUtils.createUserProfileSummary(user, totalPosts, totalComments);
    }

    public User processOAuth2User(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String username = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");

        if (email == null) {
            throw new UnauthorizedException("Email not found from OAuth2 provider");
        }

        return userRepository.findUserByEmailIgnoreCase(email)
                .map(existingUser -> updateExistingUser(existingUser, username, providerId))
                .orElseGet(() -> createNewUser(email, username, providerId));

    }

    private User updateExistingUser(User user, String name, String providerId) {
        user.setUsername(name);
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setOauth2ProviderId(providerId);
        return userRepository.save(user);
    }

    private User createNewUser(String email, String name, String providerId) {
        User user = new User();
        user.setUsername(name);
        user.setEmail(email);
        user.setOauth2ProviderId(providerId);
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setUserRoles(new ArrayList<>(Arrays.asList(
                UserRoles.READER,
                UserRoles.AUTHOR)));
        return userRepository.save(user);
    }

    private boolean hasSearchTerm(String search) {
        return search != null && !search.isBlank();
    }
}