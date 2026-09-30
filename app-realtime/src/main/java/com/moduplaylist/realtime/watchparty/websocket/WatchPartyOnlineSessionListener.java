package com.moduplaylist.realtime.watchparty.websocket;

import com.moduplaylist.realtime.global.security.RealtimePrincipal;
import com.moduplaylist.realtime.global.security.StompAuthChannelInterceptor;
import java.util.Set;
import java.util.UUID;

import com.moduplaylist.realtime.watchparty.WatchPartyOnlineRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WatchPartyOnlineSessionListener {

    private final WatchPartyOnlineRegistry watchPartyOnlineRegistry;

    public WatchPartyOnlineSessionListener(WatchPartyOnlineRegistry watchPartyOnlineRegistry) {
        this.watchPartyOnlineRegistry = watchPartyOnlineRegistry;
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        if (!(accessor.getUser() instanceof RealtimePrincipal principal)) {
            return;
        }
        if (accessor.getSessionAttributes() == null) {
            return;
        }

        Object attribute = accessor.getSessionAttributes()
                .get(StompAuthChannelInterceptor.ONLINE_PARTY_IDS_ATTRIBUTE);
        if (!(attribute instanceof Set<?> partyIds)) {
            return;
        }

        UUID userId = principal.userId();
        for (Object partyId : partyIds) {
            watchPartyOnlineRegistry.removeOnline((UUID) partyId, userId);
        }
    }
}