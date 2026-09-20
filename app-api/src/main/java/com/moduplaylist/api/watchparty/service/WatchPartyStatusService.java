package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.watchparty.event.WatchPartyEndedEvent;
import com.moduplaylist.api.watchparty.event.WatchPartyStartedEvent;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyPlaybackStatus;
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
        WatchParty party = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new WatchPartyNotFoundException(partyId));

        if (!party.getHost().getId().equals(hostId)) {
            throw new WatchPartyHostOnlyException(partyId, hostId);
        }

        party.start();

        long now = System.currentTimeMillis();
        WatchPartyPlaybackState state = new WatchPartyPlaybackState(
                WatchPartyPlaybackStatus.LIVE,
                now,
                0L,
                party.getStartEpisode(),
                party.getEndEpisode(),
                hostId,
                now
        );
        eventPublisher.publishEvent(new WatchPartyStartedEvent(partyId, state));
    }


    public void endWatchParty(UUID partyId, UUID hostId) {
        WatchParty party = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new WatchPartyNotFoundException(partyId));

        if (!party.getHost().getId().equals(hostId)) {
            throw new WatchPartyHostOnlyException(partyId, hostId);
        }

        party.end();
        eventPublisher.publishEvent(new WatchPartyEndedEvent(partyId));

    }
}