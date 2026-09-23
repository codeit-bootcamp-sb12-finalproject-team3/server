package com.moduplaylist.api.trending.service.impl;

import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.content.dto.ContentSummaryType;
import com.moduplaylist.api.content.service.ContentSummaryResponseAssembler;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.trending.service.TrendingService;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.redis.trending.TrendingContentRedisRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrendingServiceImpl implements TrendingService {

    private static final int TOP_LIMIT = 10;
    private static final int CANDIDATE_LIMIT = 100;

    private final TrendingContentRedisRepository trendingRedisRepository;
    private final ContentRepository contentRepository;
    private final ContentSummaryResponseAssembler contentSummaryResponseAssembler;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ContentSummaryResponse> findTrendingContents(UUID userId) {
        List<UUID> rankedContentIds = trendingRedisRepository
                .findTopContentIds(CANDIDATE_LIMIT);
        List<Content> rankedContents = loadVisibleContentsInRankOrder(rankedContentIds);
        List<ContentSummaryResponse> data = contentSummaryResponseAssembler
                .toResponses(rankedContents, userId);

        return CursorPageResponse.<ContentSummaryResponse>builder()
                .data(data)
                .nextCursor(null)
                .nextIdAfter(null)
                .hasNext(false)
                .totalCount((long) data.size())
                .sortBy("trendingScore")
                .sortDirection(SortDirection.DESCENDING)
                .build();
    }

    private List<Content> loadVisibleContentsInRankOrder(List<UUID> rankedContentIds) {
        if (rankedContentIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Content> contentById = new HashMap<>();
        contentRepository.findAllById(rankedContentIds)
                .forEach(content -> contentById.put(content.getId(), content));

        return rankedContentIds.stream()
                .map(contentById::get)
                .filter(this::isVisibleSummaryContent)
                .limit(TOP_LIMIT)
                .toList();
    }

    private boolean isVisibleSummaryContent(Content content) {
        if (content == null || content.isHidden()) {
            return false;
        }

        try {
            ContentSummaryType.from(content.getType());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
