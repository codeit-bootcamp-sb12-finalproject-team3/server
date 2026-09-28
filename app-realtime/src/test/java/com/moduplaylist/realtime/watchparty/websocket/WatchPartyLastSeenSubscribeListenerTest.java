package com.moduplaylist.realtime.watchparty.websocket;

import com.moduplaylist.realtime.global.security.RealtimePrincipal;
import com.moduplaylist.realtime.watchparty.WatchPartyLastSeenRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WatchPartyLastSeenSubscribeListenerTest {

    @Mock private WatchPartyLastSeenRegistry lastSeenRegistry;

    @InjectMocks
    private WatchPartyLastSeenSubscribeListener watchPartyLastSeenSubscribeListener;

    private UUID partyId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        partyId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // ===== onSubscribe() =====

    // 케이스 1: 파티 구독이 허용되면 — 다음 하트비트를 기다리지 않고 바로 기록
    @Test
    void onSubscribe_파티_구독이면_즉시_기록() {
        watchPartyLastSeenSubscribeListener.onSubscribe(
                subscribeEvent("/sub/watch-parties/" + partyId + "/chat", new RealtimePrincipal(userId)));

        verify(lastSeenRegistry).touch(eq(partyId), eq(userId), anyLong());
    }

    // 케이스 2: 파티 구독 경로가 아니면 — 기록하지 않음
    @Test
    void onSubscribe_파티_경로_아니면_무시() {
        watchPartyLastSeenSubscribeListener.onSubscribe(
                subscribeEvent("/user/queue/errors", new RealtimePrincipal(userId)));

        verifyNoInteractions(lastSeenRegistry);
    }

    // 케이스 3: 인증된 사용자가 아니면 — 기록하지 않음
    @Test
    void onSubscribe_인증_사용자_아니면_무시() {
        watchPartyLastSeenSubscribeListener.onSubscribe(
                subscribeEvent("/sub/watch-parties/" + partyId + "/chat", null));

        verifyNoInteractions(lastSeenRegistry);
    }

    // ---- 테스트용 도우미 ----

    // 실제 STOMP SUBSCRIBE 프레임과 같은 헤더를 가진 이벤트
    private SessionSubscribeEvent subscribeEvent(String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionSubscribeEvent(this, message, user);
    }
}