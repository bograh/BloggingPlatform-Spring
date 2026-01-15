package org.amalitech.bloggingplatformspring.dao.helpers;

import org.amalitech.bloggingplatformspring.dtos.requests.PostFilterRequest;
import org.amalitech.bloggingplatformspring.enums.PostSortField;
import org.amalitech.bloggingplatformspring.enums.SortDirection;

import java.util.ArrayList;
import java.util.List;

public class DAOHelperMethods {
    
    public FilterClause buildFilterClause(PostFilterRequest filterRequest) {
        if (filterRequest == null) {
            return new FilterClause("", List.of());
        }

        List<String> conditions = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        if (filterRequest.author() != null && !filterRequest.author().isBlank()) {
            conditions.add("u.username ILIKE ?");
            parameters.add("%" + filterRequest.author().trim() + "%");
        }

        if (filterRequest.search() != null && !filterRequest.search().isBlank()) {
            conditions.add("(p.title ILIKE ? OR p.body ILIKE ?)");
            String searchParam = "%" + filterRequest.search().trim() + "%";
            parameters.add(searchParam);
            parameters.add(searchParam);
        }

        if (filterRequest.tags() != null && !filterRequest.tags().isEmpty()) {
            conditions.add("""
                    p.id IN (
                        SELECT pt2.post_id
                        FROM post_tags pt2
                        JOIN tags t2 ON t2.id = pt2.tag_id
                        WHERE t2.name = ANY(?)
                    )
                    """);
            parameters.add(filterRequest.tags().toArray(new String[0]));
        }

        String whereClause = conditions.isEmpty()
                ? ""
                : "WHERE " + String.join(" AND ", conditions);

        return new FilterClause(whereClause, parameters);
    }

    public PostSortField matchSortByToEntityField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return PostSortField.UPDATED_AT;
        }

        return switch (sortBy.toLowerCase().trim()) {
            case "id" -> PostSortField.ID;
            case "title" -> PostSortField.TITLE;
            case "body" -> PostSortField.BODY;
            case "author" -> PostSortField.AUTHOR;
            default -> PostSortField.UPDATED_AT;
        };
    }

    public SortDirection getSortDirection(String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return SortDirection.DESC;
        }

        return switch (sortDirection.toUpperCase().trim()) {
            case "ASC", "ASCENDING" -> SortDirection.ASC;
            case "DESC", "DESCENDING" -> SortDirection.DESC;
            default -> SortDirection.DESC;
        };
    }

    public String buildOrderByClause(PostSortField sortField, SortDirection direction) {
        String column = switch (sortField) {
            case ID -> "p.id";
            case TITLE -> "p.title";
            case BODY -> "p.body";
            case AUTHOR -> "u.username";
            case UPDATED_AT -> "p.updated_at";
        };

        String dir = switch (direction) {
            case ASC -> "ASC";
            case DESC -> "DESC";
        };

        return column + " " + dir;
    }
}