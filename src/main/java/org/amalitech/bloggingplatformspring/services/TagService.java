package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.entity.Tag;
import org.amalitech.bloggingplatformspring.repository.TagRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Set<Tag> getOrCreateTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }

        List<String> normalizedNames = tagNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::toLowerCase)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        if (normalizedNames.isEmpty()) {
            return new HashSet<>();
        }

        List<Tag> existingTags = tagRepository.findTagsByNameIn(normalizedNames);
        Set<Tag> result = new HashSet<>(existingTags);

        Set<String> existingNames = existingTags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        List<String> missingNames = normalizedNames.stream()
                .filter(name -> !existingNames.contains(name))
                .collect(Collectors.toList());

        if (!missingNames.isEmpty()) {
            List<Tag> newTags = missingNames.stream()
                    .map(Tag::new)
                    .collect(Collectors.toList());

            try {
                List<Tag> savedTags = tagRepository.saveAll(newTags);
                result.addAll(savedTags);
            } catch (DataIntegrityViolationException e) {
                List<Tag> retryTags = tagRepository.findTagsByNameIn(missingNames);
                result.addAll(retryTags);
            }
        }

        return result;
    }
}