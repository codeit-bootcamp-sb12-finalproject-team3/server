package com.moduplaylist.realtime.watchparty.websocket;

import com.moduplaylist.realtime.global.security.RealtimePrincipal;
import com.moduplaylist.realtime.watchparty.WatchPartyLastSeenRegistry;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

// 재접속 직후 다음 하트비트(최대 1분)를 기다리다 5분을 넘겨 정리되는 것을 막기 위해
// 구독이 "허용된" 순간 바로 기록한다. (인터셉터가 거부하면 이 이벤트는 발행되지 않음)
@Component
public class WatchPartyLastSeenSubscribeListener {

    private final WatchPartyLastSeenRegistry lastSeenRegistry;

    public WatchPartyLastSeenSubscribeListener(WatchPartyLastSeenRegistry lastSeenRegistry) {
        this.lastSeenRegistry = lastSeenRegistry;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        if (!(event.getUser() instanceof RealtimePrincipal principal)) {
            return;
        }
        String destination = StompHeaderAccessor.wrap(event.getMessage()).getDestination();
        UUID partyId = WatchPartyDestinations.parsePartyId(destination);
        if (partyId == null) {
            return;
        }
        lastSeenRegistry.touch(partyId, principal.userId(), Instant.now().toEpochMilli());
    }
}