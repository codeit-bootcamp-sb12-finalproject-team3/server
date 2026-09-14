package com.moduplaylist.batch.job.contentembedding;

import com.moduplaylist.batch.job.contentembedding.dto.ContentEmbeddingResult;
import com.moduplaylist.batch.job.contentembedding.dto.ContentEmbeddingSource;
import com.moduplaylist.core.content.entity.*;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorDocument;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorRepository;
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
    private final ContentEmbeddingTextBuilder textBuilder;
    private final EmbeddingGenerator embeddingGenerator;
    private final ContentVectorRepository vectorRepository;

    public ContentEmbeddingResult embedAndIndex(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
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

        ContentEmbeddingSource source = new ContentEmbeddingSource(
                content.getTitle(),
                content.getType().getValue(),
                content.getDescription(),
                genres,
                tags
        );
        String embeddingText = textBuilder.build(source);
        float[] embedding = embeddingGenerator.embed(embeddingText);
        ContentVectorDocument document = new ContentVectorDocument(
                contentId,
                source.getType(),
                source.getTitle(),
                source.getDescription(),
                genres,
                tags,
                embedding,
                embeddingGenerator.modelName(),
                content.getUpdatedAt(),
                Instant.now()
        );
        vectorRepository.upsert(document);

        return new ContentEmbeddingResult(contentId, embeddingText, embedding.length);
    }
}
