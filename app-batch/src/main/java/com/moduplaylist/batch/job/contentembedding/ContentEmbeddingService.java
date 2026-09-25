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

    public ContentEmbeddingResult embedAndIndex(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
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
        String embeddingText = textBuilder.build(source);
        float[] embedding = embeddingGenerator.embed(embeddingText);
        ContentEmbeddingFields embeddingFields = ContentEmbeddingFields.builder()
                .embedding(embedding)
                .embeddingModel(embeddingGenerator.modelName())
                .sourceUpdatedAt(sourceUpdatedAt)
                .embeddedAt(Instant.now())
                .build();
        completionService.publishIfCurrent(contentId, sourceUpdatedAt, embeddingFields);

        return new ContentEmbeddingResult(contentId, embeddingText, embedding.length);
    }

}
