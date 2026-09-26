package com.moduplaylist.batch.job.contentembedding;

import com.moduplaylist.batch.job.contentembedding.dto.ContentEmbeddingResult;
import com.moduplaylist.batch.job.contentembedding.dto.ContentEmbeddingSource;
import com.moduplaylist.core.content.entity.*;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.core.content.repository.SportEventRepository;
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
    private final SportEventRepository sportEventRepository;
    private final ContentEmbeddingTextBuilder textBuilder;
    private final EmbeddingGenerator embeddingGenerator;
    private final ContentVectorRepository vectorRepository;
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

        ContentEmbeddingSource source = new ContentEmbeddingSource(
                content.getTitle(),
                content.getType().getValue(),
                content.getDescription(),
                genres,
                tags
        );
        String embeddingText = textBuilder.build(source);
        float[] embedding = embeddingGenerator.embed(embeddingText);
        ContentVectorDocument document = ContentVectorDocument.builder()
                .contentId(contentId)
                .type(source.getType())
                .title(source.getTitle())
                .description(source.getDescription())
                .hidden(content.isHidden())
                .genres(genres)
                .tags(tags)
                .embedding(embedding)
                .embeddingModel(embeddingGenerator.modelName())
                .sourceUpdatedAt(sourceUpdatedAt)
                .embeddedAt(Instant.now())
                .build();
        completionService.publishIfCurrent(contentId, sourceUpdatedAt, document);

        return new ContentEmbeddingResult(contentId, embeddingText, embedding.length);
    }

    public void indexSportSearchDocument(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        if (content.getType() != ContentType.SPORT) {
            throw new IllegalArgumentException("스포츠 검색 문서는 SPORT 콘텐츠만 생성할 수 있습니다.");
        }
        SportEvent sportEvent = sportEventRepository.findWithSportTypeByContentId(contentId)
                .orElseThrow(() -> new IllegalStateException(
                        "스포츠 콘텐츠에 경기 정보가 없습니다. contentId=" + contentId
                ));

        ContentVectorDocument document = ContentVectorDocument.builder()
                .contentId(contentId)
                .type(content.getType().getValue())
                .title(content.getTitle())
                .description(content.getDescription())
                .hidden(content.isHidden())
                .genres(List.of())
                .tags(List.of())
                .sportTypeCode(sportEvent.getSportType().getCode())
                .sportType(sportEvent.getSportType().getName())
                .leagueName(sportEvent.getLeagueName())
                .season(sportEvent.getSeason())
                .homeTeamName(sportEvent.getHomeTeamName())
                .awayTeamName(sportEvent.getAwayTeamName())
                .sourceUpdatedAt(content.getUpdatedAt())
                .build();
        vectorRepository.upsert(document);
    }

    public void deleteFromIndex(UUID contentId) {
        vectorRepository.deleteById(contentId);
    }
}
