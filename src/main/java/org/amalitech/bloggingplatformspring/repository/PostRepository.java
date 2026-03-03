package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @EntityGraph(attributePaths = { "author", "tags" })
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    Optional<Post> findPostById(Long id);

    @EntityGraph(attributePaths = { "author", "tags" })
    @Query("SELECT p FROM Post p")
    List<Post> findAllWithAuthorAndTags();

    @EntityGraph(attributePaths = { "author", "tags" })
    @Query(value = "SELECT p FROM Post p ORDER BY p.postedAt DESC")
    Page<Post> findRecentPosts(Pageable pageable);

    @EntityGraph(attributePaths = { "author", "tags" })
    @Query(value = """
                SELECT p FROM Post p
                WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.body)  LIKE LOWER(CONCAT('%', :query, '%'))
            """, countQuery = """
                SELECT COUNT(p) FROM Post p
                WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.body)  LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<Post> search(@Param("query") String query, Pageable pageable);

    @EntityGraph(attributePaths = { "author", "tags" })
    List<Post> findPostsByAuthorOrderByUpdatedAtDesc(User author, Limit limit);

    Long countByAuthor(User user);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.postedAt >= :since")
    long countPostsCreatedAfter(@Param("since") LocalDateTime since);

    @EntityGraph(attributePaths = { "author", "tags" })
    @Query("SELECT p FROM Post p WHERE p.postedAt >= :since ORDER BY p.postedAt DESC")
    List<Post> findRecentPostsForDigest(@Param("since") LocalDateTime since, Pageable pageable);
}