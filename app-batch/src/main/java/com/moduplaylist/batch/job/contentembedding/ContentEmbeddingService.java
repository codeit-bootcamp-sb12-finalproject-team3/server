package com.moduplaylist.batch.job.contentembedding;

import com.moduplaylist.batch.job.contentembedding.dto.ContentEmbeddingResult;
import com.moduplaylist.batch.job.contentembedding.dto.ContentEmbeddingSource;
import com.moduplaylist.core.content.entity.*;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.content.repository.ContentCastRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import com.moduplaylist.infrastructure.opensearch.content.ContentEmbeddingFields;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ContentEmbeddingService {

    private final ContentRepository contentRepository;
    private final ContentGenreRepository contentGenreRepository;
    private final ContentTagRepository contentTagRepository;
    private final ContentCastRepository contentCastRepository;
    private final ContentEmbeddingTextBuilder textBuilder;
    private final EmbeddingGenerator embeddingGenerator;
    private final ContentEmbeddingCompletionService completionService;
    private final TransactionTemplate transactionTemplate;

    public ContentEmbeddingResult embedAndIndex(UUID contentId) {
        EmbeddingSnapshot snapshot = transactionTemplate.execute(status -> loadSnapshot(contentId));
        if (snapshot == null) return new ContentEmbeddingResult(contentId, "", 0, false);
        String embeddingText = textBuilder.build(snapshot.source());
        // No transaction/row lock while calling the embedding provider.
        float[] embedding = embeddingGenerator.embed(embeddingText);
        ContentEmbeddingFields embeddingFields = ContentEmbeddingFields.builder()
                .embedding(embedding)
                .embeddingModel(embeddingGenerator.modelName())
                .sourceUpdatedAt(snapshot.sourceUpdatedAt())
                .embeddedAt(Instant.now())
                .build();
        boolean published = completionService.publishIfCurrent(contentId, snapshot.sourceUpdatedAt(), embeddingFields);
        return new ContentEmbeddingResult(contentId, embeddingText, embedding.length, published);
    }

    private EmbeddingSnapshot loadSnapshot(UUID contentId) {
        UUID parentId = contentRepository.findParentId(contentId).orElse(null);
        Content parent = parentId == null ? null : contentRepository.findById(parentId).orElse(null);
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        if (content.isHidden() || (content.getType() != ContentType.MOVIE && content.getType() != ContentType.TV_SEASON)
            || !content.isEmbeddingAllowedByAiTaggingStatus()
            || (content.getType() == ContentType.TV_SEASON && (parent == null || parent.isHidden()
                || !parent.getId().equals(content.getParentContent().getId())))) return null;
        Instant sourceUpdatedAt = content.getEmbeddingSourceUpdatedAt();
        List<String> genres = contentGenreRepository
                .findAllWithGenreByContentIdIn(List.of(contentId)).stream()
                .map(ContentGenre::getGenre)
                .map(Genre::getName)
                .distinct()
                .sorted()
                .toList();
        List<String> tags = contentTagRepository
                .findAllWithTagByContentIdIn(List.of(contentId)).stream()
                .map(ContentTag::getTag)
                .map(Tag::getName)
                .distinct()
                .sorted()
                .toList();
        List<ContentCast> casts = contentCastRepository
                .findAllByContent_IdOrderByDisplayOrderAsc(contentId);
        List<String> castNames = casts.stream()
                .map(ContentCast::getName)
                .toList();
        Content titleSource = content.getType() == ContentType.TV_SEASON
                ? content.getParentContent()
                : content;
        String originalTitle = titleSource == null ? null : titleSource.getOriginalTitle();

        ContentEmbeddingSource source = new ContentEmbeddingSource(
                content.getTitle(),
                originalTitle,
                castNames,
                content.getType().getValue(),
                content.getDescription(),
                genres,
                tags
        );
        return new EmbeddingSnapshot(source, sourceUpdatedAt);
    }

    private record EmbeddingSnapshot(ContentEmbeddingSource source, Instant sourceUpdatedAt) { }
}
