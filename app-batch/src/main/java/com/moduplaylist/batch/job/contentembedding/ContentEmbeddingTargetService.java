package com.moduplaylist.batch.job.contentembedding;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorDocument;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentEmbeddingTargetService {

    private static final List<ContentType> EMBEDDABLE_TYPES =
            List.of(ContentType.MOVIE, ContentType.TV_SEASON);

    private final ContentRepository contentRepository;
    private final ContentTagRepository contentTagRepository;
    private final ContentVectorRepository vectorRepository;
    private final EmbeddingGenerator embeddingGenerator;

    public List<UUID> findTargetContentIds(ContentEmbeddingRunWindow window) {
        List<Content> contents = window.fullScan()
                ? contentRepository.findEmbeddingSourcesThrough(
                        EMBEDDABLE_TYPES, window.through())
                : contentRepository.findModifiedEmbeddingSources(
                        EMBEDDABLE_TYPES, window.after(), window.through());
        if (contents.isEmpty()) {
            return List.of();
        }

        List<UUID> contentIds = contents.stream().map(Content::getId).toList();
        Map<UUID, List<String>> tagsByContentId = contentTagRepository
                .findAllWithTagByContentIdIn(contentIds).stream()
                .collect(Collectors.groupingBy(
                        relation -> relation.getContent().getId(),
                        Collectors.mapping(relation -> relation.getTag().getName(), Collectors.toList())
                ));

        return contents.stream()
                .filter(content -> requiresEmbedding(
                        content,
                        sortedDistinct(tagsByContentId.get(content.getId()))
                ))
                .map(Content::getId)
                .toList();
    }

    private List<String> sortedDistinct(List<String> names) {
        return names == null ? List.of() : names.stream().distinct().sorted().toList();
    }

    public List<UUID> findDeletedContentIds() {
        HashSet<UUID> existingContentIds = new HashSet<>(contentRepository.findAllIds());
        return vectorRepository.findAllIds().stream()
                .filter(contentId -> !existingContentIds.contains(contentId))
                .sorted()
                .toList();
    }

    private boolean requiresEmbedding(Content content, List<String> tags) {
        return vectorRepository.findById(content.getId())
                .map(document -> isOutdated(content, tags, document))
                .orElse(true);
    }

    private boolean isOutdated(
            Content content,
            List<String> tags,
            ContentVectorDocument document
    ) {
        return !Objects.equals(embeddingGenerator.modelName(), document.getEmbeddingModel())
                || !Objects.equals(content.getType().getValue(), document.getType())
                || !Objects.equals(content.getTitle(), document.getTitle())
                || !Objects.equals(content.getDescription(), document.getDescription())
                || !tags.equals(document.getTags());
    }
}
