package org.amalitech.bloggingplatformspring.dtos.requests;

import jakarta.persistence.criteria.Join;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.Tag;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Objects;

public class PostSpecifications {

    public static Specification<Post> hasAuthor(String username) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("author").get("username")), username.toLowerCase());
    }

    public static Specification<Post> searchByContent(String search) {
        return (root, query, cb) -> {
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("content")), pattern)
            );
        };
    }

    public static Specification<Post> hasTags(List<String> tags) {
        return (root, query, cb) -> {
            Objects.requireNonNull(query).distinct(true);
            Join<Post, Tag> tagJoin = root.join("tags");
            return tagJoin.get("name").in(tags);
        };
    }
}