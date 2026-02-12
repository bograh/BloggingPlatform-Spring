package org.amalitech.bloggingplatformspring.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.UserProfileResponse;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.InvalidUserIdFormatException;
import org.amalitech.bloggingplatformspring.exceptions.UnauthorizedException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.security.JwtTokenProvider;
import org.amalitech.bloggingplatformspring.utils.CommentUtils;
import org.amalitech.bloggingplatformspring.utils.PostUtils;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public UserProfileResponse getUserProfile(HttpServletRequest httpServletRequest) {
        String token = jwtTokenProvider.getTokenFromRequest(httpServletRequest);
        String email = jwtTokenProvider.getEmailFromAccessToken(token);
        User user = userRepository.findUserByEmailIgnoreCase(email).orElseThrow(
                () -> new UnauthorizedException("Invalid email or password")
        );
        String userID = String.valueOf(user.getId());

        if (userID.isBlank())
            throw new BadRequestException("User ID cannot be empty");

        try {
            List<Post> recentPosts = postRepository.findPostsByAuthorOrderByUpdatedAtDesc(user, Limit.of(4));
            List<PostResponseDTO> recentPostsResponse = recentPosts.stream()
                    .map(post -> {
                        Long totalComments = commentRepository.countByPostId(post.getId());
                        return postUtils.createPostResponseFromPost(post, totalComments);
                    }).toList();

            List<Comment> recentComments = commentRepository.findCommentsByAuthorOrderByCommentedAtDesc(user.getUsername(), Limit.of(5));
            List<CommentResponse> recentCommentsResponse = recentComments.stream()
                    .map(CommentUtils::createCommentResponseFromComment).toList();

            Long totalPosts = postRepository.countByAuthor(user);
            Long totalComments = commentRepository.countByAuthor(user.getUsername());

            return userUtils.createUserProfileResponse(user, recentPostsResponse, recentCommentsResponse, totalPosts, totalComments);

        } catch (IllegalArgumentException e) {
            throw new InvalidUserIdFormatException("Invalid UUID format for userID");
        }

    }
}