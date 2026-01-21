package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.entity.Tag;
import org.amalitech.bloggingplatformspring.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public Set<Tag> getOrCreateTags(List<String> tagNames) {
        return tagNames.stream()
                .map(tagName -> tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(new Tag(tagName))))
                .collect(Collectors.toSet());
    }
}