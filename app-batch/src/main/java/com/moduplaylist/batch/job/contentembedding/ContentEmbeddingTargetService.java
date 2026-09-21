package com.moduplaylist.batch.job.contentembedding;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.entity.SportEvent;
import com.moduplaylist.core.content.repository.ContentGenreRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.core.content.repository.SportEventRepository;
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
    private static final List<ContentType> INDEXED_TYPES =
            List.of(ContentType.MOVIE, ContentType.TV_SEASON, ContentType.SPORT);

    private final ContentRepository contentRepository;
    private final ContentGenreRepository contentGenreRepository;
    private final ContentTagRepository contentTagRepository;
    private final SportEventRepository sportEventRepository;
    private final ContentVectorRepository vectorRepository;
    private final EmbeddingGenerator embeddingGenerator;

    public List<UUID> findTargetContentIds(ContentEmbeddingRunWindow window) {
        List<Content> contents = window.fullScan()
                ? contentRepository.findEmbeddingSourcesThrough(
                        EMBEDDABLE_TYPES, window.through())
                : contentRepository.findPendingEmbeddingSources(EMBEDDABLE_TYPES);
        if (contents.isEmpty()) {
            return List.of();
        }

        List<UUID> contentIds = contents.stream().map(Content::getId).toList();
        Map<UUID, List<String>> genresByContentId = contentGenreRepository
                .findAllWithGenreByContentIdIn(contentIds).stream()
                .collect(Collectors.groupingBy(
                        relation -> relation.getContent().getId(),
                        Collectors.mapping(relation -> relation.getGenre().getName(), Collectors.toList())
                ));
        Map<UUID, List<String>> tagsByContentId = contentTagRepository
                .findAllWithTagByContentIdIn(contentIds).stream()
                .collect(Collectors.groupingBy(
                        relation -> relation.getContent().getId(),
                        Collectors.mapping(relation -> relation.getTag().getName(), Collectors.toList())
                ));

        return contents.stream()
                .filter(content -> content.isEmbeddingPending() || requiresEmbedding(
                        content,
                        sortedDistinct(genresByContentId.get(content.getId())),
                        sortedDistinct(tagsByContentId.get(content.getId()))
                ))
                .map(Content::getId)
                .toList();
    }

    private List<String> sortedDistinct(List<String> names) {
        return names == null ? List.of() : names.stream().distinct().sorted().toList();
    }

    public List<UUID> findDeletedContentIds() {
        HashSet<UUID> existingContentIds = new HashSet<>(
                contentRepository.findAllVisibleIdsByTypeIn(INDEXED_TYPES)
        );
        return vectorRepository.findAllIds().stream()
                .filter(contentId -> !existingContentIds.contains(contentId))
                .sorted()
                .toList();
    }

    public List<UUID> findSportSearchDocumentTargetIds() {
        return contentRepository
                .findAllByTypeAndHiddenFalseOrderByIdAsc(ContentType.SPORT).stream()
                .filter(this::requiresSportSearchDocument)
                .map(Content::getId)
                .toList();
    }

    private boolean requiresSportSearchDocument(Content content) {
        return vectorRepository.findById(content.getId())
                .map(document -> isSportSearchDocumentOutdated(content, document))
                .orElse(true);
    }

    private boolean isSportSearchDocumentOutdated(
            Content content,
            ContentVectorDocument document
    ) {
        if (!Objects.equals(content.getType().getValue(), document.getType())
                || !Objects.equals(content.getTitle(), document.getTitle())
                || !Objects.equals(content.getDescription(), document.getDescription())
                || !Objects.equals(content.isHidden(), document.getHidden())) {
            return true;
        }

        return sportEventRepository.findWithSportTypeByContentId(content.getId())
                .map(sportEvent -> hasChangedSportSearchFields(sportEvent, document))
                .orElse(true);
    }

    private boolean hasChangedSportSearchFields(
            SportEvent sportEvent,
            ContentVectorDocument document
    ) {
        return !Objects.equals(sportEvent.getSportType().getName(), document.getSportType())
                || !Objects.equals(sportEvent.getLeagueName(), document.getLeagueName())
                || !Objects.equals(sportEvent.getHomeTeamName(), document.getHomeTeamName())
                || !Objects.equals(sportEvent.getAwayTeamName(), document.getAwayTeamName());
    }

    private boolean requiresEmbedding(Content content, List<String> genres, List<String> tags) {
        return vectorRepository.findById(content.getId())
                .map(document -> isOutdated(content, genres, tags, document))
                .orElse(true);
    }

    private boolean isOutdated(
            Content content,
            List<String> genres,
            List<String> tags,
            ContentVectorDocument document
    ) {
        return !Objects.equals(embeddingGenerator.modelName(), document.getEmbeddingModel())
                || !Objects.equals(content.getType().getValue(), document.getType())
                || !Objects.equals(content.getTitle(), document.getTitle())
                || !Objects.equals(content.getDescription(), document.getDescription())
                || !genres.equals(document.getGenres())
                || !tags.equals(document.getTags());
    }
}
