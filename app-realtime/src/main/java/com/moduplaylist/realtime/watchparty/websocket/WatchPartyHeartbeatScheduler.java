package com.moduplaylist.realtime.watchparty.websocket;

import com.moduplaylist.realtime.watchparty.WatchPartyLastSeenRegistry;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WatchPartyHeartbeatScheduler {

    private static final Logger log = LoggerFactory.getLogger(WatchPartyHeartbeatScheduler.class);

    private final SimpUserRegistry simpUserRegistry;
    private final WatchPartyLastSeenRegistry lastSeenRegistry;

    public WatchPartyHeartbeatScheduler(
            SimpUserRegistry simpUserRegistry,
            WatchPartyLastSeenRegistry lastSeenRegistry
    ) {
        this.simpUserRegistry = simpUserRegistry;
        this.lastSeenRegistry = lastSeenRegistry;
    }

    // 1분마다 "이 인스턴스에 연결된" 파티 구독자의 마지막 확인 시각 갱신.
    // 각 인스턴스가 자기 연결만 갱신하므로 분산 락이 필요 없다.
    // 이 주기를 늘리면 app-api 유령 기준(5분)도 같이 검토할 것.
    @Scheduled(fixedRate = 60_000)
    public void heartbeat() {
        try {
            Map<UUID, Set<UUID>> userIdsByPartyId = collectPartySubscribers();
            if (userIdsByPartyId.isEmpty()) {
                return;
            }
            lastSeenRegistry.touchAll(userIdsByPartyId, Instant.now().toEpochMilli());
        } catch (Exception e) {
            log.error("Watch Party 하트비트 갱신 실패", e);
        }
    }

    private Map<UUID, Set<UUID>> collectPartySubscribers() {
        Map<UUID, Set<UUID>> result = new HashMap<>();
        for (SimpUser user : simpUserRegistry.getUsers()) {
            UUID userId = WatchPartyDestinations.parseUuid(user.getName());
            if (userId == null) {
                continue;
            }
            for (SimpSession session : user.getSessions()) {
                for (SimpSubscription subscription : session.getSubscriptions()) {
                    UUID partyId = WatchPartyDestinations.parsePartyId(subscription.getDestination());
                    if (partyId != null) {
                        result.computeIfAbsent(partyId, key -> new HashSet<>()).add(userId);
                    }
                }
            }
        }
        return result;
    }
}