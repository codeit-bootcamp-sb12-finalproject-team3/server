package com.moduplaylist.realtime.watchparty;

import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackState;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackStatus;

import java.util.Optional;
import java.util.UUID;

public interface WatchPartyPlaybackRegistry {

    Optional<WatchPartyPlaybackState> find(UUID partyId);

    void update(UUID partyId, WatchPartyPlaybackState state);

    // 키 없음 = 아직 시작 전(SCHEDULED) → 종료 아님
    default boolean isEnded(UUID partyId) {
        return find(partyId)
                .map(state -> state.getStatus() == WatchPartyPlaybackStatus.ENDED)
                .orElse(false);
    }
}
