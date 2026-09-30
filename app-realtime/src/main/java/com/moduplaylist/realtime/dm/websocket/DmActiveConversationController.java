package com.moduplaylist.realtime.dm.websocket;

import com.moduplaylist.realtime.dm.presence.DmActiveConversationRegistry;
import com.moduplaylist.realtime.global.security.RealtimePrincipal;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class DmActiveConversationController {

    public static final String ACTIVE_CONVERSATION_ATTRIBUTE = "dmActiveConversationId";

    private final DmActiveConversationRegistry activeConversationRegistry;

    public DmActiveConversationController(DmActiveConversationRegistry activeConversationRegistry) {
        this.activeConversationRegistry = activeConversationRegistry;
    }

    @MessageMapping("/dm/conversations/activate")
    public void activate(
            DmConversationActivityRequest request,
            Principal principal,
            StompHeaderAccessor accessor
    ) {
        UUID userId = requireUserId(principal);
        UUID conversationId = requireConversationId(request);
        String sessionId = requireSessionId(accessor);
        Map<String, Object> sessionAttributes = requireSessionAttributes(accessor);

        Object previous = sessionAttributes.get(ACTIVE_CONVERSATION_ATTRIBUTE);
        if (previous instanceof UUID previousConversationId
                && !previousConversationId.equals(conversationId)) {
            activeConversationRegistry.deactivate(userId, previousConversationId, sessionId);
        }

        activeConversationRegistry.activate(userId, conversationId, sessionId);
        sessionAttributes.put(ACTIVE_CONVERSATION_ATTRIBUTE, conversationId);
    }

    @MessageMapping("/dm/conversations/deactivate")
    public void deactivate(
            DmConversationActivityRequest request,
            Principal principal,
            StompHeaderAccessor accessor
    ) {
        UUID userId = requireUserId(principal);
        UUID conversationId = requireConversationId(request);
        String sessionId = requireSessionId(accessor);
        Map<String, Object> sessionAttributes = requireSessionAttributes(accessor);

        if (!conversationId.equals(sessionAttributes.get(ACTIVE_CONVERSATION_ATTRIBUTE))) {
            return;
        }

        activeConversationRegistry.deactivate(userId, conversationId, sessionId);
        sessionAttributes.remove(ACTIVE_CONVERSATION_ATTRIBUTE, conversationId);
    }

    private UUID requireUserId(Principal principal) {
        if (principal instanceof RealtimePrincipal realtimePrincipal) {
            return realtimePrincipal.userId();
        }
        throw new IllegalArgumentException("Authenticated principal is required.");
    }

    private UUID requireConversationId(DmConversationActivityRequest request) {
        if (request == null || request.conversationId() == null) {
            throw new IllegalArgumentException("conversationId is required.");
        }
        return request.conversationId();
    }

    private String requireSessionId(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("STOMP sessionId is required.");
        }
        return sessionId;
    }

    private Map<String, Object> requireSessionAttributes(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new IllegalArgumentException("STOMP session attributes are required.");
        }
        return sessionAttributes;
    }
}
