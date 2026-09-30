package com.moduplaylist.batch.job.contentembedding;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import com.moduplaylist.infrastructure.opensearch.content.ContentSearchDocument;
import com.moduplaylist.infrastructure.opensearch.content.ContentSearchDocumentRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentEmbeddingTargetService {

    private static final List<ContentType> EMBEDDABLE_TYPES =
            List.of(ContentType.MOVIE, ContentType.TV_SEASON);
    private static final List<Content.AiTaggingStatus> TERMINAL_TAGGING_STATUSES = List.of(
            Content.AiTaggingStatus.COMPLETED,
            Content.AiTaggingStatus.COMPLETED_PARTIAL,
            Content.AiTaggingStatus.FAILED
    );

    private final ContentRepository contentRepository;
    private final ContentSearchDocumentRepository searchDocumentRepository;
    private final EmbeddingGenerator embeddingGenerator;

    public List<UUID> findTargetContentIds(ContentEmbeddingRunWindow window) {
        List<Content> contents = window.fullScan()
                ? contentRepository.findEmbeddingSourcesThrough(
                        EMBEDDABLE_TYPES, TERMINAL_TAGGING_STATUSES, window.through())
                : contentRepository.findPendingEmbeddingSources(
                        EMBEDDABLE_TYPES, TERMINAL_TAGGING_STATUSES);

        return contents.stream()
                .filter(content -> content.isEmbeddingPending() || requiresEmbedding(content))
                .map(Content::getId)
                .toList();
    }

    private boolean requiresEmbedding(Content content) {
        return searchDocumentRepository.findById(content.getId())
                .map(document -> isEmbeddingOutdated(content, document))
                .orElse(true);
    }

    private boolean isEmbeddingOutdated(
            Content content,
            ContentSearchDocument document
    ) {
        return document.getEmbedding() == null
                || !Objects.equals(embeddingGenerator.modelName(), document.getEmbeddingModel())
                || !Objects.equals(
                        content.getEmbeddingSourceUpdatedAt(), document.getSourceUpdatedAt());
    }
}
