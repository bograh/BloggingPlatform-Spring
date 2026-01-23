package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    Optional<Post> findPostById(Long id);

    void deletePostById(Long id);

    @Query("""
                SELECT p FROM Post p
                WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.body)  LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<Post> search(@Param("query") String query, Pageable pageable);

    Page<Post> findByAuthor_UsernameIgnoreCase(String authorUsername, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Post p JOIN p.tags t WHERE t.name IN :tags")
    Page<Post> findByTagsNames(@Param("tags") List<String> tags, Pageable pageable);


}