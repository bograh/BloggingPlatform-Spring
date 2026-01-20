package org.amalitech.bloggingplatformspring.dao.helpers;

import java.util.List;

public record FilterClause(
        String whereClause,
        List<Object> parameters
) {
}