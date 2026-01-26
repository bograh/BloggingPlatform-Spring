package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.entity.Tag;
import org.amalitech.bloggingplatformspring.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    private Tag tag1;
    private Tag tag2;
    private Tag tag3;

    @BeforeEach
    void setUp() {
        tag1 = new Tag("java");
        tag1.setId(1L);

        tag2 = new Tag("spring");
        tag2.setId(2L);

        tag3 = new Tag("testing");
        tag3.setId(3L);
    }


    @Test
    void getOrCreateTags_WithNullInput_ShouldReturnEmptySet() {
        Set<Tag> result = tagService.getOrCreateTags(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tagRepository, never()).findTagsByNameIn(anyList());
        verify(tagRepository, never()).saveAll(anyList());
    }

    @Test
    void getOrCreateTags_WithEmptyList_ShouldReturnEmptySet() {
        Set<Tag> result = tagService.getOrCreateTags(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tagRepository, never()).findTagsByNameIn(anyList());
        verify(tagRepository, never()).saveAll(anyList());
    }

    @Test
    void getOrCreateTags_WithAllBlankNames_ShouldReturnEmptySet() {
        List<String> tagNames = Arrays.asList("", "   ", null, "\t");

        Set<Tag> result = tagService.getOrCreateTags(tagNames);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tagRepository, never()).findTagsByNameIn(anyList());
        verify(tagRepository, never()).saveAll(anyList());
    }

    @Test
    void getOrCreateTags_WithExistingTags_ShouldReturnExistingTags() {
        List<String> tagNames = Arrays.asList("Java", "Spring");
        List<Tag> existingTags = Arrays.asList(tag1, tag2);

        when(tagRepository.findTagsByNameIn(Arrays.asList("java", "spring")))
                .thenReturn(existingTags);

        Set<Tag> result = tagService.getOrCreateTags(tagNames);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(tag1));
        assertTrue(result.contains(tag2));

        verify(tagRepository).findTagsByNameIn(Arrays.asList("java", "spring"));
        verify(tagRepository, never()).saveAll(anyList());
    }

    @Test
    void getOrCreateTags_WithNewTags_ShouldCreateAndReturnNewTags() {
        List<String> tagNames = Arrays.asList("docker", "kubernetes");
        List<Tag> newTags = Arrays.asList(
                new Tag("docker"),
                new Tag("kubernetes")
        );

        when(tagRepository.findTagsByNameIn(Arrays.asList("docker", "kubernetes")))
                .thenReturn(Collections.emptyList());
        when(tagRepository.saveAll(anyList())).thenReturn(newTags);

        Set<Tag> result = tagService.getOrCreateTags(tagNames);

        assertNotNull(result);
        assertEquals(2, result.size());

        verify(tagRepository).findTagsByNameIn(Arrays.asList("docker", "kubernetes"));
        verify(tagRepository).saveAll(argThat(iterable -> {
            List<Tag> tags = StreamSupport
                    .stream(iterable.spliterator(), false)
                    .toList();

            return tags.size() == 2 &&
                    tags.stream().anyMatch(t -> t.getName().equals("docker")) &&
                    tags.stream().anyMatch(t -> t.getName().equals("kubernetes"));
        }));

    }

    @Test
    void getOrCreateTags_WithMixedExistingAndNewTags_ShouldReturnBoth() {
        List<String> tagNames = Arrays.asList("Java", "Docker");
        List<Tag> existingTags = Collections.singletonList(tag1);
        Tag dockerTag = new Tag("docker");
        dockerTag.setId(4L);
        List<Tag> newTags = Collections.singletonList(dockerTag);

        when(tagRepository.findTagsByNameIn(Arrays.asList("java", "docker")))
                .thenReturn(existingTags);
        when(tagRepository.saveAll(anyList())).thenReturn(newTags);

        Set<Tag> result = tagService.getOrCreateTags(tagNames);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(tag1));
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("docker")));

        verify(tagRepository).findTagsByNameIn(Arrays.asList("java", "docker"));
        verify(tagRepository).saveAll(argThat(iterable -> {
            List<Tag> tags = StreamSupport
                    .stream(iterable.spliterator(), false)
                    .toList();

            return tags.size() == 1 &&
                    tags.stream().anyMatch(t -> t.getName().equals("docker"));
        }));

    }

    @Test
    void getOrCreateTags_WithDuplicateNames_ShouldReturnDistinctTags() {
        List<String> tagNames = Arrays.asList("Java", "JAVA", "java", "Spring", "spring");
        List<Tag> existingTags = Arrays.asList(tag1, tag2);

        when(tagRepository.findTagsByNameIn(Arrays.asList("java", "spring")))
                .thenReturn(existingTags);

        Set<Tag> result = tagService.getOrCreateTags(tagNames);

        assertNotNull(result);
        assertEquals(2, result.size());

        verify(tagRepository).findTagsByNameIn(Arrays.asList("java", "spring"));
        verify(tagRepository, never()).saveAll(anyList());
    }

    @Test
    void getOrCreateTags_WithMixedCaseAndWhitespace_ShouldNormalizeNames() {
        List<String> tagNames = Arrays.asList("  Java  ", "SPRING", "TeStiNg");
        List<Tag> existingTags = Arrays.asList(tag1, tag2, tag3);

        when(tagRepository.findTagsByNameIn(Arrays.asList("java", "spring", "testing")))
                .thenReturn(existingTags);

        Set<Tag> result = tagService.getOrCreateTags(tagNames);

        assertNotNull(result);
        assertEquals(3, result.size());

        verify(tagRepository).findTagsByNameIn(Arrays.asList("java", "spring", "testing"));
    }

    @Test
    void getOrCreateTags_WithNullAndBlankNamesMixed_ShouldFilterAndProcess() {
        List<String> tagNames = Arrays.asList("Java", null, "", "  ", "Spring");
        List<Tag> existingTags = Arrays.asList(tag1, tag2);

        when(tagRepository.findTagsByNameIn(Arrays.asList("java", "spring")))
                .thenReturn(existingTags);

        Set<Tag> result = tagService.getOrCreateTags(tagNames);

        assertNotNull(result);
        assertEquals(2, result.size());

        verify(tagRepository).findTagsByNameIn(Arrays.asList("java", "spring"));
    }

    @Test
    void getOrCreateTags_WhenDataIntegrityViolationOccurs_ShouldRetryAndFetchTags() {
        List<String> tagNames = Arrays.asList("docker", "kubernetes");

        List<Tag> retryTags = Arrays.asList(
                new Tag("docker"),
                new Tag("kubernetes")
        );

        when(tagRepository.findTagsByNameIn(anyList()))
                .thenReturn(Collections.emptyList())
                .thenReturn(retryTags);

        when(tagRepository.saveAll(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        Set<Tag> result = tagService.getOrCreateTags(tagNames);
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(tagRepository, times(2)).findTagsByNameIn(anyList());
        verify(tagRepository).saveAll(any());
    }

    @Test
    void getOrCreateTags_WhenDataIntegrityViolationWithMixedTags_ShouldRetryOnlyMissingTags() {
        List<String> tagNames = Arrays.asList("Java", "docker");
        List<Tag> existingTags = Collections.singletonList(tag1);
        Tag dockerTag = new Tag("docker");
        dockerTag.setId(4L);
        List<Tag> retryTags = Collections.singletonList(dockerTag);

        when(tagRepository.findTagsByNameIn(Arrays.asList("java", "docker")))
                .thenReturn(existingTags);
        when(tagRepository.saveAll(anyList()))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));
        when(tagRepository.findTagsByNameIn(Collections.singletonList("docker")))
                .thenReturn(retryTags);

        Set<Tag> result = tagService.getOrCreateTags(tagNames);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(tag1));
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("docker")));

        verify(tagRepository).findTagsByNameIn(Arrays.asList("java", "docker"));
        verify(tagRepository).saveAll(anyList());
        verify(tagRepository).findTagsByNameIn(Collections.singletonList("docker"));
    }

    @Test
    void getOrCreateTags_WithSingleTag_ShouldProcessCorrectly() {
        List<String> tagNames = Collections.singletonList("Java");
        List<Tag> existingTags = Collections.singletonList(tag1);

        when(tagRepository.findTagsByNameIn(Collections.singletonList("java")))
                .thenReturn(existingTags);

        Set<Tag> result = tagService.getOrCreateTags(tagNames);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(tag1));

        verify(tagRepository).findTagsByNameIn(Collections.singletonList("java"));
        verify(tagRepository, never()).saveAll(anyList());
    }

    @Test
    void getOrCreateTags_WithOnlyWhitespaceInNames_ShouldReturnEmptySet() {
        List<String> tagNames = Arrays.asList("   ", "\t\t", "\n", "  \t  ");

        Set<Tag> result = tagService.getOrCreateTags(tagNames);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tagRepository, never()).findTagsByNameIn(anyList());
        verify(tagRepository, never()).saveAll(anyList());
    }

    @Test
    void getOrCreateTags_WithSpecialCharactersInNames_ShouldProcessCorrectly() {
        List<String> tagNames = Arrays.asList("C++", "C#", ".NET");
        Tag cppTag = new Tag("c++");
        cppTag.setId(5L);
        Tag csharpTag = new Tag("c#");
        csharpTag.setId(6L);
        Tag dotnetTag = new Tag(".net");
        dotnetTag.setId(7L);
        List<Tag> newTags = Arrays.asList(cppTag, csharpTag, dotnetTag);

        when(tagRepository.findTagsByNameIn(Arrays.asList("c++", "c#", ".net")))
                .thenReturn(Collections.emptyList());
        when(tagRepository.saveAll(anyList())).thenReturn(newTags);

        Set<Tag> result = tagService.getOrCreateTags(tagNames);

        assertNotNull(result);
        assertEquals(3, result.size());

        verify(tagRepository).findTagsByNameIn(Arrays.asList("c++", "c#", ".net"));
        verify(tagRepository).saveAll(anyList());
    }

    @Test
    void getOrCreateTags_WithLargeNumberOfTags_ShouldProcessAll() {
        List<String> tagNames = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            tagNames.add("tag" + i);
        }

        List<Tag> existingTags = Arrays.asList(tag1, tag2);
        List<Tag> newTags = new ArrayList<>();
        for (int i = 3; i <= 100; i++) {
            Tag tag = new Tag("tag" + i);
            tag.setId((long) i);
            newTags.add(tag);
        }

        when(tagRepository.findTagsByNameIn(anyList())).thenReturn(existingTags);
        when(tagRepository.saveAll(anyList())).thenReturn(newTags);

        Set<Tag> result = tagService.getOrCreateTags(tagNames);

        assertNotNull(result);
        assertTrue(result.size() >= 2);

        verify(tagRepository).findTagsByNameIn(anyList());
        verify(tagRepository).saveAll(anyList());
    }
}