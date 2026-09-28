package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.watchparty.event.WatchPartyParticipantChangedEvent;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import com.moduplaylist.core.watchparty.repository.WatchPartyActivePartyRegistry;
import com.moduplaylist.core.watchparty.repository.WatchPartyJoinedRegistry;
import com.moduplaylist.core.watchparty.repository.WatchPartyLastSeenRegistry;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유령 JOINED 정리. 세 경로가 같은 기준·같은 절차를 쓰도록 한 곳에 모은다.
 * - 입장 시 다른 방 확인: cleanUpIfGhost
 * - 정원 확인 / 스케줄러: cleanUpGhostsInParty
 * 원칙: 확실하지 않으면 온라인으로 본다 (조회 실패 = 정리하지 않음).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class WatchPartyGhostCleaner {

    // 유령 기준 N. realtime 하트비트 주기(1분)보다 충분히 길어야 한다.
    static final Duration GHOST_THRESHOLD = Duration.ofMinutes(5);

    private final WatchPartyParticipantRepository watchPartyParticipantRepository;
    private final WatchPartyLastSeenRegistry watchPartyLastSeenRegistry;
    private final WatchPartyJoinedRegistry watchPartyJoinedRegistry;
    private final WatchPartyActivePartyRegistry watchPartyActivePartyRegistry;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 한 참가 기록이 유령이면 정리한다.
     * @return 유령이었으면 true (다른 경로가 먼저 정리했어도 true), 온라인이거나 판단 불가면 false
     */
    public boolean cleanUpIfGhost(WatchPartyParticipant participant) {
        UUID partyId = participant.getWatchParty().getId();
        UUID userId = participant.getUser().getId();

        Optional<Instant> lastSeen;
        try {
            lastSeen = watchPartyLastSeenRegistry.findLastSeen(partyId, userId);
        } catch (RuntimeException e) {
            log.warn("lastSeen 조회 실패 - 온라인으로 간주. partyId={}, userId={}", partyId, userId, e);
            return false;
        }

        Instant now = Instant.now();
        if (!isGhost(participant.getJoinedAt(), lastSeen.orElse(null), now)) {
            return false;
        }
        markLeft(participant.getId(), partyId, userId, now);
        return true;
    }

    /**
     * 방 안의 JOINED 중 유령만 정리한다.
     * @return 이번 호출에서 실제로 LEFT 처리한 인원 수
     */
    public int cleanUpGhostsInParty(UUID partyId) {
        List<WatchPartyParticipant> joined = watchPartyParticipantRepository
                .findJoinedParticipants(partyId, ParticipantStatus.JOINED);
        if (joined.isEmpty()) {
            return 0;
        }

        Map<UUID, Instant> lastSeenByUserId;
        try {
            lastSeenByUserId = watchPartyLastSeenRegistry.findAllLastSeen(partyId);
        } catch (RuntimeException e) {
            log.warn("lastSeen 조회 실패 - 전원 온라인으로 간주. partyId={}", partyId, e);
            return 0;
        }

        Instant now = Instant.now();
        int cleaned = 0;
        for (WatchPartyParticipant participant : joined) {
            UUID userId = participant.getUser().getId();
            if (isGhost(participant.getJoinedAt(), lastSeenByUserId.get(userId), now)
                    && markLeft(participant.getId(), partyId, userId, now)) {
                cleaned++;
            }
        }
        return cleaned;
    }

    // 유령 = now - max(joinedAt, lastSeen) > N
    static boolean isGhost(Instant joinedAt, Instant lastSeen, Instant now) {
        Instant lastConfirmed = (lastSeen != null && lastSeen.isAfter(joinedAt)) ? lastSeen : joinedAt;
        return Duration.between(lastConfirmed, now).compareTo(GHOST_THRESHOLD) > 0;
    }

    // 기존 leave()와 같은 연쇄. 조건부 UPDATE가 1행일 때만 진행 (동시 정리 시 LEFT 방송 중복 방지)
    private boolean markLeft(UUID participantId, UUID partyId, UUID userId, Instant now) {
        if (watchPartyParticipantRepository.markLeftIfJoined(participantId, now) != 1) {
            return false; // 다른 경로가 먼저 정리함
        }
        eventPublisher.publishEvent(
                new WatchPartyParticipantChangedEvent(
                        UUID.randomUUID(), partyId, userId, ParticipantStatus.LEFT, false));
        watchPartyJoinedRegistry.leave(partyId, userId);
        watchPartyActivePartyRegistry.clearJoinedParty(userId);
        log.info("유령 JOINED 정리 - partyId={}, userId={}", partyId, userId);
        return true;
    }
}