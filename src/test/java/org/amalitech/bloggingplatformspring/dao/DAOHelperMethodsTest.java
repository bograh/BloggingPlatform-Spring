package org.amalitech.bloggingplatformspring.dao;

import org.amalitech.bloggingplatformspring.dao.helpers.DAOHelperMethods;
import org.amalitech.bloggingplatformspring.dao.helpers.FilterClause;
import org.amalitech.bloggingplatformspring.dtos.requests.PostFilterRequest;
import org.amalitech.bloggingplatformspring.enums.PostSortField;
import org.amalitech.bloggingplatformspring.enums.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DAOHelperMethodsTest {

    private DAOHelperMethods daoHelperMethods;

    @BeforeEach
    void setUp() {
        daoHelperMethods = new DAOHelperMethods();
    }


    @Test
    void buildFilterClause_NullRequest_ReturnsEmptyClause() {
        FilterClause clause = daoHelperMethods.buildFilterClause(null);

        assertEquals("", clause.whereClause());
        assertTrue(clause.parameters().isEmpty());
    }

    @Test
    void buildFilterClause_AuthorOnly() {
        PostFilterRequest request = new PostFilterRequest(
                "john",
                null,
                null
        );

        FilterClause clause = daoHelperMethods.buildFilterClause(request);

        assertEquals("WHERE u.username ILIKE ?", clause.whereClause());
        assertEquals(1, clause.parameters().size());
        assertEquals("%john%", clause.parameters().getFirst());
    }

    @Test
    void buildFilterClause_SearchOnly() {
        PostFilterRequest request = new PostFilterRequest(
                null,
                "spring",
                null
        );

        FilterClause clause = daoHelperMethods.buildFilterClause(request);

        assertEquals(
                "WHERE (p.title ILIKE ? OR p.body ILIKE ?)",
                clause.whereClause()
        );

        assertEquals(2, clause.parameters().size());
        assertEquals("%spring%", clause.parameters().get(0));
        assertEquals("%spring%", clause.parameters().get(1));
    }

    @Test
    void buildFilterClause_TagsOnly() {
        PostFilterRequest request = new PostFilterRequest(
                null,
                null,
                List.of("java", "backend")
        );

        FilterClause clause = daoHelperMethods.buildFilterClause(request);

        assertTrue(clause.whereClause().contains("p.id IN"));
        assertEquals(1, clause.parameters().size());

        Object param = clause.parameters().getFirst();
        assertInstanceOf(String[].class, param);
        assertArrayEquals(new String[]{"java", "backend"}, (String[]) param);
    }

    @Test
    void buildFilterClause_AllFiltersCombined() {
        PostFilterRequest request = new PostFilterRequest(
                "john",
                "spring",
                List.of("java")
        );

        FilterClause clause = daoHelperMethods.buildFilterClause(request);

        assertTrue(clause.whereClause().startsWith("WHERE"));
        assertTrue(clause.whereClause().contains("u.username ILIKE ?"));
        assertTrue(clause.whereClause().contains("(p.title ILIKE ? OR p.body ILIKE ?)"));
        assertTrue(clause.whereClause().contains("p.id IN"));

        assertEquals(4, clause.parameters().size());
    }

    @Test
    void matchSortByToEntityField_Null_ReturnsDefault() {
        PostSortField result = daoHelperMethods.matchSortByToEntityField(null);

        assertEquals(PostSortField.UPDATED_AT, result);
    }

    @Test
    void matchSortByToEntityField_ValidFields() {
        assertEquals(PostSortField.ID,
                daoHelperMethods.matchSortByToEntityField("id"));

        assertEquals(PostSortField.TITLE,
                daoHelperMethods.matchSortByToEntityField("title"));

        assertEquals(PostSortField.BODY,
                daoHelperMethods.matchSortByToEntityField("body"));

        assertEquals(PostSortField.AUTHOR,
                daoHelperMethods.matchSortByToEntityField("author"));
    }

    @Test
    void matchSortByToEntityField_UnknownValue_ReturnsDefault() {
        PostSortField result =
                daoHelperMethods.matchSortByToEntityField("unknown");

        assertEquals(PostSortField.UPDATED_AT, result);
    }

    @Test
    void getSortDirection_Null_ReturnsDesc() {
        SortDirection direction = daoHelperMethods.getSortDirection(null);

        assertEquals(SortDirection.DESC, direction);
    }

    @Test
    void getSortDirection_AscendingValues() {
        assertEquals(SortDirection.ASC,
                daoHelperMethods.getSortDirection("asc"));

        assertEquals(SortDirection.ASC,
                daoHelperMethods.getSortDirection("ASCENDING"));
    }

    @Test
    void getSortDirection_DescendingValues() {
        assertEquals(SortDirection.DESC,
                daoHelperMethods.getSortDirection("desc"));

        assertEquals(SortDirection.DESC,
                daoHelperMethods.getSortDirection("DESCENDING"));
    }

    @Test
    void getSortDirection_InvalidValue_ReturnsDesc() {
        SortDirection direction =
                daoHelperMethods.getSortDirection("random");

        assertEquals(SortDirection.DESC, direction);
    }

    @Test
    void buildOrderByClause_IdAsc() {
        String orderBy = daoHelperMethods.buildOrderByClause(
                PostSortField.ID,
                SortDirection.ASC
        );

        assertEquals("p.id ASC", orderBy);
    }

    @Test
    void buildOrderByClause_AuthorDesc() {
        String orderBy = daoHelperMethods.buildOrderByClause(
                PostSortField.AUTHOR,
                SortDirection.DESC
        );

        assertEquals("u.username DESC", orderBy);
    }

    @Test
    void buildOrderByClause_UpdatedAtDesc() {
        String orderBy = daoHelperMethods.buildOrderByClause(
                PostSortField.UPDATED_AT,
                SortDirection.DESC
        );

        assertEquals("p.updated_at DESC", orderBy);
    }
}