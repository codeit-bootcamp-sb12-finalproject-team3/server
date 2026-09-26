package com.moduplaylist.realtime.watchparty.websocket;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 강퇴된 사용자의 "이미 열려 있는" 파티 구독을 서버에서 해제한다.
 * 소켓 연결은 유지하고, 해당 파티(/sub/watch-parties/{partyId}/...) 구독만 제거한다.
 */
@Component
public class WatchPartySubscriptionTerminator {

    private static final Logger log = LoggerFactory.getLogger(WatchPartySubscriptionTerminator.class);

    private final SimpUserRegistry simpUserRegistry;
    private final MessageChannel brokerChannel;

    public WatchPartySubscriptionTerminator(
            SimpUserRegistry simpUserRegistry,
            @Qualifier("brokerChannel") MessageChannel brokerChannel
    ) {
        this.simpUserRegistry = simpUserRegistry;
        this.brokerChannel = brokerChannel;
    }

    public void terminate(UUID partyId, UUID userId) {
        SimpUser user = simpUserRegistry.getUser(userId.toString());
        if (user == null) {
            return; // 이 인스턴스에 연결된 세션 없음
        }

        String prefix = "/sub/watch-parties/" + partyId + "/";
        int removed = 0;
        for (SimpSession session : user.getSessions()) {
            for (SimpSubscription subscription : session.getSubscriptions()) {
                String destination = subscription.getDestination();
                if (destination != null && destination.startsWith(prefix)) {
                    brokerChannel.send(unsubscribeMessage(session.getId(), subscription.getId()));
                    removed++;
                }
            }
        }
        if (removed > 0) {
            log.info("강퇴 사용자 구독 해제. partyId={}, userId={}, count={}", partyId, userId, removed);
        }
    }

    private Message<byte[]> unsubscribeMessage(String sessionId, String subscriptionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.UNSUBSCRIBE);
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId(subscriptionId);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}