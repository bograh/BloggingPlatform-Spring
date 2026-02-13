package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.responses.StatsResponse;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public StatsResponse getStats() {
        long totalUsers = userRepository.count();
        long totalPosts = postRepository.count();
        long totalComments = commentRepository.count();

        return new StatsResponse(totalUsers, totalPosts, totalComments);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.POST_LIST_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.POSTS_CACHE_NAME, key = "#postId")
    })
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void adminDeletePost(Long postId) {
        Post post = postRepository.findPostById(postId).orElseThrow(
                () -> new ResourceNotFoundException("Post not found with id: " + postId)
        );
        postRepository.delete(post);
        commentRepository.deleteCommentsByPostId(postId);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.COMMENTS_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.POSTS_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.POST_LIST_CACHE_NAME, allEntries = true)
    })
    public void adminDeleteComment(String commentId) {
        commentRepository.deleteCommentById(commentId);
    }

}