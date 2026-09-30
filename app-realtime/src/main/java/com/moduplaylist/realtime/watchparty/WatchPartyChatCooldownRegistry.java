package com.moduplaylist.realtime.watchparty;

import java.time.Duration;
import java.util.UUID;

/**
 * 채팅 도배 방지: 사용자별 전송 간격 제한.
 */
public interface WatchPartyChatCooldownRegistry {

    // 쿨다운이 비어 있으면 자리를 잡고 true, 아직 쿨다운 중이면 false
    boolean tryAcquire(UUID partyId, UUID userId, Duration cooldown);
}