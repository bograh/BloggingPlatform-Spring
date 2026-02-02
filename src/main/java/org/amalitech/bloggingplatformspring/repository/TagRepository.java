package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {

    List<Tag> findTagsByNameIn(List<String> tagsList);

    @Query("SELECT t, COUNT(p) as postCount FROM Tag t " +
            "LEFT JOIN t.posts p " +
            "GROUP BY t " +
            "ORDER BY postCount DESC")
    List<Tag> findMostPopularTags(Pageable pageable);

}