package com.moduplaylist.realtime.watchparty.websocket;

import com.moduplaylist.realtime.global.security.RealtimePrincipal;
import com.moduplaylist.realtime.watchparty.WatchPartyPlaybackRegistry;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class WatchPartyPlaybackSyncListener {

    private static final Pattern PLAYBACK_DESTINATION_PATTERN =
            Pattern.compile("^/sub/watch-parties/([^/]+)/playback$");

    private final WatchPartyPlaybackRegistry watchPartyPlaybackRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    public WatchPartyPlaybackSyncListener(
            WatchPartyPlaybackRegistry watchPartyPlaybackRegistry,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.watchPartyPlaybackRegistry = watchPartyPlaybackRegistry;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher matcher = PLAYBACK_DESTINATION_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        UUID partyId;
        try {
            partyId = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException invalidUuid) {
            return;
        }

        if (!(event.getUser() instanceof RealtimePrincipal principal)) {
            return;
        }

        watchPartyPlaybackRegistry.find(partyId)
                .ifPresent(state -> messagingTemplate.convertAndSendToUser(
                        principal.userId().toString(), "/queue/playback-sync", state));
    }
}