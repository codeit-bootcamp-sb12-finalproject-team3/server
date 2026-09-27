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

    public void joinWatchParty(UUID partyId, UUID userId) {

        if (watchPartyKickedRegistry.isKicked(partyId, userId)) {
            throw new WatchPartyKickedCannotRejoinException(partyId, userId);
        }

        // 다른 방에 JOINED가 남아 있으면: 유령이면 정리하고 진행, 실제로 보고 있으면 차단
        Optional<WatchPartyParticipant> joinedElsewhere = watchPartyParticipantRepository
                .findFirstByUser_IdAndStatusAndWatchParty_IdNotAndWatchParty_StatusNot(
                        userId, ParticipantStatus.JOINED, partyId, WatchPartyStatus.ENDED);
        if (joinedElsewhere.isPresent()
                && !watchPartyGhostCleaner.cleanUpIfGhost(joinedElsewhere.get())) {
            throw new WatchPartyAlreadyJoinedElsewhereException(
                    userId, partyId, joinedElsewhere.get().getWatchParty().getId());
        }

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
            WatchPartyParticipant participant = existing.get();

            if (participant.getStatus() == ParticipantStatus.JOINED) {
                throw new WatchPartyAlreadyJoinedException(partyId, userId);
            }
            if (participant.getStatus() == ParticipantStatus.KICKED) {
                throw new WatchPartyKickedCannotRejoinException(partyId, userId);
            }

            validateCapacity(party);
            participant.rejoin();
            eventPublisher.publishEvent(
                    new WatchPartyParticipantChangedEvent(
                            UUID.randomUUID(), partyId, userId, ParticipantStatus.JOINED, true));
            watchPartyJoinedRegistry.join(partyId, userId);
            watchPartyActivePartyRegistry.setJoinedParty(userId, partyId);
            return;
        }

        validateCapacity(party);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        watchPartyParticipantRepository.save(new WatchPartyParticipant(user, party));
        eventPublisher.publishEvent(
                new WatchPartyParticipantChangedEvent(
                        UUID.randomUUID(), partyId, userId, ParticipantStatus.JOINED, false));
        watchPartyJoinedRegistry.join(partyId, userId);
        watchPartyActivePartyRegistry.setJoinedParty(userId, partyId);
    }


    private void validateCapacity(WatchParty party) {
        long currentCount = countJoined(party.getId());

        // 가득 찼으면 유령만 정리하고 다시 센다 (#167). 방 락(findByIdForUpdate) 안에서 실행됨
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