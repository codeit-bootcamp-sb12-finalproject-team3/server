package com.moduplaylist.api.recommendation.service.impl;

import com.moduplaylist.api.content.dto.ContentSummary;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.playlist.dto.PlaylistSummaryResponse;
import com.moduplaylist.api.recommendation.service.PlaylistRecommendationQueryService;
import com.moduplaylist.api.user.dto.UserSummary;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.entity.PlaylistContent;
import com.moduplaylist.core.playlist.entity.PlaylistSubscription;
import com.moduplaylist.core.playlist.repository.PlaylistContentQueryRepository;
import com.moduplaylist.core.playlist.repository.PlaylistQueryRepository;
import com.moduplaylist.core.playlist.repository.PlaylistSubscriptionRepository;
import com.moduplaylist.infrastructure.redis.recommendation.PlaylistRecommendationRedisRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistRecommendationQueryServiceImpl
        implements PlaylistRecommendationQueryService {

    private static final int MAX_LIMIT = 100;
    private static final int PREVIEW_CONTENT_LIMIT = 4;

    private final PlaylistRecommendationRedisRepository recommendationRedisRepository;
    private final PlaylistQueryRepository playlistQueryRepository;
    private final PlaylistSubscriptionRepository subscriptionRepository;
    private final PlaylistContentQueryRepository playlistContentQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<PlaylistSummaryResponse> findRecommendations(
            UUID userId,
            String cursor,
            int limit
    ) {
        long offset = parseOffset(cursor, limit);
        List<UUID> cachedIds = recommendationRedisRepository.findAll(userId).stream()
                .distinct()
                .toList();
        List<PlaylistQueryRepository.Item> recommendableItems =
                findCurrentlyRecommendable(userId, cachedIds);
        long totalCount = recommendableItems.size();

        List<PlaylistQueryRepository.Item> pageItems = offset >= totalCount
                ? List.of()
                : recommendableItems.subList(
                        (int) offset,
                        (int) Math.min(totalCount, offset + limit)
                );
        List<UUID> pageIds = pageItems.stream()
                .map(item -> item.getPlaylist().getId())
                .toList();
        Map<UUID, List<ContentSummary>> previews = findPreviewContents(pageIds);
        List<PlaylistSummaryResponse> data = pageItems.stream()
                .map(item -> toResponse(item, previews))
                .toList();

        boolean hasNext = offset + pageItems.size() < totalCount;
        return CursorPageResponse.<PlaylistSummaryResponse>builder()
                .data(data)
                .nextCursor(hasNext ? Long.toString(offset + pageItems.size()) : null)
                .nextIdAfter(hasNext && !pageIds.isEmpty()
                        ? pageIds.get(pageIds.size() - 1)
                        : null)
                .hasNext(hasNext)
                .totalCount(totalCount)
                .sortBy("similarity")
                .sortDirection(SortDirection.DESCENDING)
                .build();
    }

    private long parseOffset(String cursor, int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
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

    private List<PlaylistQueryRepository.Item> findCurrentlyRecommendable(
            UUID userId,
            List<UUID> cachedIds
    ) {
        if (cachedIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, PlaylistQueryRepository.Item> itemsById = new HashMap<>();
        playlistQueryRepository.findAllByIds(cachedIds)
                .forEach(item -> itemsById.put(item.getPlaylist().getId(), item));
        Set<UUID> subscribedIds = new HashSet<>(
                subscriptionRepository.findAllByUser_IdAndPlaylist_IdIn(userId, cachedIds)
                        .stream()
                        .map(PlaylistSubscription::getPlaylist)
                        .map(Playlist::getId)
                        .toList()
        );

        return cachedIds.stream()
                .map(itemsById::get)
                .filter(item -> item != null
                        && !item.getPlaylist().getOwner().getId().equals(userId)
                        && !subscribedIds.contains(item.getPlaylist().getId()))
                .toList();
    }

    private Map<UUID, List<ContentSummary>> findPreviewContents(List<UUID> playlistIds) {
        Map<UUID, List<ContentSummary>> previews = new HashMap<>();
        if (playlistIds.isEmpty()) {
            return previews;
        }

        for (PlaylistContent playlistContent : playlistContentQueryRepository.findPreviewContents(
                playlistIds, PREVIEW_CONTENT_LIMIT
        )) {
            previews.computeIfAbsent(
                    playlistContent.getPlaylist().getId(),
                    ignored -> new ArrayList<>()
            ).add(ContentSummary.from(playlistContent.getContent()));
        }
        return previews;
    }

    private PlaylistSummaryResponse toResponse(
            PlaylistQueryRepository.Item item,
            Map<UUID, List<ContentSummary>> previews
    ) {
        Playlist playlist = item.getPlaylist();
        return PlaylistSummaryResponse.builder()
                .id(playlist.getId())
                .owner(UserSummary.from(playlist.getOwner()))
                .title(playlist.getTitle())
                .description(playlist.getDescription())
                .createdAt(playlist.getCreatedAt())
                .subscriberCount(item.getSubscriberCount())
                .subscribedByMe(false)
                .contentCount(item.getContentCount())
                .previewContents(previews.getOrDefault(playlist.getId(), List.of()))
                .build();
    }
}
