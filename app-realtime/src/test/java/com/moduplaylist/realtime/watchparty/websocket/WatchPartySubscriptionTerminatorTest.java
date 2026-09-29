package com.moduplaylist.realtime.watchparty.websocket;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WatchPartySubscriptionTerminatorTest {

    private final SimpUserRegistry simpUserRegistry = mock(SimpUserRegistry.class);
    private final MessageChannel brokerChannel = mock(MessageChannel.class);
    private final WatchPartySubscriptionTerminator terminator =
            new WatchPartySubscriptionTerminator(simpUserRegistry, brokerChannel);

    private final UUID partyId = UUID.randomUUID();
    private final UUID otherPartyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void 강퇴된_파티의_구독만_UNSUBSCRIBE로_해제한다() {
        SimpSubscription kickedPartySub = subscription("sub-0", "/sub/watch-parties/" + partyId + "/chat");
        SimpSubscription otherPartySub = subscription("sub-1", "/sub/watch-parties/" + otherPartyId + "/chat");
        SimpSession session = session("session-1", kickedPartySub, otherPartySub);
        givenUser(session);

        terminator.terminate(partyId, userId);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(brokerChannel, times(1)).send(captor.capture());

        Message<?> sent = captor.getValue();
        assertThat(SimpMessageHeaderAccessor.getMessageType(sent.getHeaders()))
                .isEqualTo(SimpMessageType.UNSUBSCRIBE);
        assertThat(SimpMessageHeaderAccessor.getSessionId(sent.getHeaders())).isEqualTo("session-1");
        assertThat(SimpMessageHeaderAccessor.getSubscriptionId(sent.getHeaders())).isEqualTo("sub-0");
    }

    @Test
    void 같은_사용자의_여러_세션에_걸친_구독을_모두_해제한다() {
        SimpSession tab1 = session("session-1",
                subscription("sub-0", "/sub/watch-parties/" + partyId + "/chat"));
        SimpSession tab2 = session("session-2",
                subscription("sub-0", "/sub/watch-parties/" + partyId + "/playback"));
        givenUser(tab1, tab2);

        terminator.terminate(partyId, userId);

        verify(brokerChannel, times(2)).send(any());
    }

    @Test
    void 이_서버에_연결된_세션이_없으면_아무것도_보내지_않는다() {
        when(simpUserRegistry.getUser(userId.toString())).thenReturn(null);

        terminator.terminate(partyId, userId);

        verify(brokerChannel, never()).send(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void 종료된_파티는_모든_사용자의_해당_파티_구독만_해제한다() {
        SimpUser userA = userWith(session("session-a",
                subscription("sub-0", "/sub/watch-parties/" + partyId + "/chat"),
                subscription("sub-1", "/sub/watch-parties/" + otherPartyId + "/chat")));
        SimpUser userB = userWith(session("session-b",
                subscription("sub-0", "/sub/watch-parties/" + partyId + "/playback")));
        when(simpUserRegistry.getUsers()).thenReturn(Set.of(userA, userB));

        terminator.terminateAll(partyId);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(brokerChannel, times(2)).send(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(m -> SimpMessageHeaderAccessor.getSessionId(m.getHeaders())
                        + "/" + SimpMessageHeaderAccessor.getSubscriptionId(m.getHeaders()))
                .containsExactlyInAnyOrder("session-a/sub-0", "session-b/sub-0");
    }

    @Test
    void 종료된_파티에_연결된_사용자가_없으면_아무것도_보내지_않는다() {
        when(simpUserRegistry.getUsers()).thenReturn(Set.of());

        terminator.terminateAll(partyId);

        verify(brokerChannel, never()).send(any());
    }

    // ---- 테스트용 mock 생성 도우미 ----

    private void givenUser(SimpSession... sessions) {
        SimpUser user = mock(SimpUser.class);
        when(user.getSessions()).thenReturn(Set.of(sessions));
        when(simpUserRegistry.getUser(userId.toString())).thenReturn(user);
    }

    private SimpSession session(String sessionId, SimpSubscription... subscriptions) {
        SimpSession session = mock(SimpSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.getSubscriptions()).thenReturn(Set.of(subscriptions));
        return session;
    }

    private SimpSubscription subscription(String subscriptionId, String destination) {
        SimpSubscription subscription = mock(SimpSubscription.class);
        when(subscription.getId()).thenReturn(subscriptionId);
        when(subscription.getDestination()).thenReturn(destination);
        return subscription;
    }

    private SimpUser userWith(SimpSession... sessions) {
        SimpUser user = mock(SimpUser.class);
        when(user.getSessions()).thenReturn(Set.of(sessions));
        return user;
    }
}