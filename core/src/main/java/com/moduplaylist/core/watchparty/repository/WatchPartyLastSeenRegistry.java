package com.moduplaylist.core.watchparty.repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// 하트비트: realtime이 기록한 "마지막 확인 시각"을 읽는다. 쓰기는 realtime 담당.
public interface WatchPartyLastSeenRegistry {

    // 다른 방 참가 확인용 (한 사용자)
    Optional<Instant> findLastSeen(UUID partyId, UUID userId);

    // 정원 정리·스케줄러용 (파티 전체)
    Map<UUID, Instant> findAllLastSeen(UUID partyId);
}