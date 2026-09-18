package com.moduplaylist.batch.job.contentembedding;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.entity.SportEvent;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.SportEventRepository;
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
    private final SportEventRepository sportEventRepository;
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

    public List<UUID> findSportSearchDocumentTargetIds() {
        return contentRepository.findAll().stream()
                .filter(content -> content.getType() == ContentType.SPORT)
                .sorted(Comparator.comparing(Content::getId))
                .filter(this::requiresSearchDocument)
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

    private boolean requiresSearchDocument(Content content) {
        return vectorRepository.findById(content.getId())
                .map(document -> isSportSearchDocumentOutdated(content, document))
                .orElse(true);
    }

    private boolean isSportSearchDocumentOutdated(
            Content content,
            ContentVectorDocument document
    ) {
        if (isSearchDocumentOutdated(content, document)) {
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
                || !Objects.equals(sportEvent.getAwayTeamName(), document.getAwayTeamName())
                || !Objects.equals(sportEvent.getVenue(), document.getVenue());
    }
    // TODO: 태그 변경 감지 개선 필요. (필수)
    // 현재 임베딩 대상은 Content.updatedAt 기준이라 content_tags 추가/삭제를 감지하지 못한다.
    // 추후 임베딩 소스 변경 시각을 별도로 관리하여 태그 변경도 재임베딩 대상으로 포함한다.
    private boolean isOutdated(Content content, ContentVectorDocument document) {
        return isSearchDocumentOutdated(content, document)
                || !Objects.equals(embeddingGenerator.modelName(), document.getEmbeddingModel());
    }

    private boolean isSearchDocumentOutdated(
            Content content,
            ContentVectorDocument document
    ) {
        Instant sourceUpdatedAt = document.getSourceUpdatedAt();
        return sourceUpdatedAt == null
                || content.getUpdatedAt().isAfter(sourceUpdatedAt)
                || !Objects.equals(content.isHidden(), document.getHidden());
    }
}
