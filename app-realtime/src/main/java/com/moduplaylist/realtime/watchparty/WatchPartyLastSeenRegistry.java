package com.moduplaylist.realtime.watchparty;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 하트비트: 파티별 사용자 "마지막 확인 시각" 기록
// app-api가 이 값으로 유령 JOINED를 판단한다.
public interface WatchPartyLastSeenRegistry {

    void touch(UUID partyId, UUID userId, long seenAtMillis);

    void touchAll(Map<UUID, Set<UUID>> userIdsByPartyId, long seenAtMillis);
}