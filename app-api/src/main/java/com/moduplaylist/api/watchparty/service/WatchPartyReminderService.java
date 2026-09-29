package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyReminder;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.exception.WatchPartyHostCannotSetReminderException;
import com.moduplaylist.core.watchparty.exception.WatchPartyInvalidStateException;
import com.moduplaylist.core.watchparty.exception.WatchPartyKickedCannotRejoinException;
import com.moduplaylist.core.watchparty.exception.WatchPartyNotFoundException;
import com.moduplaylist.core.watchparty.exception.WatchPartyReminderAlreadyExistsException;
import com.moduplaylist.core.watchparty.exception.WatchPartyReminderNotFoundException;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantRepository;
import com.moduplaylist.core.watchparty.repository.WatchPartyReminderRepository;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchPartyReminderService {

    private final WatchPartyRepository watchPartyRepository;
    private final WatchPartyReminderRepository watchPartyReminderRepository;
    private final WatchPartyParticipantRepository watchPartyParticipantRepository;
    private final UserRepository userRepository;

    public void setReminder(UUID partyId, UUID userId) {
        WatchParty party = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new WatchPartyNotFoundException(partyId));

        if (party.getStatus() != WatchPartyStatus.SCHEDULED) {
            throw new WatchPartyInvalidStateException(partyId, "시작 대기 상태의 방만 알림을 설정할 수 있습니다.");
        }

        // 시작 예정 시각이 이미 지난 방(방장이 아직 시작 안 함)은 알림 대상 아님
        if (!party.getScheduledAt().isAfter(Instant.now())) {
            throw new WatchPartyInvalidStateException(partyId, "시작 예정 시각이 지난 방은 알림을 설정할 수 없습니다.");
        }

        if (party.getHost().getId().equals(userId)) {
            throw new WatchPartyHostCannotSetReminderException(partyId, userId);
        }

        boolean kicked = watchPartyParticipantRepository.findByUser_IdAndWatchParty_Id(userId, partyId)
                .filter(participant -> participant.getStatus() == ParticipantStatus.KICKED)
                .isPresent();
        if (kicked) {
            throw new WatchPartyKickedCannotRejoinException(partyId, userId);
        }

        if (watchPartyReminderRepository.existsByWatchParty_IdAndUser_Id(partyId, userId)) {
            throw new WatchPartyReminderAlreadyExistsException(partyId, userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        try {
            watchPartyReminderRepository.saveAndFlush(new WatchPartyReminder(party, user));
        } catch (DataIntegrityViolationException e) {
            throw new WatchPartyReminderAlreadyExistsException(partyId, userId, e);
        }
    }

    public void cancelReminder(UUID partyId, UUID userId) {
        if (!watchPartyRepository.existsById(partyId)) {
            throw new WatchPartyNotFoundException(partyId);
        }

        int deleted = watchPartyReminderRepository.deleteByWatchPartyIdAndUserId(partyId, userId);
        if (deleted == 0) {
            throw new WatchPartyReminderNotFoundException(partyId, userId);
        }
    }
}