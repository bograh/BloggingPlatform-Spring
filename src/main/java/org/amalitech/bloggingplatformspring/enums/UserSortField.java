package org.amalitech.bloggingplatformspring.enums;

import lombok.Getter;

@Getter
public enum UserSortField {
    USERNAME("username"),
    EMAIL("email"),
    CREATED_AT("createdAt");

    private final String propertyName;

    UserSortField(String propertyName) {
        this.propertyName = propertyName;
    }
}