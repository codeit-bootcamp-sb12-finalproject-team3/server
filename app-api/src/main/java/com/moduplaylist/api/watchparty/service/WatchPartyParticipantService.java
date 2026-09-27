package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.recommendation.service.ContentPreferenceUpdateService;
import com.moduplaylist.api.watchparty.dto.WatchPartyParticipantResponse;
import com.moduplaylist.api.watchparty.event.WatchPartyParticipantChangedEvent;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.exception.*;
import com.moduplaylist.core.watchparty.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchPartyParticipantService {

    private final WatchPartyRepository watchPartyRepository;
    private final UserRepository userRepository;
    private final WatchPartyParticipantRepository watchPartyParticipantRepository;
    private final WatchPartyKickedRegistry watchPartyKickedRegistry;
    private final WatchPartyJoinedRegistry watchPartyJoinedRegistry;
    private final WatchPartyActivePartyRegistry watchPartyActivePartyRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final WatchPartyGhostCleaner watchPartyGhostCleaner;

    public void joinWatchParty(UUID partyId, UUID userId, UUID switchFrom) {

        if (watchPartyKickedRegistry.isKicked(partyId, userId)) {
            throw new WatchPartyKickedCannotRejoinException(partyId, userId);
        }

        // 1) 다른 방 JOINED 확인: 유령이면 정리, 전환 동의(switchFrom 일치)면 나갈 대상으로 기억만, 아니면 차단
        WatchPartyParticipant switchSource = findSwitchSource(partyId, userId, switchFrom);

        // 2) B 잠금 + 실패할 수 있는 검사 전부 (A는 아직 건드리지 않음)
        WatchParty party = watchPartyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new WatchPartyNotFoundException(partyId));

        if (party.getStatus() == WatchPartyStatus.ENDED) {
            throw new WatchPartyAlreadyEndedException(partyId);
        }

        if (party.getHost().getId().equals(userId)) {
            throw new WatchPartyHostCannotJoinException(partyId, userId);
        }

        Optional<WatchPartyParticipant> existing =
                watchPartyParticipantRepository.findByUser_IdAndWatchParty_Id(userId, partyId);

        if (existing.isPresent()) {
            if (existing.get().getStatus() == ParticipantStatus.JOINED) {
                throw new WatchPartyAlreadyJoinedException(partyId, userId);
            }
            if (existing.get().getStatus() == ParticipantStatus.KICKED) {
                throw new WatchPartyKickedCannotRejoinException(partyId, userId);
            }
        }

        validateCapacity(party);

        User user = existing.isPresent() ? null
                : userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        // 3) 검사를 모두 통과한 뒤에만 A 나가기. A의 activeParty 삭제가 B 설정보다 먼저여야 함
        if (switchSource != null) {
            leaveForSwitch(switchSource);
        }

        // 4) B 참가
        boolean isRejoin = existing.isPresent();
        if (isRejoin) {
            existing.get().rejoin();
        } else {
            watchPartyParticipantRepository.save(new WatchPartyParticipant(user, party));
        }
        eventPublisher.publishEvent(
                new WatchPartyParticipantChangedEvent(
                        UUID.randomUUID(), partyId, userId, ParticipantStatus.JOINED, isRejoin));
        watchPartyJoinedRegistry.join(partyId, userId);
        watchPartyActivePartyRegistry.setJoinedParty(userId, partyId);
    }

    // 전환으로 나갈 참가 기록. null = 참가 중인 다른 방 없음(또는 유령이라 방금 정리됨) → 그냥 B 참가
    private WatchPartyParticipant findSwitchSource(UUID partyId, UUID userId, UUID switchFrom) {
        Optional<WatchPartyParticipant> joinedElsewhere = watchPartyParticipantRepository
                .findFirstByUser_IdAndStatusAndWatchParty_IdNotAndWatchParty_StatusNot(
                        userId, ParticipantStatus.JOINED, partyId, WatchPartyStatus.ENDED);

        if (joinedElsewhere.isEmpty() || watchPartyGhostCleaner.cleanUpIfGhost(joinedElsewhere.get())) {
            return null;
        }

        UUID joinedPartyId = joinedElsewhere.get().getWatchParty().getId();
        if (!joinedPartyId.equals(switchFrom)) {
            // 옵션 없음, 또는 동의한 방(A)과 지금 참가 중인 방(C)이 다름 → C 기준으로 다시 묻기
            throw new WatchPartyAlreadyJoinedElsewhereException(userId, partyId, joinedPartyId);
        }
        return joinedElsewhere.get();
    }

    // 전환 동의로 A 나가기. A는 잠그지 않고 조건부 UPDATE로 동시성 처리
    private void leaveForSwitch(WatchPartyParticipant participant) {
        UUID fromPartyId = participant.getWatchParty().getId();
        UUID userId = participant.getUser().getId();

        if (watchPartyParticipantRepository.markLeftIfJoined(participant.getId(), Instant.now()) != 1) {
            return; // 스케줄러·정원 정리가 먼저 LEFT 처리함 → 이미 나간 것으로 보고 B 참가 진행
        }
        eventPublisher.publishEvent(
                new WatchPartyParticipantChangedEvent(
                        UUID.randomUUID(), fromPartyId, userId, ParticipantStatus.LEFT, false));
        watchPartyJoinedRegistry.leave(fromPartyId, userId);
        watchPartyActivePartyRegistry.clearJoinedParty(userId);
    }

    private void validateCapacity(WatchParty party) {
        long currentCount = countJoined(party.getId());

        // 정원이 찼으면 유령(5분 넘게 연결 확인이 안 된 JOINED)만 LEFT로 정리하고 인원을 다시 센다.
        // 호출 전에 findByIdForUpdate로 이 파티 행을 잠갔으므로, 같은 방 참가 요청은 하나씩 처리되어 인원 세기가 겹치지 않는다
        if (currentCount >= party.getMaxParticipants()
                && watchPartyGhostCleaner.cleanUpGhostsInParty(party.getId()) > 0) {
            currentCount = countJoined(party.getId());
        }

        if (currentCount >= party.getMaxParticipants()) {
            throw new WatchPartyCapacityFullException(party.getId());
        }
    }

    private long countJoined(UUID partyId) {
        return watchPartyParticipantRepository.countByWatchParty_IdAndStatus(partyId, ParticipantStatus.JOINED);
    }

    public void leaveWatchParty(UUID partyId, UUID userId) {
        WatchPartyParticipant participant = watchPartyParticipantRepository
                .findByUser_IdAndWatchParty_Id(userId, partyId)
                .orElseThrow(() -> new WatchPartyNotAParticipantException(partyId, userId));

        if (participant.getStatus() != ParticipantStatus.JOINED) {
            throw new WatchPartyNotJoinedException(partyId, userId);
        }

        participant.leave();
        eventPublisher.publishEvent(
                new WatchPartyParticipantChangedEvent(
                        UUID.randomUUID(), partyId, userId, ParticipantStatus.LEFT, false));
        watchPartyJoinedRegistry.leave(partyId, userId);
        watchPartyActivePartyRegistry.clearJoinedParty(userId);
    }

    public void kickParticipant(UUID partyId, UUID hostId, UUID targetUserId) {
        WatchParty party = watchPartyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new WatchPartyNotFoundException(partyId));

        if (!party.getHost().getId().equals(hostId)) {
            throw new WatchPartyHostOnlyException(partyId, hostId);
        }

        WatchPartyParticipant participant = watchPartyParticipantRepository
                .findByUser_IdAndWatchParty_Id(targetUserId, partyId)
                .orElseThrow(() -> new WatchPartyParticipantNotFoundException(partyId, targetUserId));

        if (participant.getStatus() != ParticipantStatus.JOINED) {
            throw new WatchPartyParticipantNotJoinedException(partyId, targetUserId);
        }

        participant.kick();
        eventPublisher.publishEvent(
                new WatchPartyParticipantChangedEvent(
                        UUID.randomUUID(), partyId, targetUserId, ParticipantStatus.KICKED, false));
        watchPartyKickedRegistry.kick(partyId, targetUserId);
        watchPartyJoinedRegistry.leave(partyId, targetUserId);
        watchPartyActivePartyRegistry.clearJoinedParty(targetUserId);
    }

    @Transactional(readOnly = true)
    public List<WatchPartyParticipantResponse> getParticipants(UUID partyId) {
        if (!watchPartyRepository.existsById(partyId)) {
            throw new WatchPartyNotFoundException(partyId);
        }

        return watchPartyParticipantRepository
                .findJoinedParticipants(partyId, ParticipantStatus.JOINED)
                .stream()
                .map(WatchPartyParticipantResponse::from)
                .toList();
    }
}