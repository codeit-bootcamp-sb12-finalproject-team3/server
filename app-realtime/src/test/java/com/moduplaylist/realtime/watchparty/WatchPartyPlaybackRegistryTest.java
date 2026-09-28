package com.moduplaylist.realtime.watchparty;

import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackState;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyPlaybackStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WatchPartyPlaybackRegistryTest {

    private final UUID partyId = UUID.randomUUID();

    @Test
    void ENDED면_종료로_본다() {
        assertThat(registryReturning(state(WatchPartyPlaybackStatus.ENDED)).isEnded(partyId)).isTrue();
    }

    @Test
    void LIVE면_종료가_아니다() {
        assertThat(registryReturning(state(WatchPartyPlaybackStatus.LIVE)).isEnded(partyId)).isFalse();
    }

    // SCHEDULED는 playback 키가 아직 없음 → 구독 허용되어야 함
    @Test
    void 키가_없으면_종료가_아니다() {
        assertThat(registryReturning(Optional.empty()).isEnded(partyId)).isFalse();
    }

    // ---- 테스트용 도우미 ----

    // mock은 default 메서드도 가짜로 만들어 버리므로, find()만 정해 둔 진짜 구현체로 테스트
    private WatchPartyPlaybackRegistry registryReturning(Optional<WatchPartyPlaybackState> found) {
        return new WatchPartyPlaybackRegistry() {
            @Override
            public Optional<WatchPartyPlaybackState> find(UUID partyId) {
                return found;
            }

            @Override
            public void update(UUID partyId, WatchPartyPlaybackState state) {
            }
        };
    }

    private Optional<WatchPartyPlaybackState> state(WatchPartyPlaybackStatus status) {
        return Optional.of(new WatchPartyPlaybackState(
                status, 1000L, 0L, null, null, null, UUID.randomUUID(), 2000L));
    }
}