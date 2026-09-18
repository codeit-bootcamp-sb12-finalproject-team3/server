package com.moduplaylist.realtime.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import com.moduplaylist.realtime.watchparty.WatchPartyActivePartyRegistry;
import com.moduplaylist.realtime.watchparty.WatchPartyHostRegistry;
import com.moduplaylist.realtime.watchparty.WatchPartyJoinedRegistry;
import com.moduplaylist.realtime.watchparty.WatchPartyKickedRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import org.springframework.security.authentication.BadCredentialsException;

class StompAuthChannelInterceptorTest {

    private JwtAccessTokenVerifier tokenVerifier;
    private AccessTokenSessionRegistry accessTokenSessionRegistry;
    private StompAuthChannelInterceptor interceptor;
    private WatchPartyKickedRegistry watchPartyKickedRegistry;
    private WatchPartyJoinedRegistry watchPartyJoinedRegistry;
    private WatchPartyHostRegistry watchPartyHostRegistry;
    private WatchPartyActivePartyRegistry watchPartyActivePartyRegistry;
    private SimpMessagingTemplate messagingTemplate;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TOKEN_ID = "test-jti";


    private Message<byte[]> createConnectMessage(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @BeforeEach
    void setUp() {
        tokenVerifier = mock(JwtAccessTokenVerifier.class);
        accessTokenSessionRegistry = mock(AccessTokenSessionRegistry.class);
        watchPartyKickedRegistry = mock(WatchPartyKickedRegistry.class);
        watchPartyJoinedRegistry = mock(WatchPartyJoinedRegistry.class);
        watchPartyHostRegistry = mock(WatchPartyHostRegistry.class);
        watchPartyActivePartyRegistry = mock(WatchPartyActivePartyRegistry.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);

        interceptor = new StompAuthChannelInterceptor(
                tokenVerifier,
                accessTokenSessionRegistry,
                watchPartyKickedRegistry,
                watchPartyJoinedRegistry,
                watchPartyHostRegistry,
                watchPartyActivePartyRegistry,
                messagingTemplate
        );

    }

    @Test
    @DisplayName("CONNECT가 아닌 프레임은 그냥 통과시킨다")
    void nonConnectFrame_passesThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isEqualTo(message);
        verify(tokenVerifier, never()).verify(any());
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 거부한다")
    void missingToken_throwsBadCredentials() {
        Message<byte[]> message = createConnectMessage(null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 거부한다")
    void malformedAuthorizationHeader_throwsBadCredentials() {
        Message<byte[]> message = createConnectMessage("Basic xxx");

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("서명 검증에 실패하면 거부한다")
    void invalidSignature_throwsBadCredentials() {
        Message<byte[]> message = createConnectMessage("Bearer invalid-token");
        when(tokenVerifier.verify(anyString()))
                .thenThrow(new BadCredentialsException("Invalid access token."));

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Redis에 비활성 토큰이면 거부한다")
    void inactiveToken_throwsBadCredentials() {
        Message<byte[]> message = createConnectMessage("Bearer valid-token");
        when(tokenVerifier.verify(anyString()))
                .thenReturn(new VerifiedAccessToken(USER_ID, TOKEN_ID));
        when(accessTokenSessionRegistry.isAccessTokenActive(USER_ID, TOKEN_ID)).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Redis 조회 실패 시 fail-closed로 거부한다")
    void redisFailure_throwsBadCredentials() {
        Message<byte[]> message = createConnectMessage("Bearer valid-token");
        when(tokenVerifier.verify(anyString()))
                .thenReturn(new VerifiedAccessToken(USER_ID, TOKEN_ID));
        when(accessTokenSessionRegistry.isAccessTokenActive(any(), any()))
                .thenThrow(new DataAccessException("redis down") {});

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(BadCredentialsException.class)
                .hasCauseInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("모든 검증 통과 시 Principal을 세팅한다")
    void validToken_setsPrincipal() {
        Message<byte[]> message = createConnectMessage("Bearer valid-token");
        when(tokenVerifier.verify(anyString()))
                .thenReturn(new VerifiedAccessToken(USER_ID, TOKEN_ID));
        when(accessTokenSessionRegistry.isAccessTokenActive(USER_ID, TOKEN_ID)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, null);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isEqualTo(new RealtimePrincipal(USER_ID));
    }


    private Message<byte[]> createMessage(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(new RealtimePrincipal(USER_ID)); // CONNECT 때 이미 붙었다고 가정
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("강퇴된 유저의 SUBSCRIBE는 조용히 거부한다")
    void kickedUser_subscribe_silentlyRejected() {
        UUID partyId = UUID.randomUUID();
        Message<byte[]> message = createMessage
                    (StompCommand.SUBSCRIBE, "/sub/watch-parties/" + partyId + "/chat");
        when(watchPartyKickedRegistry.isKicked(partyId, USER_ID)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNull();
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    @DisplayName("이미 다른 방에 JOINED인 유저가 다른 파티를 SUBSCRIBE하면 에러 응답 후 거부한다")
    void joinedElsewhere_subscribe_rejectedWithError() {
        UUID partyId = UUID.randomUUID();
        UUID otherPartyId = UUID.randomUUID();
        Message<byte[]> message = createMessage
                    (StompCommand.SUBSCRIBE, "/sub/watch-parties/" + partyId + "/chat");
        when(watchPartyKickedRegistry.isKicked(partyId, USER_ID)).thenReturn(false);
        when(watchPartyActivePartyRegistry.findJoinedPartyId(USER_ID)).thenReturn(Optional.of(otherPartyId));

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNull();
        verify(messagingTemplate)
                .convertAndSendToUser(eq(USER_ID.toString()), eq("/sub/errors"), any());
    }

    @Test
    @DisplayName("JOINED 상태가 없는 유저의 SUBSCRIBE는 통과시킨다")
    void noActiveParty_subscribe_passesThrough() {
        UUID partyId = UUID.randomUUID();
        Message<byte[]> message = createMessage
                    (StompCommand.SUBSCRIBE, "/sub/watch-parties/" + partyId + "/chat");
        when(watchPartyKickedRegistry.isKicked(partyId, USER_ID)).thenReturn(false);
        when(watchPartyActivePartyRegistry.findJoinedPartyId(USER_ID)).thenReturn(Optional.empty());

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isEqualTo(message);
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    @DisplayName("요청한 방과 같은 방에 이미 JOINED면 SUBSCRIBE를 통과시킨다")
    void activePartyMatchesRequested_subscribe_passesThrough() {
        UUID partyId = UUID.randomUUID();
        Message<byte[]> message = createMessage
                    (StompCommand.SUBSCRIBE, "/sub/watch-parties/" + partyId + "/chat");
        when(watchPartyKickedRegistry.isKicked(partyId, USER_ID)).thenReturn(false);
        when(watchPartyActivePartyRegistry.findJoinedPartyId(USER_ID)).thenReturn(Optional.of(partyId));

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isEqualTo(message);
    }

    @Test
    @DisplayName("강퇴된 유저의 SEND는 조용히 거부한다")
    void kickedUser_send_silentlyRejected() {
        UUID partyId = UUID.randomUUID();
        Message<byte[]> message = createMessage
                    (StompCommand.SEND, "/pub/watch-parties/" + partyId + "/chat");
        when(watchPartyKickedRegistry.isKicked(partyId, USER_ID)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNull();
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    @DisplayName("참가자도 방장도 아닌 유저의 SEND는 에러 응답 후 거부한다")
    void notJoinedNorHost_send_rejectedWithError() {
        UUID partyId = UUID.randomUUID();
        Message<byte[]> message = createMessage
                    (StompCommand.SEND, "/pub/watch-parties/" + partyId + "/chat");
        when(watchPartyKickedRegistry.isKicked(partyId, USER_ID)).thenReturn(false);
        when(watchPartyJoinedRegistry.isJoined(partyId, USER_ID)).thenReturn(false);
        when(watchPartyHostRegistry.isHost(partyId, USER_ID)).thenReturn(false);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNull();
        verify(messagingTemplate)
                .convertAndSendToUser(eq(USER_ID.toString()), eq("/sub/errors"), any());
    }

    @Test
    @DisplayName("정식 참가자(JOINED)의 SEND는 통과시킨다")
    void joinedUser_send_passesThrough() {
        UUID partyId = UUID.randomUUID();
        Message<byte[]> message = createMessage
                    (StompCommand.SEND, "/pub/watch-parties/" + partyId + "/chat");
        when(watchPartyKickedRegistry.isKicked(partyId, USER_ID)).thenReturn(false);
        when(watchPartyJoinedRegistry.isJoined(partyId, USER_ID)).thenReturn(true);
        when(watchPartyHostRegistry.isHost(partyId, USER_ID)).thenReturn(false);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isEqualTo(message);
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    @DisplayName("방장(host)의 SEND는 JOINED가 아니어도 통과시킨다")
    void hostUser_send_passesThrough() {
        UUID partyId = UUID.randomUUID();
        Message<byte[]> message = createMessage
                    (StompCommand.SEND, "/pub/watch-parties/" + partyId + "/chat");
        when(watchPartyKickedRegistry.isKicked(partyId, USER_ID)).thenReturn(false);
        when(watchPartyJoinedRegistry.isJoined(partyId, USER_ID)).thenReturn(false);
        when(watchPartyHostRegistry.isHost(partyId, USER_ID)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isEqualTo(message);
    }

    @Test
    @DisplayName("watch-party가 아닌 destination의 SUBSCRIBE/SEND는 인가 체크 없이 통과한다")
    void nonWatchPartyDestination_passesThroughWithoutCheck() {
        Message<byte[]> subscribe = createMessage
                    (StompCommand.SUBSCRIBE, "/sub/conversations/1");
        Message<byte[]> send = createMessage(StompCommand.SEND, "/pub/conversations/1");

        assertThat(interceptor.preSend(subscribe, null)).isEqualTo(subscribe);
        assertThat(interceptor.preSend(send, null)).isEqualTo(send);
        verifyNoInteractions(watchPartyKickedRegistry, watchPartyJoinedRegistry,
                                watchPartyHostRegistry, watchPartyActivePartyRegistry);
    }

    @Test
    @DisplayName("watch-party 경로인데 partyId가 UUID 형식이 아니면 SUBSCRIBE를 에러 응답 후 거부한다")
    void invalidPartyIdFormat_subscribe_rejectedWithError() {
        Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, "/sub/watch-parties/not-a-uuid/chat");

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNull();
        verify(messagingTemplate)
                .convertAndSendToUser(eq(USER_ID.toString()), eq("/sub/errors"), any());
        verifyNoInteractions(watchPartyKickedRegistry, watchPartyJoinedRegistry,
                watchPartyHostRegistry, watchPartyActivePartyRegistry);
    }

    @Test
    @DisplayName("watch-party 경로인데 partyId가 UUID 형식이 아니면 SEND를 에러 응답 후 거부한다")
    void invalidPartyIdFormat_send_rejectedWithError() {
        Message<byte[]> message = createMessage(StompCommand.SEND, "/pub/watch-parties/not-a-uuid/chat");

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNull();
        verify(messagingTemplate)
                .convertAndSendToUser(eq(USER_ID.toString()), eq("/sub/errors"), any());
        verifyNoInteractions(watchPartyKickedRegistry, watchPartyJoinedRegistry,
                watchPartyHostRegistry, watchPartyActivePartyRegistry);
    }
}