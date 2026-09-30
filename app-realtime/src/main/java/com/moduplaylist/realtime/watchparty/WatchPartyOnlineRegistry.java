package com.moduplaylist.realtime.watchparty;

import java.util.UUID;


// 온라인 판단(유령 JOINED 정리)은 lastSeen(하트비트) 기준.
// 이 online 카운트는 현재 읽는 곳이 없음 — 정리 여부는 추후 논의.
public interface WatchPartyOnlineRegistry {

    void addOnline(UUID partyId, UUID userId);

    void removeOnline(UUID partyId, UUID userId);
}