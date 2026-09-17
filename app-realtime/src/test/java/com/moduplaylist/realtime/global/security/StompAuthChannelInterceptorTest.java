package com.moduplaylist.realtime.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import org.springframework.security.authentication.BadCredentialsException;

class StompAuthChannelInterceptorTest {

    private JwtAccessTokenVerifier tokenVerifier;
    private AccessTokenSessionRegistry accessTokenSessionRegistry;
    private StompAuthChannelInterceptor interceptor;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TOKEN_ID = "test-jti";

    private Message<byte[]> createConnectMessage(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @BeforeEach
    void setUp() {
        tokenVerifier = mock(JwtAccessTokenVerifier.class);
        accessTokenSessionRegistry = mock(AccessTokenSessionRegistry.class);
        interceptor = new StompAuthChannelInterceptor(tokenVerifier, accessTokenSessionRegistry);
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
}