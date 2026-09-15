package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.exception.WatchPartyHostOnlyException;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.moduplaylist.core.watchparty.exception.WatchPartyNotFoundException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchPartyStatusService {

    private final WatchPartyRepository watchPartyRepository;

    public void startWatchParty(UUID partyId, UUID hostId) {
        WatchParty party = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new WatchPartyNotFoundException(partyId));

        if (!party.getHost().getId().equals(hostId)) {
            throw new WatchPartyHostOnlyException(partyId, hostId);
        }

        party.start();
    }

    public void endWatchParty(UUID partyId, UUID hostId) {
        WatchParty party = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new WatchPartyNotFoundException(partyId));

        if (!party.getHost().getId().equals(hostId)) {
            throw new WatchPartyHostOnlyException(partyId, hostId);
        }

        party.end();
    }
}