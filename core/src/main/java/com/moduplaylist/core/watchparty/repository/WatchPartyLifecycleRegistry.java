package com.moduplaylist.core.watchparty.repository;

import java.util.UUID;

public interface WatchPartyLifecycleRegistry {

    // 파티 종료 시 안전장치 TTL 예약 (명시적 삭제가 실패했을 때의 방어선)
    void armSafetyNetTtl(UUID partyId);

    // 파티 관련 Redis 키 즉시 일괄 삭제 (방장 즉시삭제 API 또는 배치가 호출)
    void deletePartyKeysNow(UUID partyId);
}