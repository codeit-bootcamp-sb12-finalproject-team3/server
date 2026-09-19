package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.content.dto.ContentWatchPartyItemResponse;
import com.moduplaylist.api.content.dto.ContentWatchPartyResponse;
import com.moduplaylist.api.content.dto.WatchPartyDisplayStatus;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.user.dto.UserSummary;
import com.moduplaylist.api.watchparty.dto.CreateWatchPartyRequest;
import com.moduplaylist.api.watchparty.dto.WatchPartyContentSummary;
import com.moduplaylist.api.watchparty.dto.WatchPartyResponse;
import com.moduplaylist.api.watchparty.dto.WatchPartySummaryResponse;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.exception.ContentTypeNotSupportedException;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.exception.WatchPartyInvalidEpisodeRangeException;
import com.moduplaylist.core.watchparty.exception.WatchPartyNotFoundException;
import com.moduplaylist.core.watchparty.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchPartyService {

    private static final int CONTENT_WIDGET_LIMIT = 20;

    private final WatchPartyRepository watchPartyRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final WatchPartyQueryRepository watchPartyQueryRepository;
    private final WatchPartyParticipantRepository watchPartyParticipantRepository;
    private final WatchPartyHostRegistry watchPartyHostRegistry;

    public WatchPartyResponse createWatchParty(UUID hostId, CreateWatchPartyRequest request) {

        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new UserNotFoundException(hostId));

        Content content = contentRepository.findById(request.getContentId())
                .orElseThrow(() -> new ContentNotFoundException(request.getContentId()));

        validateWatchPartyContent(content);
        validateEpisodeRange(content, request.getStartEpisode(), request.getEndEpisode());

        WatchParty watchParty = WatchParty.builder()
                .host(host)
                .contentId(content.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .scheduledAt(request.getScheduledAt())
                .maxParticipants(request.getMaxParticipants())
                .sessionDurationMinutes(request.getSessionDurationMinutes())
                .startEpisode(request.getStartEpisode())
                .endEpisode(request.getEndEpisode())
                .build();

        WatchParty saved = watchPartyRepository.save(watchParty);
        watchPartyHostRegistry.setHost(saved.getId(), hostId);

        return toResponse(saved, host, content, 0);
    }

    private void validateEpisodeRange(Content content, Integer startEpisode, Integer endEpisode) {
        boolean hasEpisodeRange = startEpisode != null || endEpisode != null;
        boolean isEpisodicContent = content.getType() == ContentType.TV_SEASON;

        if (hasEpisodeRange && !isEpisodicContent) {
            throw new WatchPartyInvalidEpisodeRangeException(content.getId());
        }
    }

    private void validateWatchPartyContent(Content content) {
        if (!content.getType().isPersonalizable()) {
            throw new ContentTypeNotSupportedException(content.getId(), content.getType());
        }
    }

    @Transactional(readOnly = true)
    public WatchPartyResponse getWatchParty(UUID partyId) {
        WatchParty watchParty = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new WatchPartyNotFoundException(partyId));
        return toResponse(watchParty);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<WatchPartySummaryResponse> getWatchParties(
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

        List<WatchPartySummaryResponse> data = result.getWatchParties().stream()
                .map(this::toSummaryResponse)
                .toList();

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (!result.getWatchParties().isEmpty()) {
            WatchParty last = result.getWatchParties().get(result.getWatchParties().size() - 1);
            nextCursor = last.getScheduledAt().toString();
            nextIdAfter = last.getId();
        }

        return CursorPageResponse.<WatchPartySummaryResponse>builder()
                .data(data)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(result.isHasNext())
                .totalCount(result.getTotalCount())
                .sortBy("scheduledAt")
                .sortDirection(sortDirection)
                .build();
    }

    @Transactional(readOnly = true)
    public ContentWatchPartyResponse getWatchPartiesForContentWidget(UUID contentId) {
        Instant now = Instant.now();
        Instant liveWindowStart = now.minus(1, ChronoUnit.HOURS);

        List<WatchParty> fetched = watchPartyQueryRepository
                .findContentWidgetItems(contentId, now, liveWindowStart, CONTENT_WIDGET_LIMIT + 1);

        boolean hasMore = fetched.size() > CONTENT_WIDGET_LIMIT;
        List<WatchParty> watchParties = hasMore ? fetched.subList(0, CONTENT_WIDGET_LIMIT) : fetched;

        List<ContentWatchPartyItemResponse> items = watchParties.stream()
                .map(w -> toWidgetItem(w, now))
                .toList();

        return ContentWatchPartyResponse.builder()
                .data(items)
                .hasMore(hasMore)
                .build();
    }

    private ContentWatchPartyItemResponse toWidgetItem(WatchParty watchParty, Instant now) {
        WatchPartyDisplayStatus displayStatus = watchParty.getScheduledAt().isAfter(now)
                ? WatchPartyDisplayStatus.SCHEDULED
                : WatchPartyDisplayStatus.LIVE;

        int currentParticipants = (int) watchPartyParticipantRepository
                .countByWatchParty_IdAndStatus(watchParty.getId(), ParticipantStatus.JOINED);

        return ContentWatchPartyItemResponse.builder()
                .id(watchParty.getId())
                .title(watchParty.getTitle())
                .displayStatus(displayStatus)
                .scheduledAt(watchParty.getScheduledAt())
                .participantCount(currentParticipants)
                .maxParticipants(watchParty.getMaxParticipants())
                .build();
    }

    private WatchPartyResponse toResponse(WatchParty watchParty) {
        Content content = contentRepository.findById(watchParty.getContentId())
                .orElseThrow(() -> new ContentNotFoundException(watchParty.getContentId()));
        int currentParticipants = (int) watchPartyParticipantRepository
                .countByWatchParty_IdAndStatus(watchParty.getId(), ParticipantStatus.JOINED);
        return toResponse(watchParty, watchParty.getHost(), content, currentParticipants);
    }

    private WatchPartyResponse toResponse(WatchParty watchParty, User host, Content content, int currentParticipants) {
        return new WatchPartyResponse(
                watchParty.getId(),
                toHostSummary(host),
                toContentSummary(content),
                watchParty.getTitle(),
                watchParty.getDescription(),
                watchParty.getScheduledAt(),
                watchParty.getStatus(),
                watchParty.getMaxParticipants(),
                watchParty.getSessionDurationMinutes(),
                currentParticipants,
                watchParty.getStartEpisode(),
                watchParty.getEndEpisode(),
                watchParty.getCreatedAt(),
                watchParty.getEndedAt()
        );
    }
<<<<<<< HEAD
}
=======

    private WatchPartySummaryResponse toSummaryResponse(WatchParty watchParty) {
        Content content = contentRepository.findById(watchParty.getContentId())
                .orElseThrow(() -> new ContentNotFoundException(watchParty.getContentId()));
        int currentParticipants = (int) watchPartyParticipantRepository
                .countByWatchParty_IdAndStatus(watchParty.getId(), ParticipantStatus.JOINED);

        return WatchPartySummaryResponse.builder()
                .id(watchParty.getId())
                .host(toHostSummary(watchParty.getHost()))
                .content(toContentSummary(content))
                .title(watchParty.getTitle())
                .scheduledAt(watchParty.getScheduledAt())
                .status(watchParty.getStatus())
                .maxParticipants(watchParty.getMaxParticipants())
                .currentParticipantCount(currentParticipants)
                .createdAt(watchParty.getCreatedAt())
                .build();
    }

    private UserSummary toHostSummary(User host) {
        return UserSummary.builder()
                .userId(host.getId())
                .name(host.getName())
                .profileImageUrl(host.getProfileImageUrl())
                .build();
    }

    private WatchPartyContentSummary toContentSummary(Content content) {
        return WatchPartyContentSummary.builder()
                .id(content.getId())
                .type(content.getType().getValue())
                .title(content.getTitle())
                .thumbnailUrl(content.getThumbnailUrl())
                .build();
    }
}
>>>>>>> origin/int
