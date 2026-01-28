package org.amalitech.bloggingplatformspring.controllers;

import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponseGeneric;
import org.amalitech.bloggingplatformspring.dtos.responses.TagResponse;
import org.amalitech.bloggingplatformspring.services.TagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class TagControllerTest {

    @Mock
    private TagService tagService;

    @InjectMocks
    private TagController tagController;

    @Test
    void getPopularTags_shouldReturnOkWithPopularTags_whenTagsExist() {
        TagResponse tag1 = new TagResponse("java");
        TagResponse tag2 = new TagResponse("spring");
        TagResponse tag3 = new TagResponse("python");

        List<TagResponse> popularTags = Arrays.asList(tag1, tag2, tag3);

        when(tagService.getPopularTags()).thenReturn(popularTags);

        ResponseEntity<?> response = tagController.getPopularTags();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ApiResponseGeneric<List<TagResponse>> body =
                (ApiResponseGeneric<List<TagResponse>>) response.getBody();

        assertNotNull(body);
        assertEquals("Popular Tags retrieved", body.getMessage());
        assertNotNull(body.getData());
        assertEquals(3, body.getData().size());
        assertEquals("java", body.getData().get(0).name());
        assertEquals("spring", body.getData().get(1).name());
        assertEquals("python", body.getData().get(2).name());

        verify(tagService, times(1)).getPopularTags();
    }

    @Test
    void getPopularTags_shouldReturnOkWithEmptyList_whenNoTagsExist() {
        List<TagResponse> emptyList = Collections.emptyList();

        when(tagService.getPopularTags()).thenReturn(emptyList);

        ResponseEntity<?> response = tagController.getPopularTags();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ApiResponseGeneric<List<TagResponse>> body =
                (ApiResponseGeneric<List<TagResponse>>) response.getBody();

        assertNotNull(body);
        assertEquals("Popular Tags retrieved", body.getMessage());
        assertNotNull(body.getData());
        assertTrue(body.getData().isEmpty());

        verify(tagService, times(1)).getPopularTags();
    }

    @Test
    void getPopularTags_shouldReturnSuccessResponse_withCorrectStructure() {
        TagResponse tag = new TagResponse("javascript");
        List<TagResponse> popularTags = Collections.singletonList(tag);

        when(tagService.getPopularTags()).thenReturn(popularTags);

        ResponseEntity<?> response = tagController.getPopularTags();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(ApiResponseGeneric.class, response.getBody());

        verify(tagService, times(1)).getPopularTags();
    }

    @Test
    void getPopularTags_shouldCallServiceExactlyOnce() {
        when(tagService.getPopularTags()).thenReturn(Collections.emptyList());

        tagController.getPopularTags();

        verify(tagService, times(1)).getPopularTags();
        verifyNoMoreInteractions(tagService);
    }

    @Test
    void getPopularTags_shouldReturnHttpStatus200() {
        when(tagService.getPopularTags()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = tagController.getPopularTags();

        assertEquals(200, response.getStatusCode().value());
    }
}