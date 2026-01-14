package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.Tag;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface TagRepository {

    Tag saveTag(String name) throws SQLException;

    List<Tag> getAllTags() throws SQLException;

    List<Tag> getAllTagsFromList(List<String> tagsList) throws SQLException;

    Boolean existsByName(String name) throws SQLException;

    Optional<Integer> getIdByName(String name) throws SQLException;
}