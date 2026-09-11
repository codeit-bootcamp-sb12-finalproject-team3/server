package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.exception.*;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantRepository;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchPartyParticipantService {

    private final WatchPartyRepository watchPartyRepository;
    private final UserRepository userRepository;
    private final WatchPartyParticipantRepository watchPartyParticipantRepository;

    public void joinWatchParty(UUID partyId, UUID userId) {

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
            return;
        }

        validateCapacity(party);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        watchPartyParticipantRepository.save(new WatchPartyParticipant(user, party));
    }


    private void validateCapacity(WatchParty party) {
        long currentCount = watchPartyParticipantRepository
                .countByWatchParty_IdAndStatus(party.getId(), ParticipantStatus.JOINED);

        if (currentCount >= party.getMaxParticipants()) {
            throw new WatchPartyCapacityFullException(party.getId());
        }
    }

    public void leaveWatchParty(UUID partyId, UUID userId) {
        WatchPartyParticipant participant = watchPartyParticipantRepository
                .findByUser_IdAndWatchParty_Id(userId, partyId)
                .orElseThrow(() -> new WatchPartyParticipantNotFoundException(partyId, userId));

        if (participant.getStatus() != ParticipantStatus.JOINED) {
            throw new WatchPartyNotJoinedException(partyId, userId);
        }

        participant.leave();
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
            throw new WatchPartyNotJoinedException(partyId, targetUserId);
        }

        participant.kick();
    }
}