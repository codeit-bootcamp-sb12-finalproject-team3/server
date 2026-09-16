package com.moduplaylist.realtime.global.security;

import com.moduplaylist.core.user.repository.JwtRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);
    private final JwtAccessTokenVerifier tokenVerifier;
    private final JwtRegistry jwtRegistry;

    public StompAuthChannelInterceptor(JwtAccessTokenVerifier tokenVerifier, JwtRegistry jwtRegistry) {
        this.tokenVerifier = tokenVerifier;
        this.jwtRegistry = jwtRegistry;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        // 토큰 꺼내기
        String token = resolveToken(accessor);
        if (!StringUtils.hasText(token)) {
            throw new BadCredentialsException("Access token is required.");
        }

        // 서명/클레임 검증 & 인증 실패 처리
        VerifiedAccessToken verifiedToken = tokenVerifier.verify(token);

        // Redis jti 활성 세션 확인
        try {
            if (!jwtRegistry.isAccessTokenActive(verifiedToken.userId(), verifiedToken.tokenId())) {
                throw new BadCredentialsException("Inactive access token.");
            }
        } catch (DataAccessException redisFailure) {
            log.warn("Redis jti 확인 실패로 WebSocket 인증을 거부했습니다. userId={}",
                    verifiedToken.userId(), redisFailure);
            throw new BadCredentialsException("Authentication service unavailable.", redisFailure);
        }

        // 인증 성공 처리
        accessor.setUser(new RealtimePrincipal(verifiedToken.userId()));
        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(authorization)
                && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        return null;
    }
}