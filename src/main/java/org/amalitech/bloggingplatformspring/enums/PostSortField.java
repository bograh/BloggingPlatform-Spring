package org.amalitech.bloggingplatformspring.enums;

public enum PostSortField {
    ID("p.id"),
    TITLE("p.title"),
    BODY("p.body"),
    UPDATED_AT("p.updated_at"),
    AUTHOR("u.username");

    private final String sqlName;

    PostSortField(String sqlName) {
        this.sqlName = sqlName;
    }

    public String sqlName() {
        return sqlName;
    }
}