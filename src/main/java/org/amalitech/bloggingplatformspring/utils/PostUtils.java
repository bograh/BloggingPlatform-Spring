package org.amalitech.bloggingplatformspring.utils;

import org.amalitech.bloggingplatformspring.dtos.requests.PostFilterRequest;
import org.amalitech.bloggingplatformspring.dtos.requests.PostSpecifications;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PostResponseDTO;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.Tag;
import org.amalitech.bloggingplatformspring.enums.PostSortField;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PostUtils {

    private final CommentRepository commentRepository;

    public PostUtils(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public PostResponseDTO createResponseFromPostAndTags(Post post, String authorName, List<String> tags, Long totalComments) {
        return new PostResponseDTO(
                post.getId(),
                post.getTitle(),
                post.getBody(),
                String.valueOf(post.getAuthor().getId()),
                authorName,
                tags,
                formatDate(post.getPostedAt()),
                formatDate(post.getUpdatedAt()),
                totalComments
        );
    }

    public PostResponseDTO createPostResponseFromPost(Post post, Long totalComments) {
        return new PostResponseDTO(
                post.getId(),
                post.getTitle(),
                post.getBody(),
                post.getAuthor().getUsername(),
                String.valueOf(post.getAuthor().getId()),
                post.getTags().stream()
                        .map(Tag::getName)
                        .toList(),
                formatDate(post.getPostedAt()),
                formatDate(post.getUpdatedAt()),
                totalComments
        );
    }

    public PageResponse<PostResponseDTO> mapPostPageToPostResponsePage(Page<Post> postPage) {
        List<PostResponseDTO> postsResponse = postPage.getContent().stream()
                .map(post -> {
                    Long totalComments = commentRepository.countByPostId(post.getId());
                    return createPostResponseFromPost(post, totalComments);
                })
                .toList();

        return new PageResponse<>(
                postsResponse,
                postPage.getPageable().getPageNumber(),
                postPage.getSize(),
                postPage.getSort().toString(),
                postPage.getTotalElements(),
                postPage.isLast()
        );
    }

    public Specification<Post> buildSpecification(PostFilterRequest filter) {
        return Specification.allOf(
                filter.author() != null ? PostSpecifications.hasAuthor(filter.author()) : null,
                filter.search() != null ? PostSpecifications.searchByContent(filter.search()) : null,
                filter.tags() != null && !filter.tags().isEmpty() ? PostSpecifications.hasTags(filter.tags()) : null
        ).and(Specification.allOf());
    }

    public String mapSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return PostSortField.UPDATED_AT.getPropertyName();
        }

        return switch (sortBy.toLowerCase().trim()) {
            case "id" -> PostSortField.ID.getPropertyName();
            case "title" -> PostSortField.TITLE.getPropertyName();
            case "body" -> PostSortField.BODY.getPropertyName();
            case "author" -> PostSortField.AUTHOR.getPropertyName();
            default -> PostSortField.UPDATED_AT.getPropertyName();
        };
    }

    public String mapOrderField(String order) {
        if (order == null || order.isBlank()) {
            return "DESC";
        }

        if (order.trim().equalsIgnoreCase("asc"))
            return "ASC";

        return "DESC";
    }

    private String formatDate(LocalDateTime localDateTime) {
        return localDateTime.format(
                DateTimeFormatter.ofPattern(Constants.DateTimeFormatPattern)
        );
    }


}