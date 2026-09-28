package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.content.dto.ContentWatchPartyResponse;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.watchparty.dto.*;
import com.moduplaylist.api.watchparty.event.WatchPartyCreatedEvent;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.api.user.dto.UserSummary;
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
import com.moduplaylist.core.watchparty.exception.*;
import com.moduplaylist.core.watchparty.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchPartyService {

    private static final int CONTENT_WIDGET_LIMIT = 20;
    // 정원 상한(현재는 30명). 참가자 목록·방송 부하와 방 분위기를 고려한 값
    static final int MAX_PARTICIPANTS_LIMIT = 30;

    private final WatchPartyRepository watchPartyRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final WatchPartyQueryRepository watchPartyQueryRepository;
    private final WatchPartyParticipantRepository watchPartyParticipantRepository;
    private final WatchPartyHostRegistry watchPartyHostRegistry;
    private final WatchPartyPlaybackRegistry watchPartyPlaybackRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final WatchPartyReminderRepository watchPartyReminderRepository;

    public WatchPartyResponse createWatchParty(UUID hostId, CreateWatchPartyRequest request) {

        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new UserNotFoundException(hostId));

        Content content = contentRepository.findById(request.getContentId())
                .orElseThrow(() -> new ContentNotFoundException(request.getContentId()));

        validateWatchPartyContent(content);
        validateEpisodeRange(content, request.getStartEpisode(), request.getEndEpisode());
        validateMaxParticipantsLimit(request.getMaxParticipants());

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

    public WatchPartyResponse updateWatchParty(UUID requesterId, UUID partyId, UpdateWatchPartyRequest request) {
        WatchParty watchParty = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new WatchPartyNotFoundException(partyId));

        if (!watchParty.getHost().getId().equals(requesterId)) {
            throw new WatchPartyHostOnlyException(partyId, requesterId);
        }

        Content content = contentRepository.findById(watchParty.getContentId())
                .orElseThrow(() -> new ContentNotFoundException(watchParty.getContentId()));
        validateEpisodeRange(content, request.getStartEpisode(), request.getEndEpisode());
        if (!Objects.equals(watchParty.getMaxParticipants(), request.getMaxParticipants())) {
            validateMaxParticipantsLimit(request.getMaxParticipants());
        }
        validateMaxParticipants(partyId, request.getMaxParticipants());

        watchParty.update(
                request.getTitle(),
                request.getDescription(),
                request.getScheduledAt(),
                request.getMaxParticipants(),
                request.getSessionDurationMinutes(),
                request.getStartEpisode(),
                request.getEndEpisode()
        );

        return toResponse(watchParty);
    }


    private void validateMaxParticipantsLimit(Integer maxParticipants) {
        if (maxParticipants > MAX_PARTICIPANTS_LIMIT) {
            throw new WatchPartyMaxParticipantsExceededException(maxParticipants, MAX_PARTICIPANTS_LIMIT);
        }
    }

    private void validateMaxParticipants(UUID partyId, Integer maxParticipants) {
        long currentCount = watchPartyParticipantRepository
                .countByWatchParty_IdAndStatus(partyId, ParticipantStatus.JOINED);

        if (maxParticipants < currentCount) {
            throw new WatchPartyMaxParticipantsBelowCurrentException(partyId, currentCount, maxParticipants);
        }
    }

    public void deleteWatchParty(UUID requesterId, UUID partyId) {
        WatchParty watchParty = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new WatchPartyNotFoundException(partyId));

        if (!watchParty.getHost().getId().equals(requesterId)) {
            throw new WatchPartyHostOnlyException(partyId, requesterId);
        }

        watchParty.validateEditable();

        watchPartyRepository.delete(watchParty);
        watchPartyHostRegistry.removeHost(partyId);
    }

    private void validateEpisodeRange(Content content, Integer startEpisode, Integer endEpisode) {
        boolean hasEpisodeRange = startEpisode != null || endEpisode != null;
        boolean isEpisodicContent = content.getType() == ContentType.TV_SEASON;

        if (hasEpisodeRange && !isEpisodicContent) {
            throw new WatchPartyInvalidEpisodeRangeException(content.getId());
        }

        if (hasEpisodeRange && isEpisodicContent) {
            Integer episodeCount = content.getEpisodeCount();
            if (endEpisode != null && episodeCount != null && endEpisode > episodeCount) {
                throw new WatchPartyInvalidEpisodeRangeException(content.getId(), episodeCount, endEpisode);
            }
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
            WatchPartyStatus statusEqual, UUID contentIdEqual, WatchPartySearch.Sort sort,
            String cursor, UUID idAfter, int limit, SortDirection sortDirection) {

        if (contentIdEqual != null && statusEqual != null) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }

        boolean popularSort = sort == WatchPartySearch.Sort.PARTICIPANT_COUNT;
        boolean contentSearch = contentIdEqual != null;
        // 콘텐츠 전용 커서(상태|시각)는 "콘텐츠 검색 + 기본 정렬"일 때만. 인기순이면 인기순 커서를 쓴다
        boolean contentCursorMode = contentSearch && !popularSort;
        boolean ascending = sortDirection == SortDirection.ASCENDING;

        ContentCursor contentCursor = contentCursorMode ? parseContentCursor(cursor, idAfter) : null;
        Long cursorParticipantCount = popularSort ? parseParticipantCountCursor(cursor, idAfter) : null;
        Instant cursorScheduledAt = null;
        if (contentCursorMode) {
            cursorScheduledAt = contentCursor.scheduledAt();
        } else if (!popularSort && cursor != null) {
            cursorScheduledAt = Instant.parse(cursor);
        }

        if (contentSearch) {
            validateContentForWatchParty(contentIdEqual);
        }

        WatchPartySearch search = WatchPartySearch.builder()
                .statusEqual(statusEqual)
                .contentIdEqual(contentIdEqual)
                .sort(sort)
                .cursorScheduledAt(cursorScheduledAt)
                .cursorId(idAfter)
                .cursorStatus(contentCursorMode ? contentCursor.status() : null)
                .cursorParticipantCount(cursorParticipantCount)
                .contentScheduledAtFrom(contentSearch ? Instant.now().minus(1, ChronoUnit.HOURS) : null)
                .ascending(ascending)
                .limit(limit)
                .build();

        WatchPartyQueryRepository.SearchResult result = watchPartyQueryRepository.search(search);

        List<WatchParty> watchParties = result.getWatchParties();
        Map<UUID, Content> contentById = fetchContentMap(watchParties);
        // 인기순은 ① 순위표에서 이미 센 값을 재사용 → 참가자 수 쿼리 생략 (요청당 쿼리 4개 유지)
        Map<UUID, Integer> participantCountById = popularSort
                ? toIntCountMap(result.getParticipantCounts())
                : fetchParticipantCountMap(watchParties);

        List<WatchPartySummaryResponse> data = watchParties.stream()
                .map(wp -> toSummaryResponse(wp, contentById, participantCountById))
                .toList();

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (!watchParties.isEmpty()) {
            WatchParty last = watchParties.get(watchParties.size() - 1);
            if (popularSort) {
                nextCursor = String.valueOf(result.getParticipantCounts().get(last.getId()));
            } else if (contentSearch) {
                nextCursor = formatContentCursor(last);
            } else {
                nextCursor = last.getScheduledAt().toString();
            }
            nextIdAfter = last.getId();
        }

        return CursorPageResponse.<WatchPartySummaryResponse>builder()
                .data(data)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(result.isHasNext())
                .totalCount(result.getTotalCount())
                .sortBy(popularSort ? "participantCount" : "scheduledAt")
                .sortDirection(contentCursorMode ? SortDirection.ASCENDING : sortDirection)
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

    // 인기순 커서: nextCursor에 담아 보낸 "참가자 수" 문자열을 다시 숫자로
    private Long parseParticipantCountCursor(String cursor, UUID idAfter) {
        if (cursor == null && idAfter == null) {
            return null;   // 첫 페이지
        }
        if (cursor == null || idAfter == null) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
        try {
            long count = Long.parseLong(cursor);
            if (count < 0) {
                throw new IllegalArgumentException("Negative participant count cursor");
            }
            return count;
        } catch (IllegalArgumentException e) {   // NumberFormatException도 여기로 (하위 클래스)
            throw new BaseException(ErrorCode.INVALID_REQUEST, e);
        }
    }

    // 리포지토리는 count를 Long으로, 응답 DTO는 int로 쓰므로 변환
    private Map<UUID, Integer> toIntCountMap(Map<UUID, Long> counts) {
        return counts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));
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

    @Transactional(readOnly = true)
    public List<WatchPartySummaryResponse> getScheduledWatchParties(UUID userId) {
        List<WatchParty> watchParties = watchPartyReminderRepository.findScheduledByUserId(
                userId,
                WatchPartyStatus.SCHEDULED,
                Instant.now()
            ).stream()
            .map(reminder -> reminder.getWatchParty())
            .toList();

        Map<UUID, Content> contentById = fetchContentMap(watchParties);
        Map<UUID, Integer> participantCountById = fetchParticipantCountMap(watchParties);

        return watchParties.stream()
            .map(wp -> toSummaryResponse(wp, contentById, participantCountById))
            .toList();
    }

    private WatchPartySummaryResponse toSummaryResponse(
        WatchParty watchParty,
        Map<UUID, Content> contentById,
        Map<UUID, Integer> participantCountById) {

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
        if (watchParties.isEmpty()) {
            return Map.of();
        }

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
