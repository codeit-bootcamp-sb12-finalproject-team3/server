package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.content.dto.ContentWatchPartyResponse;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.watchparty.event.WatchPartyCreatedEvent;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final WatchPartyPlaybackRegistry watchPartyPlaybackRegistry;
    private final ApplicationEventPublisher eventPublisher;

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

        eventPublisher.publishEvent(new WatchPartyCreatedEvent(
                UUID.randomUUID(), saved.getId(), hostId, content.getId(), saved.getScheduledAt()
        ));

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

        if (contentIdEqual != null && statusEqual != null) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }

        boolean contentSearch = contentIdEqual != null;
        boolean ascending = sortDirection == SortDirection.ASCENDING;
        ContentCursor contentCursor = contentSearch ? parseContentCursor(cursor, idAfter) : null;
        Instant cursorScheduledAt = contentSearch
                ? contentCursor.scheduledAt()
                : (cursor != null ? Instant.parse(cursor) : null);

        if (contentSearch) {
            validateContentForWatchParty(contentIdEqual);
        }

        WatchPartySearch search = WatchPartySearch.builder()
                .statusEqual(statusEqual)
                .contentIdEqual(contentIdEqual)
                .cursorScheduledAt(cursorScheduledAt)
                .cursorId(idAfter)
                .cursorStatus(contentSearch ? contentCursor.status() : null)
                .contentScheduledAtFrom(contentSearch ? Instant.now().minus(1, ChronoUnit.HOURS) : null)
                .ascending(ascending)
                .limit(limit)
                .build();

        WatchPartyQueryRepository.SearchResult result = watchPartyQueryRepository.search(search);

        List<WatchParty> watchParties = result.getWatchParties();
        Map<UUID, Content> contentById = fetchContentMap(watchParties);
        Map<UUID, Integer> participantCountById = fetchParticipantCountMap(watchParties);

        List<WatchPartySummaryResponse> data = watchParties.stream()
                .map(wp -> toSummaryResponse(wp, contentById, participantCountById))
                .toList();

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (!result.getWatchParties().isEmpty()) {
            WatchParty last = result.getWatchParties().get(result.getWatchParties().size() - 1);
            nextCursor = contentSearch
                    ? formatContentCursor(last)
                    : last.getScheduledAt().toString();
            nextIdAfter = last.getId();
        }

        return CursorPageResponse.<WatchPartySummaryResponse>builder()
                .data(data)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(result.isHasNext())
                .totalCount(result.getTotalCount())
                .sortBy("scheduledAt")
                .sortDirection(contentSearch ? SortDirection.ASCENDING : sortDirection)
                .build();
    }

    @Transactional(readOnly = true)
    public ContentWatchPartyResponse getWatchPartiesForContentWidget(UUID contentId) {
        WatchPartySearch search = WatchPartySearch.builder()
                .contentIdEqual(contentId)
                .contentScheduledAtFrom(Instant.now().minus(1, ChronoUnit.HOURS))
                .limit(CONTENT_WIDGET_LIMIT)
                .build();
        WatchPartyQueryRepository.SearchResult result = watchPartyQueryRepository.search(search);
        List<WatchParty> watchParties = result.getWatchParties();
        Map<UUID, Content> contentById = fetchContentMap(watchParties);
        Map<UUID, Integer> participantCountById = fetchParticipantCountMap(watchParties);

        List<WatchPartySummaryResponse> items = watchParties.stream()
                .map(wp -> toSummaryResponse(wp, contentById, participantCountById))
                .toList();

        return ContentWatchPartyResponse.builder()
                .data(items)
                .hasMore(result.isHasNext())
                .build();
    }

    private void validateContentForWatchParty(UUID contentId) {
        Content content = contentRepository.findByIdAndHiddenFalse(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        validateWatchPartyContent(content);
    }

    private ContentCursor parseContentCursor(String cursor, UUID idAfter) {
        if (cursor == null && idAfter == null) {
            return new ContentCursor(null, null);
        }
        if (cursor == null || idAfter == null) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
        try {
            int separator = cursor.indexOf('|');
            if (separator <= 0 || separator == cursor.length() - 1) {
                throw new IllegalArgumentException("Invalid content watch party cursor");
            }
            WatchPartyStatus status = WatchPartyStatus.valueOf(cursor.substring(0, separator));
            if (status != WatchPartyStatus.LIVE && status != WatchPartyStatus.SCHEDULED) {
                throw new IllegalArgumentException("Invalid content watch party cursor status");
            }
            return new ContentCursor(status, Instant.parse(cursor.substring(separator + 1)));
        } catch (IllegalArgumentException e) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, e);
        }
    }

    private String formatContentCursor(WatchParty watchParty) {
        return watchParty.getStatus().name() + "|" + watchParty.getScheduledAt();
    }

    private record ContentCursor(WatchPartyStatus status, Instant scheduledAt) {}

    private WatchPartyResponse toResponse(WatchParty watchParty) {
        Content content = contentRepository.findById(watchParty.getContentId())
                .orElseThrow(() -> new ContentNotFoundException(watchParty.getContentId()));
        int currentParticipants = (int) watchPartyParticipantRepository
                .countByWatchParty_IdAndStatus(watchParty.getId(), ParticipantStatus.JOINED);
        return toResponse(watchParty, watchParty.getHost(), content, currentParticipants);
    }

    private WatchPartyResponse toResponse(WatchParty watchParty, User host, Content content, int currentParticipants) {
        WatchPartyPlaybackState playback = watchParty.getStatus() == WatchPartyStatus.LIVE
                ? watchPartyPlaybackRegistry.find(watchParty.getId()).orElse(null)
                : null;

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
                watchParty.getEndedAt(),
                playback != null ? playback.getStatus() : null,
                playback != null ? playback.getStartedAt() : null,
                playback != null ? playback.getAccumulatedPauseMs() : null,
                playback != null ? playback.getPausedAt() : null
        );
    }

    private WatchPartySummaryResponse toSummaryResponse(
            WatchParty watchParty, Map<UUID, Content> contentById, Map<UUID, Integer> participantCountById) {
        Content content = contentById.get(watchParty.getContentId());
        if (content == null) {
            throw new ContentNotFoundException(watchParty.getContentId());
        }
        int currentParticipants = participantCountById.getOrDefault(watchParty.getId(), 0);

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

    private Map<UUID, Content> fetchContentMap(List<WatchParty> watchParties) {
        List<UUID> contentIds = watchParties.stream()
                .map(WatchParty::getContentId)
                .distinct()
                .toList();
        return contentRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(Content::getId, Function.identity()));
    }

    private Map<UUID, Integer> fetchParticipantCountMap(List<WatchParty> watchParties) {
        List<UUID> watchPartyIds = watchParties.stream()
                .map(WatchParty::getId)
                .toList();
        Map<UUID, Integer> counts = watchPartyParticipantRepository
                .countByWatchPartyIdsAndStatus(watchPartyIds, ParticipantStatus.JOINED)
                .stream()
                .collect(Collectors.toMap(
                        WatchPartyParticipantRepository.ParticipantCount::getWatchPartyId,
                        p -> (int) p.getCount()
                ));
        watchParties.forEach(wp -> counts.putIfAbsent(wp.getId(), 0));
        return counts;
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
