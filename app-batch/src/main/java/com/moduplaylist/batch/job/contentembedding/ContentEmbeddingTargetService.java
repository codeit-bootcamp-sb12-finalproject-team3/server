package com.moduplaylist.batch.job.contentembedding;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorDocument;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentEmbeddingTargetService {

    private final ContentRepository contentRepository;
    private final ContentVectorRepository vectorRepository;
    private final EmbeddingGenerator embeddingGenerator;

    public List<UUID> findTargetContentIds() {
        return contentRepository.findAll().stream()
                .filter(content -> content.getType().isPersonalizable())
                .sorted(Comparator.comparing(Content::getId))
                .filter(this::requiresEmbedding)
                .map(Content::getId)
                .toList();
    }

    public List<UUID> findDeletedContentIds() {
        HashSet<UUID> existingContentIds = new HashSet<>(contentRepository.findAllIds());
        return vectorRepository.findAllIds().stream()
                .filter(contentId -> !existingContentIds.contains(contentId))
                .sorted()
                .toList();
    }

    private boolean requiresEmbedding(Content content) {
        return vectorRepository.findById(content.getId())
                .map(document -> isOutdated(content, document))
                .orElse(true);
    }

    private boolean isOutdated(Content content, ContentVectorDocument document) {
        Instant sourceUpdatedAt = document.getSourceUpdatedAt();
        return sourceUpdatedAt == null
                || content.getUpdatedAt().isAfter(sourceUpdatedAt)
                || !Objects.equals(embeddingGenerator.modelName(), document.getEmbeddingModel());
    }
}
