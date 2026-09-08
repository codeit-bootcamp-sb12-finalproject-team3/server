package com.moduplaylist.api.content.service.impl;

import com.moduplaylist.api.content.dto.*;
import com.moduplaylist.api.content.event.ContentDeletedEvent;
import com.moduplaylist.api.content.service.ContentService;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.core.common.exception.*;
import com.moduplaylist.core.content.*;
import com.moduplaylist.core.content.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
@Transactional(readOnly = true)
public class ContentServiceImpl implements ContentService {
    private final ContentRepository contents;
    private final EpisodeRepository episodes;
    private final ContentQueryRepository queries;
    private final ContentRelationRepository relations;
    private final ApplicationEventPublisher events;

    @Override
    public ContentDetailResponse findById(UUID id) {
        return detail(required(id, false));
    }

    @Override
    public CursorPageResponse<ContentSummaryResponse> findAll(ContentListRequest request) {
        boolean ratingSort = "averageRating".equals(request.getSortBy());
        Instant time = null;
        BigDecimal rating = null;
        if ((request.getCursor() == null) != (request.getIdAfter() == null)) {
            throw invalid("cursor와 idAfter는 함께 지정해야 합니다.");
        }
        if (request.getCursor() != null) {
            try {
                if (ratingSort) {
                    rating = new BigDecimal(request.getCursor());
                    if (rating.signum() < 0 || rating.compareTo(new BigDecimal("5")) > 0
                            || rating.stripTrailingZeros().scale() > 2) {
                        throw new IllegalArgumentException();
                    }
                } else {
                    time = Instant.parse(request.getCursor());
                }
            } catch (RuntimeException e) {
                throw invalid("정렬 기준에 맞지 않는 cursor입니다.");
            }
        }
        var found = queries.search(new ContentSearch(request.getTypeEqual(), request.getGenreIdEqual(),
                request.getSportTypeEqual(), request.getKeywordLike(), request.getLikedByUserIdEqual(),
                ratingSort, request.getSortDirection() == SortDirection.ASCENDING,
                time, rating, request.getIdAfter(), request.getLimit()));
        boolean hasNext = found.contents().size() > request.getLimit();
        List<Content> page = found.contents().stream().limit(request.getLimit()).toList();
        Content last = hasNext ? page.get(page.size() - 1) : null;
        return CursorPageResponse.<ContentSummaryResponse>builder()
                .data(page.stream().map(this::summary).toList()).hasNext(hasNext)
                .nextCursor(last == null ? null : ratingSort
                        ? last.getAverageRating().toPlainString() : last.getCreatedAt().toString())
                .nextIdAfter(last == null ? null : last.getId()).totalCount(found.totalCount())
                .sortBy(request.getSortBy()).sortDirection(request.getSortDirection()).build();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ContentDetailResponse create(ContentCreateRequest request) {
        ContentType type = ContentType.fromValue(request.getType());
        Content parent = null;
        if (request.getParentContentId() != null) {
            parent = required(request.getParentContentId(), true);
            if (parent.getType() != ContentType.TV_SERIES) {
                throw invalid("시즌의 부모는 TV 시리즈여야 합니다.");
            }
            if (contents.existsByParentContent_IdAndSeasonNumber(parent.getId(), request.getSeasonNumber())) {
                throw new BaseException(ErrorCode.CONTENT_CONFLICT);
            }
        }
        Content entity = Content.builder().title(request.getTitle()).type(type)
                .description(request.getDescription()).thumbnailUrl(request.getThumbnailUrl())
                .parentContent(parent).seasonNumber(request.getSeasonNumber()).seasonCount(request.getSeasonCount())
                .episodeCount(request.getEpisodeCount()).sportType(request.getSportType())
                .releaseDate(request.getReleaseDate()).runtime(request.getRuntime()).metadata(request.getMetadata()).build();
        try {
            contents.saveAndFlush(entity);
            if (request.getTags() != null) relations.replaceManualTags(entity.getId(), normalizeTags(request.getTags()));
        } catch (DataIntegrityViolationException e) {
            throw new BaseException(ErrorCode.CONTENT_CONFLICT, e);
        }
        return detail(entity);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ContentDetailResponse update(UUID id, ContentUpdateRequest request) {
        Content entity = required(id, true);
        ContentType type = request.getType() == null ? entity.getType() : ContentType.fromValue(request.getType());
        if (type != entity.getType()) {
            if (type == ContentType.TV_SEASON || type == ContentType.SPORT) {
                throw invalid("시즌 또는 스포츠로 변경하려면 추가 관계 정보가 필요하므로 이 수정 요청으로 변경할 수 없습니다.");
            }
            if (!contents.findAllByParentContent_IdOrderBySeasonNumberAsc(id).isEmpty()
                    || !episodes.findAllBySeason_IdOrderByEpisodeNumberAsc(id).isEmpty()) {
                throw new BaseException(ErrorCode.CONTENT_CONFLICT);
            }
        }
        entity.updateDetails(request.getTitle(), request.getDescription(), request.getThumbnailUrl(), type);
        try {
            contents.flush();
            if (request.getTags() != null) relations.replaceManualTags(id, normalizeTags(request.getTags()));
        } catch (DataIntegrityViolationException e) {
            throw new BaseException(ErrorCode.CONTENT_CONFLICT, e);
        }
        return detail(entity);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID id) {
        Content root = required(id, true);
        List<Content> targets = new ArrayList<>();
        targets.add(root);
        // 시즌을 잠그면 해당 시즌을 참조하는 새 Watch Party의 FK 삽입과 삭제가 경합하지 않는다.
        for (Content season : contents.findAllByParentContent_IdOrderBySeasonNumberAsc(id)) {
            targets.add(required(season.getId(), true));
        }
        for (Content target : targets) {
            if (relations.hasWatchParty(target.getId())) {
                BaseException exception = new BaseException(ErrorCode.CONTENT_DELETE_RESTRICTED);
                exception.addDetail("contentId", target.getId());
                throw exception;
            }
        }
        var deleted = targets.stream().map(c ->
                new ContentDeletedEvent.DeletedContent(c.getId(), c.getThumbnailUrl())).toList();
        try {
            // schema.sql의 CASCADE로 시즌·회차·리뷰 등 종속 데이터를 함께 삭제한다.
            contents.deleteByIdInDatabase(id);
            contents.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BaseException(ErrorCode.CONTENT_CONFLICT, e);
        }
        events.publishEvent(new ContentDeletedEvent(deleted));
    }

    private Content required(UUID id, boolean lock) {
        return (lock ? contents.findByIdForUpdate(id) : contents.findById(id)).orElseThrow(() -> {
            BaseException exception = new BaseException(ErrorCode.CONTENT_NOT_FOUND);
            exception.addDetail("contentId", id);
            return exception;
        });
    }

    private List<String> normalizeTags(List<String> tags) {
        return tags.stream().map(String::strip).distinct().sorted().toList();
    }

    private BaseException invalid(String reason) {
        BaseException exception = new BaseException(ErrorCode.INVALID_REQUEST);
        exception.addDetail("reason", reason);
        return exception;
    }

    private ContentSummaryResponse summary(Content c) {
        return ContentSummaryResponse.builder().id(c.getId()).title(c.getTitle()).type(c.getType().getValue())
                .thumbnailUrl(c.getThumbnailUrl()).sportType(c.getSportType()).seasonNumber(c.getSeasonNumber())
                .releaseDate(c.getReleaseDate()).averageRating(c.getAverageRating()).likeCount(c.getLikeCount())
                .reviewCount(c.getReviewCount()).createdAt(c.getCreatedAt()).build();
    }

    private ContentDetailResponse detail(Content c) {
        UUID seriesId = c.getType() == ContentType.TV_SERIES ? c.getId()
                : c.getParentContent() == null ? null : c.getParentContent().getId();
        var seasons = seriesId == null ? List.<ContentSummaryResponse>of()
                : contents.findAllByParentContent_IdOrderBySeasonNumberAsc(seriesId).stream().map(this::summary).toList();
        var episodeList = c.getType() != ContentType.TV_SEASON ? List.<EpisodeResponse>of()
                : episodes.findAllBySeason_IdOrderByEpisodeNumberAsc(c.getId()).stream().map(e ->
                EpisodeResponse.builder().id(e.getId()).episodeNumber(e.getEpisodeNumber()).title(e.getTitle())
                        .description(e.getDescription()).stillImageUrl(e.getStillImageUrl()).runtime(e.getRuntime())
                        .airDate(e.getAirDate()).build()).toList();
        return ContentDetailResponse.builder().id(c.getId())
                .parentContentId(c.getParentContent() == null ? null : c.getParentContent().getId())
                .title(c.getTitle()).type(c.getType().getValue()).description(c.getDescription())
                .thumbnailUrl(c.getThumbnailUrl()).sportType(c.getSportType()).seasonNumber(c.getSeasonNumber())
                .seasonCount(c.getSeasonCount()).episodeCount(c.getEpisodeCount()).releaseDate(c.getReleaseDate())
                .runtime(c.getRuntime()).metadata(c.getMetadata()).averageRating(c.getAverageRating())
                .likeCount(c.getLikeCount()).reviewCount(c.getReviewCount()).createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt()).episodes(episodeList).seasons(seasons)
                .genres(relations.genres(c.getId())).tags(relations.tags(c.getId()))
                .casts(relations.casts(c.getId())).otts(relations.otts(c.getId()))
                .playlists(relations.playlists(c.getId())).watchParties(relations.watchParties(c.getId(), Instant.now())).build();
    }
}

