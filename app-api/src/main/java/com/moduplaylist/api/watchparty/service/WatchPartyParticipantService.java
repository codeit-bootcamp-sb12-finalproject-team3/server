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
                throw new IllegalStateException("강퇴된 방에는 다시 참가할 수 없습니다.");
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
}