package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.watchparty.dto.CreateWatchPartyRequest;
import com.moduplaylist.api.watchparty.dto.WatchPartyResponse;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.exception.WatchPartyInvalidEpisodeRangeException;
import com.moduplaylist.core.watchparty.exception.WatchPartyNotFoundException;
import com.moduplaylist.core.watchparty.repository.WatchPartyQueryRepository;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import com.moduplaylist.core.watchparty.repository.WatchPartySearch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchPartyService {

    private final WatchPartyRepository watchPartyRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final WatchPartyQueryRepository watchPartyQueryRepository;

    public WatchPartyResponse createWatchParty(UUID hostId, CreateWatchPartyRequest request) {

        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new UserNotFoundException(hostId));

        Content content = contentRepository.findById(request.getContentId())
                .orElseThrow(() -> new ContentNotFoundException(request.getContentId()));

        validateEpisodeRange(content, request.getStartEpisode(), request.getEndEpisode());

        WatchParty watchParty = WatchParty.builder()
                .host(host)
                .contentId(content.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .scheduledAt(request.getScheduledAt())
                .maxParticipants(request.getMaxParticipants())
                .startEpisode(request.getStartEpisode())
                .endEpisode(request.getEndEpisode())
                .build();

        WatchParty saved = watchPartyRepository.save(watchParty);

        return toResponse(saved, host, content);
    }

    private void validateEpisodeRange(Content content, Integer startEpisode, Integer endEpisode) {
        boolean hasEpisodeRange = startEpisode != null || endEpisode != null;
        boolean isEpisodicContent = content.getType() == ContentType.TV_SEASON;

        if (hasEpisodeRange && !isEpisodicContent) {
            throw new WatchPartyInvalidEpisodeRangeException(content.getId());
        }
    }

    @Transactional(readOnly = true)
    public WatchPartyResponse getWatchParty(UUID partyId) {
        WatchParty watchParty = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new WatchPartyNotFoundException(partyId));
        return toResponse(watchParty);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<WatchPartyResponse> getWatchParties(
            WatchPartyStatus statusEqual, UUID contentIdEqual,
            String cursor, UUID idAfter, int limit, SortDirection sortDirection) {

        boolean ascending = sortDirection == SortDirection.ASCENDING;
        Instant cursorScheduledAt = (cursor != null) ? Instant.parse(cursor) : null;

        WatchPartySearch search = WatchPartySearch.builder()
                .statusEqual(statusEqual)
                .contentIdEqual(contentIdEqual)
                .cursorScheduledAt(cursorScheduledAt)
                .cursorId(idAfter)
                .ascending(ascending)
                .limit(limit)
                .build();

        WatchPartyQueryRepository.SearchResult result = watchPartyQueryRepository.search(search);

        List<WatchPartyResponse> data = result.getWatchParties().stream()
                .map(this::toResponse)
                .toList();

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (!result.getWatchParties().isEmpty()) {
            WatchParty last = result.getWatchParties().get(result.getWatchParties().size() - 1);
            nextCursor = last.getScheduledAt().toString();
            nextIdAfter = last.getId();
        }

        return CursorPageResponse.<WatchPartyResponse>builder()
                .data(data)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(result.isHasNext())
                .totalCount(result.getTotalCount())
                .sortBy("scheduledAt")
                .sortDirection(sortDirection)
                .build();
    }

    private WatchPartyResponse toResponse(WatchParty watchParty) {
        Content content = contentRepository.findById(watchParty.getContentId())
                .orElseThrow(() -> new ContentNotFoundException(watchParty.getContentId()));
        return toResponse(watchParty, watchParty.getHost(), content);
    }


    private WatchPartyResponse toResponse(WatchParty watchParty, User host, Content content) {
        WatchPartyResponse.HostSummary hostSummary = new WatchPartyResponse.HostSummary(
                host.getId(), host.getName(), host.getProfileImageUrl());

        WatchPartyResponse.ContentSummary contentSummary = new WatchPartyResponse.ContentSummary(
                content.getId(), content.getType().getValue(), content.getTitle(), content.getThumbnailUrl());

        return new WatchPartyResponse(
                watchParty.getId(),
                hostSummary,
                contentSummary,
                watchParty.getTitle(),
                watchParty.getDescription(),
                watchParty.getScheduledAt(),
                watchParty.getStatus(),
                watchParty.getMaxParticipants(),
                0, // 생성 시점 참가자 수 - host는 미포함이라 항상 0
                watchParty.getStartEpisode(),
                watchParty.getEndEpisode(),
                watchParty.getCreatedAt(),
                watchParty.getEndedAt()
        );
    }
}