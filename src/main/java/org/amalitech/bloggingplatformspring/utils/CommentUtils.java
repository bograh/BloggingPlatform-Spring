package org.amalitech.bloggingplatformspring.utils;

import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.dtos.requests.CommentFilterRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.CommentResponse;
import org.amalitech.bloggingplatformspring.dtos.responses.PageResponse;
import org.amalitech.bloggingplatformspring.entity.Comment;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CommentUtils {

    private final MongoTemplate mongoTemplate;

    public CommentResponse createCommentResponseFromComment(Comment comment) {
        return mapToCommentResponse(comment);
    }

    public Page<Comment> findComments(CommentFilterRequest filter, Pageable pageable) {
        Query query = new Query();

        if (filter != null) {
            if (filter.postId() != null) {
                query.addCriteria(Criteria.where("post_id").is(filter.postId()));
            }

            if (filter.author() != null && !filter.author().isBlank()) {
                query.addCriteria(Criteria.where("author").is(filter.author()));
            }

            if (filter.search() != null && !filter.search().isBlank()) {
                query.addCriteria(Criteria.where("content").regex(filter.search(), "i"));
            }
        }

        query.with(pageable);

        List<Comment> comments = mongoTemplate.find(query, Comment.class);
        long total = mongoTemplate.count(query.skip(0).limit(0), Comment.class);

        return new PageImpl<>(comments, pageable, total);
    }

    public Sort createSort(String sortBy, String order) {
        String entitySortField = mapSortField(sortBy);
        Sort.Direction direction = Sort.Direction.fromString(mapOrderField(order));
        return Sort.by(direction, entitySortField);
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy != null ? sortBy.toLowerCase() : "commentedAt") {
            case "author" -> "author";
            case "postId" -> "post_id";
            case "content" -> "content";
            default -> "commentedAt";
        };
    }

    private String mapOrderField(String order) {
        return (order != null && order.equalsIgnoreCase("asc")) ? "ASC" : "DESC";
    }

    public PageResponse<CommentResponse> mapCommentsToResponse(Page<Comment> comments) {
        List<CommentResponse> commentResponses = comments.getContent().stream()
                .map(this::mapToCommentResponse)
                .toList();

        return new PageResponse<>(
                commentResponses,
                comments.getNumber(),
                comments.getSize(),
                comments.getSort().toString(),
                comments.getTotalElements(),
                comments.isLast()
        );
    }

    private CommentResponse mapToCommentResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setPostId(comment.getPostId());
        response.setAuthor(comment.getAuthor());
        response.setContent(comment.getContent());
        response.setCreatedAt(comment.getCommentedAt().toString());
        return response;
    }
}