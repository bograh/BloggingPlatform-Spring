package org.amalitech.bloggingplatformspring.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.requests.CreatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.requests.PostFilterRequest;
import org.amalitech.bloggingplatformspring.dtos.requests.UpdatePostDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.PostImage;
import org.amalitech.bloggingplatformspring.entity.Tag;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.exceptions.BadRequestException;
import org.amalitech.bloggingplatformspring.exceptions.ForbiddenException;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.PostImageRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.amalitech.bloggingplatformspring.utils.PostUtils;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostUtils postUtils;
    private final TagService tagService;
    private final UserUtils userUtils;
    private final PostRankingIndexService postRankingIndexService;
    private final AsyncImageUploadService asyncImageUploadService;
    private final PostImageRepository postImageRepository;
    private final NotificationQueueService notificationQueueService;

    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.POSTS_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.POST_LIST_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.POPULAR_POSTS_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.TRENDING_POSTS_CACHE_NAME, allEntries = true)
    })
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public PostResponseDTO createPost(CreatePostDTO createPostDTO, HttpServletRequest request) {
        return createPost(createPostDTO, request, null);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.POSTS_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.POST_LIST_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.POPULAR_POSTS_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.TRENDING_POSTS_CACHE_NAME, allEntries = true)
    })
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public PostResponseDTO createPost(CreatePostDTO createPostDTO, HttpServletRequest request, MultipartFile image) {
        User user = userUtils.getUserFromRequest(request);
        Post post = new Post();
        post.setTitle(createPostDTO.getTitle());
        post.setBody(createPostDTO.getBody());
        post.setAuthor(user);

        if (createPostDTO.getTags() != null && !createPostDTO.getTags().isEmpty()) {
            Set<Tag> tags = tagService.getOrCreateTags(createPostDTO.getTags());
            post.setTags(tags);
        } else {
            post.setTags(new HashSet<>());
        }

        Post savedPost = postRepository.save(post);
        notificationQueueService.queuePostPublishedEmail(savedPost);

        if (image != null && !image.isEmpty()) {
            asyncImageUploadService.initiateUpload(savedPost.getId(), image);
        }

        postRankingIndexService.rebuildIndexes();

        PostResponseDTO response = postUtils.createResponseFromPostAndTags(
                savedPost,
                user.getUsername(),
                createPostDTO.getTags(),
                0L);

        return attachImageUrls(response);
    }

    @Cacheable(cacheNames = Constants.POST_LIST_CACHE_NAME, key = "'page:' + #page + 'size:' + #size + 'sort:' + #sortBy + 'order:' + #order", condition = "!#postFilterRequest.hasFilters()")
    public PageResponse<PostResponseDTO> getAllPosts(int page, int size, String sortBy, String order,
            PostFilterRequest postFilterRequest) {
        size = Math.min(size, 30);
        String entitySortField = postUtils.mapSortField(sortBy);
        String orderBy = postUtils.mapOrderField(order);
        Sort sort = Sort.by(Sort.Direction.fromString(orderBy), entitySortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Post> spec = postUtils.buildSpecification(postFilterRequest);

        Page<Post> postPage = postRepository.findAll(spec, pageable);
        PageResponse<PostResponseDTO> pageResponse = postUtils.mapPostPageToPostResponsePage(postPage);
        List<PostResponseDTO> contentWithImages = pageResponse.content().stream()
                .map(this::attachImageUrls)
                .toList();

        return new PageResponse<>(
                contentWithImages,
                pageResponse.page(),
                pageResponse.size(),
                pageResponse.sort(),
                pageResponse.totalElements(),
                pageResponse.last());
    }

    @Cacheable(cacheNames = Constants.POSTS_CACHE_NAME, key = "#postId")
    public PostResponseDTO getPostById(Long postId) {
        if (postId <= 0) {
            throw new BadRequestException("Post ID must be a positive number");
        }
        Post post = postRepository.findPostById(postId).orElseThrow(
                () -> new ResourceNotFoundException("Post not found with id: " + postId));

        Long totalComments = commentRepository.countByPostId(postId);
        PostResponseDTO response = postUtils.createPostResponseFromPost(post, totalComments);
        return attachImageUrls(response);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.POST_LIST_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.POSTS_CACHE_NAME, key = "#postId"),
            @CacheEvict(cacheNames = Constants.POPULAR_POSTS_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.TRENDING_POSTS_CACHE_NAME, allEntries = true)
    })
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public PostResponseDTO updatePost(Long postId, UpdatePostDTO updatePostDTO, HttpServletRequest request) {
        User user = userUtils.getUserFromRequest(request);

        Post post = postRepository.findPostById(postId).orElseThrow(
                () -> new ResourceNotFoundException("Post with ID: " + postId + " not found."));

        if (!user.getId().equals(post.getAuthor().getId())) {
            throw new ForbiddenException("You are not permitted to edit this post.");
        }

        if (!updatePostDTO.getTitle().isBlank()) {
            post.setTitle(updatePostDTO.getTitle());
        }

        if (!updatePostDTO.getBody().isBlank()) {
            post.setBody(updatePostDTO.getBody());
        }

        if (updatePostDTO.getTags() != null && !updatePostDTO.getTags().isEmpty()) {
            Set<Tag> updatedTags = tagService.getOrCreateTags(updatePostDTO.getTags());
            post.getTags().addAll(updatedTags);
        }

        post.setUpdatedAt(LocalDateTime.now());

        Post savedPost = postRepository.save(post);
        postRankingIndexService.rebuildIndexes();
        long totalComments = commentRepository.countByPostId(savedPost.getId());

        PostResponseDTO response = postUtils.createPostResponseFromPost(savedPost, totalComments);
        return attachImageUrls(response);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.POST_LIST_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.POSTS_CACHE_NAME, key = "#postId"),
            @CacheEvict(cacheNames = Constants.POPULAR_POSTS_CACHE_NAME, allEntries = true),
            @CacheEvict(cacheNames = Constants.TRENDING_POSTS_CACHE_NAME, allEntries = true)
    })
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void deletePost(Long postId, HttpServletRequest request) {
        User user = userUtils.getUserFromRequest(request);
        Post post = postRepository.findPostById(postId).orElseThrow(
                () -> new ResourceNotFoundException("Post with ID: " + postId + " not found."));

        if (!user.getId().equals(post.getAuthor().getId())) {
            throw new ForbiddenException("You are not permitted to delete this post.");
        }

        asyncImageUploadService.deleteAllImagesForPost(postId);
        postRepository.delete(post);
        commentRepository.deleteCommentsByPostId(postId);
        postRankingIndexService.rebuildIndexes();
    }

    public List<PostResponseDTO> getPopularPosts(int limit) {
        return postRankingIndexService.getPopularPosts(limit)
                .stream()
                .map(this::attachImageUrls)
                .toList();
    }

    public List<PostResponseDTO> getTrendingPosts(int limit) {
        return postRankingIndexService.getTrendingPosts(limit)
                .stream()
                .map(this::attachImageUrls)
                .toList();
    }

    private PostResponseDTO attachImageUrls(PostResponseDTO postResponseDTO) {
        if (postResponseDTO == null || postResponseDTO.getId() == null) {
            return postResponseDTO;
        }

        List<String> imageUrls = postImageRepository.findByPostId(postResponseDTO.getId())
                .stream()
                .map(PostImage::getCdnUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();

        postResponseDTO.setImageUrls(imageUrls);
        return postResponseDTO;
    }
}