package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.watchparty.event.WatchPartyEndedEvent;
import com.moduplaylist.api.watchparty.event.WatchPartyStartedEvent;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyPlaybackStatus;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.exception.WatchPartyHostOnlyException;
import com.moduplaylist.core.watchparty.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.moduplaylist.core.watchparty.exception.WatchPartyNotFoundException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchPartyStatusService {

    private final WatchPartyRepository watchPartyRepository;
    private final ApplicationEventPublisher eventPublisher;


    public void startWatchParty(UUID partyId, UUID hostId) {
        // 스케줄러 자동 시작과 동시에 실행돼도 한 번만 시작되도록 락을 잡고 읽는다
        WatchParty party = watchPartyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new WatchPartyNotFoundException(partyId));

        if (!party.getHost().getId().equals(hostId)) {
            throw new WatchPartyHostOnlyException(partyId, hostId);
        }

        startInternal(party);
    }

    // 스케줄러 전용: 방장 검증 없이, 아직 SCHEDULED일 때만 시작 (이미 시작됐으면 조용히 건너뜀)
    public void autoStartIfScheduled(UUID partyId) {
        watchPartyRepository.findByIdForUpdate(partyId)
                .filter(party -> party.getStatus() == WatchPartyStatus.SCHEDULED)
                .ifPresent(this::startInternal);
    }

    private void startInternal(WatchParty party) {
        party.start();

        long now = System.currentTimeMillis();
        WatchPartyPlaybackState state = new WatchPartyPlaybackState(
                WatchPartyPlaybackStatus.LIVE,
                now,
                0L,
                null,              // pausedAt — 시작 시점엔 일시정지 아니므로 null
                party.getStartEpisode(),
                party.getEndEpisode(),
                party.getHost().getId(),
                now
        );
        eventPublisher.publishEvent(new WatchPartyStartedEvent(UUID.randomUUID(), party.getId(), state));
    }


    public void endWatchParty(UUID partyId, UUID hostId) {
        WatchParty party = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new WatchPartyNotFoundException(partyId));

        if (!party.getHost().getId().equals(hostId)) {
            throw new WatchPartyHostOnlyException(partyId, hostId);
        }

        party.end();
        eventPublisher.publishEvent(new WatchPartyEndedEvent(UUID.randomUUID(), partyId));

    }
}