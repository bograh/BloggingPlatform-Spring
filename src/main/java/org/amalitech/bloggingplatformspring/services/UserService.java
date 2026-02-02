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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserUtils userUtils;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostUtils postUtils;

    public UserService(UserRepository userRepository, UserUtils userUtils, PostRepository postRepository, CommentRepository commentRepository, PostUtils postUtils) {
        this.userRepository = userRepository;
        this.userUtils = userUtils;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postUtils = postUtils;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UserResponseDTO registerUser(RegisterUserDTO registerUserDTO) {
        String username = registerUserDTO.getUsername();
        String email = registerUserDTO.getEmail();
        String password = registerUserDTO.getPassword();

        if (password.toLowerCase().contains(username.toLowerCase())) {
            throw new BadRequestException("Password must not contain username");
        }

        if (Boolean.TRUE.equals(userRepository.existsByUsernameIgnoreCase(username))) {
            throw new BadRequestException("Username is taken");
        }

        if (Boolean.TRUE.equals(userRepository.existsByEmailIgnoreCase(email))) {
            throw new BadRequestException("Email is taken");
        }

        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(hashedPassword);
        userRepository.save(user);

        return userUtils.mapUserToUserResponse(user);
    }

    public UserResponseDTO signInUser(SignInUserDTO signInUserDTO) {
        String email = signInUserDTO.getEmail();
        String password = signInUserDTO.getPassword();

        if (Boolean.FALSE.equals(userRepository.existsByEmailIgnoreCase(email))) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findUserByEmailIgnoreCase(email).orElseThrow(
                () -> new UnauthorizedException("Invalid email or password")
        );

        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());
        if (!result.verified) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return userUtils.mapUserToUserResponse(user);

    }

    @Cacheable(cacheNames = "users", key = "'profile:' + #userID")
    public UserProfileResponse getUserProfile(String userID) {
        if (userID.isBlank())
            throw new BadRequestException("User ID cannot be empty");

        try {
            UUID id = UUID.fromString(userID);
            User user = userRepository.findById(id).orElseThrow(
                    () -> new ResourceNotFoundException("User not found with id: " + id)
            );
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