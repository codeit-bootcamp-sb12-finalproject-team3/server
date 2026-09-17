package com.moduplaylist.api.recommendation.service.impl;

import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.content.dto.ContentSummaryType;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.recommendation.service.ContentRecommendationQueryService;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.redis.recommendation.ContentRecommendationRedisRepository;
import com.moduplaylist.infrastructure.redis.recommendation.RecommendationCachePage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentRecommendationQueryServiceImpl
        implements ContentRecommendationQueryService {

    private static final int MAX_LIMIT = 100;

    private final ContentRecommendationRedisRepository recommendationRedisRepository;
    private final ContentRepository contentRepository;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ContentSummaryResponse> findRecommendations(
            UUID userId,
            String cursor,
            int limit
    ) {
        long offset = parseOffset(cursor, limit);
        RecommendationCachePage cachePage =
                recommendationRedisRepository.findPage(userId, offset, limit);
        List<ContentSummaryResponse> data = loadInRecommendationOrder(cachePage.contentIds());
        boolean hasNext = offset + cachePage.contentIds().size() < cachePage.totalCount();

        String nextCursor = hasNext ? Long.toString(offset + cachePage.contentIds().size()) : null;
        UUID nextIdAfter = hasNext && !cachePage.contentIds().isEmpty()
                ? cachePage.contentIds().get(cachePage.contentIds().size() - 1)
                : null;

        return CursorPageResponse.<ContentSummaryResponse>builder()
                .data(data)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(hasNext)
                .totalCount(cachePage.totalCount())
                .sortBy("similarity")
                .sortDirection(SortDirection.DESCENDING)
                .build();
    }

    private long parseOffset(String cursor, int limit) {
        if (limit < 1 || limit > MAX_LIMIT ) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
        if (cursor == null) {
            return 0L;
        }

        try {
            long offset = Long.parseLong(cursor);
            if (offset < 0) {
                throw new NumberFormatException("negative cursor");
            }
            return offset;
        } catch (NumberFormatException exception) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, exception);
        }
    }

    private List<ContentSummaryResponse> loadInRecommendationOrder(List<UUID> contentIds) {
        Map<UUID, Content> contentById = new HashMap<>();
        contentRepository.findAllById(contentIds)
                .forEach(content -> contentById.put(content.getId(), content));

        return contentIds.stream()
                .map(contentById::get)
                .filter(content ->
                        content != null && content.getType().isPersonalizable())
                .map(this::toResponse)
                .toList();
    }

    private ContentSummaryResponse toResponse(Content content) {
        return ContentSummaryResponse.builder()
            .id(content.getId())
            .title(content.getTitle())
            .type(ContentSummaryType.from(content.getType()))
            .thumbnailUrl(content.getThumbnailUrl())
            .seasonNumber(content.getSeasonNumber())
            .releaseDate(content.getReleaseDate())
            .averageRating(content.getAverageRating())
            .likeCount(content.getLikeCount())
            .reviewCount(content.getReviewCount())
            .build();
    }
}
