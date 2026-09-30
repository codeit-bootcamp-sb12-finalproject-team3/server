package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.watchparty.dto.WatchPartyParticipantResponse;
import com.moduplaylist.api.watchparty.event.WatchPartyParticipantChangedEvent;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.exception.*;
import com.moduplaylist.core.watchparty.repository.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

        // Serialize JOIN requests by user, even when their destination parties differ.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // The party lock keeps the capacity check and join atomic for this destination.
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
        if (existing.isPresent() && existing.get().getStatus() == ParticipantStatus.KICKED) {
            throw new WatchPartyKickedCannotRejoinException(partyId, userId);
        }

        boolean alreadyJoined = existing
                .map(participant -> participant.getStatus() == ParticipantStatus.JOINED)
                .orElse(false);
        if (!alreadyJoined) {
            validateCapacity(party);
        }

        // B is now known to be joinable. Only after that may A be changed.
        WatchPartyParticipant switchSource = findSwitchSource(partyId, userId);
        UUID switchedFromPartyId = null;
        if (switchSource != null && leaveForSwitch(switchSource)) {
            switchedFromPartyId = switchSource.getWatchParty().getId();
        }

        if (alreadyJoined) {
            synchronizeJoinRegistriesAfterCommit(switchedFromPartyId, partyId, userId);
            return;
        }

        boolean isRejoin = existing.isPresent();
        if (isRejoin) {
            existing.get().rejoin();
        } else {
            watchPartyParticipantRepository.save(new WatchPartyParticipant(user, party));
        }
        eventPublisher.publishEvent(
                new WatchPartyParticipantChangedEvent(
                        UUID.randomUUID(), partyId, userId, ParticipantStatus.JOINED, isRejoin));
        synchronizeJoinRegistriesAfterCommit(switchedFromPartyId, partyId, userId);
    }

    private WatchPartyParticipant findSwitchSource(UUID partyId, UUID userId) {
        return watchPartyParticipantRepository
                .findFirstByUser_IdAndStatusAndWatchParty_IdNot(
                        userId, ParticipantStatus.JOINED, partyId)
                .orElse(null);
    }

    // A conditional update avoids duplicate LEFT events if cleanup won the race.
    private boolean leaveForSwitch(WatchPartyParticipant participant) {
        UUID fromPartyId = participant.getWatchParty().getId();
        UUID userId = participant.getUser().getId();

        if (watchPartyParticipantRepository.markLeftIfJoined(participant.getId(), Instant.now()) != 1) {
            return false;
        }
        eventPublisher.publishEvent(
                new WatchPartyParticipantChangedEvent(
                        UUID.randomUUID(), fromPartyId, userId, ParticipantStatus.LEFT, false));
        return true;
    }

    // Redis is derived state. Update it only after the DB transaction has committed.
    private void synchronizeJoinRegistriesAfterCommit(UUID fromPartyId, UUID partyId, UUID userId) {
        Runnable synchronization = () -> {
            if (fromPartyId != null) {
                watchPartyJoinedRegistry.leave(fromPartyId, userId);
            }
            watchPartyJoinedRegistry.join(partyId, userId);
            watchPartyActivePartyRegistry.setJoinedParty(userId, partyId);
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            synchronization.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                synchronization.run();
            }
        });
    }

    private void validateCapacity(WatchParty party) {
        long currentCount = countJoined(party.getId());
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
