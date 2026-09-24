package com.moduplaylist.realtime.global.security;


import com.moduplaylist.realtime.watchparty.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);
    // watch-parties SEND/SUBSCRIBE destination에서 partyId(UUID)를 뽑기 위한 패턴
    private static final Pattern WATCH_PARTY_DESTINATION_PATTERN =
            Pattern.compile("^/(?:pub|sub)/watch-parties/([^/]+)/.*$");
    public static final String ONLINE_PARTY_IDS_ATTRIBUTE = "watchPartyOnlineIds";

    private final JwtAccessTokenVerifier tokenVerifier;
    private final AccessTokenSessionRegistry accessTokenSessionRegistry;
    private final WatchPartyKickedRegistry watchPartyKickedRegistry;
    private final WatchPartyJoinedRegistry watchPartyJoinedRegistry;
    private final WatchPartyHostRegistry watchPartyHostRegistry;
    private final WatchPartyActivePartyRegistry watchPartyActivePartyRegistry;
    private final WatchPartyOnlineRegistry watchPartyOnlineRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    public StompAuthChannelInterceptor(
            JwtAccessTokenVerifier tokenVerifier,
            AccessTokenSessionRegistry accessTokenSessionRegistry,
            WatchPartyKickedRegistry watchPartyKickedRegistry,
            WatchPartyJoinedRegistry watchPartyJoinedRegistry,
            WatchPartyHostRegistry watchPartyHostRegistry,
            WatchPartyActivePartyRegistry watchPartyActivePartyRegistry,
            WatchPartyOnlineRegistry watchPartyOnlineRegistry,
            @Lazy SimpMessagingTemplate messagingTemplate
    ) {
        this.tokenVerifier = tokenVerifier;
        this.accessTokenSessionRegistry = accessTokenSessionRegistry;
        this.watchPartyKickedRegistry = watchPartyKickedRegistry;
        this.watchPartyJoinedRegistry = watchPartyJoinedRegistry;
        this.watchPartyHostRegistry = watchPartyHostRegistry;
        this.watchPartyActivePartyRegistry = watchPartyActivePartyRegistry;
        this.watchPartyOnlineRegistry = watchPartyOnlineRegistry;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            return handleConnect(message, accessor);
        }
        if (StompCommand.SUBSCRIBE.equals(command)) {
            return handleSubscribe(message, accessor);
        }
        if (StompCommand.SEND.equals(command)) {
            return handleSend(message, accessor);
        }
        return message;
    }

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        // 토큰 꺼내기
        String token = resolveToken(accessor);
        if (!StringUtils.hasText(token)) {
            throw new BadCredentialsException("Access token is required.");
        }

        // 서명/클레임 검증 & 인증 실패 처리
        VerifiedAccessToken verifiedToken = tokenVerifier.verify(token);

        // Redis jti 활성 세션 확인
        try {
            if (!accessTokenSessionRegistry.isAccessTokenActive(verifiedToken.userId(), verifiedToken.tokenId())) {
                throw new BadCredentialsException("Inactive access token.");
            }
        } catch (DataAccessException redisFailure) {
            log.warn("Redis jti 확인 실패로 WebSocket 인증을 거부했습니다. userId={}",
                    verifiedToken.userId(), redisFailure);
            throw new BadCredentialsException("Authentication service unavailable.", redisFailure);
        }

        // 인증 성공 처리
        accessor.setUser(new RealtimePrincipal(verifiedToken.userId()));
        return message;
    }

    // SUBSCRIBE: kicked 체크 + "이미 다른 방에 JOINED면 구경 차단" 체크
    private Message<?> handleSubscribe(Message<?> message, StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (!isWatchPartyDestination(destination)) {
            return message;
        }
        UUID userId = resolveUserId(accessor);
        UUID partyId = parsePartyId(destination);
        if (partyId == null) {
            sendError(userId, "잘못된 요청입니다.");
            return null;
        }

        if (watchPartyKickedRegistry.isKicked(partyId, userId)) {
            return null;
        }

        Optional<UUID> joinedPartyId = watchPartyActivePartyRegistry.findJoinedPartyId(userId);
        if (joinedPartyId.isPresent() && !joinedPartyId.get().equals(partyId)) {
            sendError(userId, "이미 시청 중인 Watch Party가 있습니다. 먼저 나가주세요.");
            return null;
        }

        if (trackOnlineParty(accessor, partyId)) {
            watchPartyOnlineRegistry.addOnline(partyId, userId);
        }

        return message;
    }

    @SuppressWarnings("unchecked")
    private boolean trackOnlineParty(StompHeaderAccessor accessor, UUID partyId) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return false;
        }
        Set<UUID> onlinePartyIds = (Set<UUID>) sessionAttributes
                .computeIfAbsent(ONLINE_PARTY_IDS_ATTRIBUTE, key -> new HashSet<UUID>());
        return onlinePartyIds.add(partyId);
    }

    // SEND: kicked 체크 + "JOINED 또는 host만 채팅 가능" 체크
    private Message<?> handleSend(Message<?> message, StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (!isWatchPartyDestination(destination)) {
            return message;
        }
        UUID userId = resolveUserId(accessor);
        UUID partyId = parsePartyId(destination);
        if (partyId == null) {
            sendError(userId, "잘못된 요청입니다.");
            return null;
        }

        if (watchPartyKickedRegistry.isKicked(partyId, userId)) {
            return null;
        }

        boolean allowed = watchPartyJoinedRegistry.isJoined(partyId, userId)
                || watchPartyHostRegistry.isHost(partyId, userId);
        if (!allowed) {
            sendError(userId, "참가 후 채팅이 가능합니다.");
            return null;
        }

        return message;
    }

    // destination이 watch-party 관련 경로인지 여부만 판단 (패턴 매칭)
    private boolean isWatchPartyDestination(String destination) {
        return destination != null && WATCH_PARTY_DESTINATION_PATTERN.matcher(destination).matches();
    }

    // watch-party 경로에서 partyId(UUID)를 파싱. 경로는 맞지만 형식이 UUID가 아니면 null
    private UUID parsePartyId(String destination) {
        Matcher matcher = WATCH_PARTY_DESTINATION_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException invalidUuid) {
            return null;
        }
    }


    // CONNECT 때 심어둔 RealtimePrincipal에서 userId 꺼내기
    private UUID resolveUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof RealtimePrincipal principal) {
            return principal.userId();
        }
        throw new BadCredentialsException("Authenticated principal not found.");
    }

    // 개인 에러 큐(/user/queue/errors)로 안내 메시지 전송
    private void sendError(UUID userId, String errorMessage) {
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/errors", errorMessage);
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
