package org.amalitech.bloggingplatformspring.controllers;

import org.amalitech.bloggingplatformspring.dtos.responses.ApiResponseGeneric;
import org.amalitech.bloggingplatformspring.dtos.responses.TagResponse;
import org.amalitech.bloggingplatformspring.services.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/popular")
    public ResponseEntity<?> getPopularTags() {
        List<TagResponse> popularTags = tagService.getPopularTags();
        ApiResponseGeneric<List<TagResponse>> response =
                ApiResponseGeneric.success("Popular Tags retrieved", popularTags);
        return ResponseEntity.ok(response);
    }

}