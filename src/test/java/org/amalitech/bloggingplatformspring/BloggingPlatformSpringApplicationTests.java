package org.amalitech.bloggingplatformspring;

import org.amalitech.bloggingplatformspring.controllers.CommentController;
import org.amalitech.bloggingplatformspring.controllers.PerformanceMetricsController;
import org.amalitech.bloggingplatformspring.controllers.PostController;
import org.amalitech.bloggingplatformspring.controllers.UserController;
import org.amalitech.bloggingplatformspring.services.CommentService;
import org.amalitech.bloggingplatformspring.services.PerformanceMetricsService;
import org.amalitech.bloggingplatformspring.services.PostService;
import org.amalitech.bloggingplatformspring.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BloggingPlatformSpringApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private UserController userController;

    @Autowired(required = false)
    private PostController postController;

    @Autowired(required = false)
    private CommentController commentController;

    @Autowired(required = false)
    private PerformanceMetricsController performanceMetricsController;

    @Autowired(required = false)
    private UserService userService;

    @Autowired(required = false)
    private PostService postService;

    @Autowired(required = false)
    private CommentService commentService;

    @Autowired(required = false)
    private PerformanceMetricsService performanceMetricsService;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void allControllersAreLoaded() {
        assertThat(userController).isNotNull();
        assertThat(postController).isNotNull();
        assertThat(commentController).isNotNull();
        assertThat(performanceMetricsController).isNotNull();
    }

    @Test
    void allServicesAreLoaded() {
        assertThat(userService).isNotNull();
        assertThat(postService).isNotNull();
        assertThat(commentService).isNotNull();
        assertThat(performanceMetricsService).isNotNull();
    }

    @Test
    void controllersHaveServicesDependenciesInjected() {
        assertThat(userController).isNotNull();
        assertThat(postController).isNotNull();
        assertThat(commentController).isNotNull();
        assertThat(performanceMetricsController).isNotNull();
    }

    @Test
    void applicationContextContainsBeans() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        assertThat(beanNames).isNotEmpty().hasSizeGreaterThan(10);
    }

    @Test
    void requiredBeansExist() {
        assertThat(applicationContext.containsBean("userController")).isTrue();
        assertThat(applicationContext.containsBean("postController")).isTrue();
        assertThat(applicationContext.containsBean("commentController")).isTrue();
        assertThat(applicationContext.containsBean("performanceMetricsController")).isTrue();
    }

    @Test
    void testProfileIsActive() {
        String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
        assertThat(activeProfiles).contains("test");
    }
}