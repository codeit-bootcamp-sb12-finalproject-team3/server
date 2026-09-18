package com.moduplaylist.batch.job.contentembedding;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorDocument;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorRepository;
import java.time.Instant;
import java.util.Comparator;
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

    private final ContentRepository contentRepository;
    private final ContentTagRepository contentTagRepository;
    private final ContentVectorRepository vectorRepository;
    private final EmbeddingGenerator embeddingGenerator;

    public List<UUID> findTargetContentIds() {
        List<Content> contents = contentRepository.findAll().stream()
                .filter(content -> content.getType().isPersonalizable())
                .sorted(Comparator.comparing(Content::getId))
                .toList();
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
        Instant sourceUpdatedAt = document.getSourceUpdatedAt();
        return sourceUpdatedAt == null
                || content.getUpdatedAt().isAfter(sourceUpdatedAt)
                || !Objects.equals(embeddingGenerator.modelName(), document.getEmbeddingModel())
                || !tags.equals(document.getTags());
    }
}
