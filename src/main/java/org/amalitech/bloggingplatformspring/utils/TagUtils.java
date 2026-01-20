package org.amalitech.bloggingplatformspring.utils;

import org.amalitech.bloggingplatformspring.entity.Tag;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TagUtils {

    public Tag mapRowToTag(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");

        return new Tag(id, name);
    }
}