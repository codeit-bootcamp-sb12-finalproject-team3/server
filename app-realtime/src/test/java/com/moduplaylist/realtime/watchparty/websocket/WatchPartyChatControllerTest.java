package com.moduplaylist.realtime.watchparty.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.realtime.watchparty.WatchPartyChatCooldownRegistry;
import com.moduplaylist.realtime.watchparty.WatchPartyChatLogRegistry;
import com.moduplaylist.realtime.watchparty.WatchPartyPlaybackRegistry;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyChatSendRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WatchPartyChatControllerTest {

    @Mock
    private WatchPartyChatLogRegistry watchPartyChatLogRegistry;
    @Mock
    private WatchPartyPlaybackRegistry watchPartyPlaybackRegistry;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private WatchPartyChatCooldownRegistry watchPartyChatCooldownRegistry;

    private WatchPartyChatController controller;
    private UUID partyId;
    private UUID senderId;
    private Principal principal;

    @BeforeEach
    void setUp() {
        // 컨트롤러 생성자 파라미터 순서와 똑같이 (쿨다운 Registry가 맨 끝)
        controller = new WatchPartyChatController(
                watchPartyChatLogRegistry,
                watchPartyPlaybackRegistry,
                redisTemplate,
                new ObjectMapper(),
                messagingTemplate,
                watchPartyChatCooldownRegistry
        );
        partyId = UUID.randomUUID();
        senderId = UUID.randomUUID();
        principal = () -> senderId.toString();
    }

    // ===== sendChat() =====

    // 케이스 1: 쿨다운이 비어 있으면 이력 저장 + 방송
    @Test
    void sendChat_쿨다운_통과_저장하고_방송한다() {
        given(watchPartyPlaybackRegistry.find(partyId)).willReturn(Optional.empty());
        given(watchPartyChatCooldownRegistry.tryAcquire(partyId, senderId, WatchPartyChatController.CHAT_COOLDOWN))
                .willReturn(true);

        controller.sendChat(partyId, request("안녕하세요"), principal);

        verify(watchPartyChatLogRegistry).append(eq(partyId), any());
        verify(redisTemplate).convertAndSend(eq("watchparty:" + partyId + ":chat"), anyString());
    }

    // 케이스 2: 쿨다운 중이면 에러 안내만 보내고 저장·방송하지 않음
    @Test
    void sendChat_쿨다운_중_에러를_보내고_저장_방송하지_않는다() {
        given(watchPartyPlaybackRegistry.find(partyId)).willReturn(Optional.empty());
        given(watchPartyChatCooldownRegistry.tryAcquire(partyId, senderId, WatchPartyChatController.CHAT_COOLDOWN))
                .willReturn(false);

        controller.sendChat(partyId, request("ㅋ"), principal);

        verify(messagingTemplate).convertAndSendToUser(
                senderId.toString(), "/queue/errors", "채팅은 1초에 한 번만 보낼 수 있습니다.");
        verify(watchPartyChatLogRegistry, never()).append(any(), any());
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    // 케이스 3: 빈 메시지는 쿨다운을 쓰지 않음 (고쳐서 바로 다시 보낼 수 있게)
    @Test
    void sendChat_빈_메시지_쿨다운을_확인하지_않는다() {
        given(watchPartyPlaybackRegistry.find(partyId)).willReturn(Optional.empty());

        controller.sendChat(partyId, request("   "), principal);

        verify(watchPartyChatCooldownRegistry, never()).tryAcquire(any(), any(), any());
        verify(watchPartyChatLogRegistry, never()).append(any(), any());
    }

    private WatchPartyChatSendRequest request(String content) {
        WatchPartyChatSendRequest request = new WatchPartyChatSendRequest();
        ReflectionTestUtils.setField(request, "content", content);   // DTO에 setter·생성자가 없어서 필드에 직접 넣음
        return request;
    }
}