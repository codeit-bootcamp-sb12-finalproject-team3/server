package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
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

        WatchParty party = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다: " + partyId));

        if (party.getStatus() == WatchPartyStatus.ENDED) {
            throw new IllegalStateException("이미 종료된 방에는 참가할 수 없습니다.");
        }

        if (party.getHost().getId().equals(userId)) {
            throw new IllegalStateException("방장은 참가 신청 대상이 아닙니다.");
        }

        Optional<WatchPartyParticipant> existing =
                watchPartyParticipantRepository.findByUser_IdAndWatchParty_Id(userId, partyId);

        if (existing.isPresent()) {
            WatchPartyParticipant participant = existing.get();

            if (participant.getStatus() == ParticipantStatus.JOINED) {
                throw new IllegalStateException("이미 참가 중입니다.");
            }
            if (participant.getStatus() == ParticipantStatus.KICKED) {
                throw new SecurityException("강퇴된 방에는 다시 참가할 수 없습니다.");
            }

            validateCapacity(party);
            participant.rejoin();
            return;
        }

        validateCapacity(party);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));

        watchPartyParticipantRepository.save(new WatchPartyParticipant(user, party));
    }

    private void validateCapacity(WatchParty party) {
        long currentCount = watchPartyParticipantRepository
                .countByWatchParty_IdAndStatus(party.getId(), ParticipantStatus.JOINED);

        if (currentCount >= party.getMaxParticipants()) {
            throw new IllegalStateException("정원이 가득 찼습니다.");
        }
    }

    public void leaveWatchParty(UUID partyId, UUID userId) {
        WatchPartyParticipant participant = watchPartyParticipantRepository
                .findByUser_IdAndWatchParty_Id(userId, partyId)
                .orElseThrow(() -> new IllegalStateException("참가 중인 방이 아닙니다."));

        if (participant.getStatus() != ParticipantStatus.JOINED) {
            throw new IllegalStateException("현재 참가 중이 아닙니다.");
        }

        participant.leave();
    }

    public void kickParticipant(UUID partyId, UUID hostId, UUID targetUserId) {
        WatchParty party = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다: " + partyId));

        if (!party.getHost().getId().equals(hostId)) {
            throw new SecurityException("방장만 참가자를 강퇴할 수 있습니다.");
        }

        WatchPartyParticipant participant = watchPartyParticipantRepository
                .findByUser_IdAndWatchParty_Id(targetUserId, partyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 참가자를 찾을 수 없습니다.")); //사용자 자체를 모름

        if (participant.getStatus() != ParticipantStatus.JOINED) {
            throw new IllegalStateException("현재 참가 중인 사용자가 아닙니다."); //사용자가 어떠한 사유로 인해 방에 없음(강퇴 등)
        }

        participant.kick();
    }
}