package org.amalitech.bloggingplatformspring.enums;

import lombok.Getter;

@Getter
public enum PostSortField {
    ID("id"),
    TITLE("title"),
    BODY("body"),
    UPDATED_AT("updatedAt"),
    AUTHOR("author.username");

    private final String propertyName;

    PostSortField(String propertyName) {
        this.propertyName = propertyName;
    }

}