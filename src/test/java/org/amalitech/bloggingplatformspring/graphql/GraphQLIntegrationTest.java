package org.amalitech.bloggingplatformspring.graphql;

import org.amalitech.bloggingplatformspring.graphql.config.GraphQLScalarConfig;
import org.amalitech.bloggingplatformspring.graphql.resolvers.GraphQLQueryResolver;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;

import static org.mockito.Mockito.when;

/**
 * Integration test for GraphQL queries
 * Tests the GraphQL endpoint configuration and basic query execution
 */
@GraphQlTest(GraphQLQueryResolver.class)
@Import(GraphQLScalarConfig.class)
public class GraphQLIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private org.amalitech.bloggingplatformspring.services.CommentService commentService;

    @MockitoBean
    private org.amalitech.bloggingplatformspring.repository.UserRepository userRepository;

    @MockitoBean
    private org.amalitech.bloggingplatformspring.repository.TagRepository tagRepository;

    @Test
    void testGetAllPostsQuery() {
        // Mock the service to return empty list
        when(postService.getAllPosts()).thenReturn(new ArrayList<>());

        // Execute GraphQL query
        graphQlTester.document("{ getAllPosts { id title body } }")
                .execute()
                .path("getAllPosts")
                .entityList(Object.class)
                .hasSize(0);
    }

    @Test
    void testGraphQLSchemaIsValid() {
        // This test verifies that the GraphQL schema is properly loaded
        // If the schema has syntax errors, this test will fail
        graphQlTester.document("{ __schema { queryType { name } } }")
                .execute()
                .path("__schema.queryType.name")
                .entity(String.class)
                .isEqualTo("Query");
    }
}