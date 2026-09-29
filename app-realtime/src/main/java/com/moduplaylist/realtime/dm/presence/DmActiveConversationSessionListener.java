package com.moduplaylist.realtime.dm.presence;

import com.moduplaylist.realtime.dm.websocket.DmActiveConversationController;
import com.moduplaylist.realtime.global.security.RealtimePrincipal;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class DmActiveConversationSessionListener {

    private final DmActiveConversationRegistry activeConversationRegistry;

    public DmActiveConversationSessionListener(
            DmActiveConversationRegistry activeConversationRegistry
    ) {
        this.activeConversationRegistry = activeConversationRegistry;
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (!(accessor.getUser() instanceof RealtimePrincipal principal)) {
            return;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return;
        }
        Object activeConversationId = sessionAttributes.get(
                DmActiveConversationController.ACTIVE_CONVERSATION_ATTRIBUTE
        );
        if (!(activeConversationId instanceof UUID conversationId)) {
            return;
        }

        String sessionId = accessor.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        activeConversationRegistry.deactivate(principal.userId(), conversationId, sessionId);
    }
}
