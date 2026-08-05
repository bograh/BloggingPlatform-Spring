package org.amalitech.bloggingplatformspring.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.requests.CommentFilterRequest;
import org.amalitech.bloggingplatformspring.dtos.requests.CreateCommentDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.DeleteCommentRequestDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.ForbiddenException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.utils.CommentUtils;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserUtils userUtils;
    private final CommentUtils commentUtils;
    private final PostRankingIndexService postRankingIndexService;
    private final NotificationQueueService notificationQueueService;

    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.COMMENTS_CACHE_NAME, key = "'post:' + #newComment.postId"),
            @CacheEvict(cacheNames = Constants.POSTS_CACHE_NAME, key = "#newComment.postId"),
            @CacheEvict(cacheNames = Constants.POST_LIST_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.POPULAR_POSTS_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.TRENDING_POSTS_CACHE_NAME, allEntries = true)
    })
    public CommentResponse addCommentToPost(CreateCommentDTO newComment, HttpServletRequest request) {
        User user = userUtils.getUserFromRequest(request);

        Comment comment = new Comment();
        comment.setContent(newComment.getCommentContent());
        comment.setPostId(newComment.getPostId());
        comment.setCommentedAt(LocalDateTime.now());
        comment.setAuthorId(String.valueOf(user.getId()));
        comment.setAuthor(user.getUsername());
        commentRepository.save(comment);

        postRepository.findPostById(newComment.getPostId()).ifPresent(post -> {
            if (!String.valueOf(post.getAuthor().getId()).equals(String.valueOf(user.getId()))) {
                notificationQueueService.queueNewCommentNotification(
                        post, user.getUsername(), newComment.getCommentContent());
            }
        });

        postRankingIndexService.rebuildIndexes();

        return commentUtils.createCommentResponseFromComment(comment);

    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = Constants.COMMENTS_CACHE_NAME, key = "'post:' + #postId")
    public List<CommentResponse> getAllCommentsByPostId(Long postId) {
        postRepository.findPostById(postId).orElseThrow(
                () -> new ResourceNotFoundException("Post not found with ID: " + postId));
        List<Comment> comments = commentRepository.findByPostIdOrderByCommentedAtDesc(postId);

        return comments.stream()
                .map(commentUtils::createCommentResponseFromComment)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = Constants.COMMENTS_CACHE_NAME, key = "#commentId")
    public CommentResponse getCommentById(String commentId) {

        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        return commentUtils.createCommentResponseFromComment(comment);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.COMMENTS_CACHE_NAME, key = "'post:' + #deleteCommentRequestDTO.postId"),
            @CacheEvict(cacheNames = Constants.COMMENTS_CACHE_NAME, key = "#commentId"),
            @CacheEvict(cacheNames = Constants.POSTS_CACHE_NAME, key = "#deleteCommentRequestDTO.postId"),
            @CacheEvict(cacheNames = Constants.POST_LIST_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.POPULAR_POSTS_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.TRENDING_POSTS_CACHE_NAME, allEntries = true)
    })
    public void deleteComment(String commentId,
                              DeleteCommentRequestDTO deleteCommentRequestDTO,
                              HttpServletRequest request) {
        User user = userUtils.getUserFromRequest(request);
        postRepository.findPostById(deleteCommentRequestDTO.getPostId()).orElseThrow(
                () -> new ResourceNotFoundException("Post not found"));

        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (!comment.getAuthorId().equalsIgnoreCase(String.valueOf(user.getId()))) {
            throw new ForbiddenException("You cannot delete this comment");
        }

        commentRepository.deleteCommentById(commentId);
        postRankingIndexService.rebuildIndexes();

    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getAllComments(int page, int size, String sortBy, String order,
                                                        CommentFilterRequest commentFilterRequest) {
        size = Math.min(size, 30);

        Sort sort = commentUtils.createSort(sortBy, order);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Comment> comments = commentUtils.findComments(commentFilterRequest, pageable);
        return commentUtils.mapCommentsToResponse(comments);

    }
}