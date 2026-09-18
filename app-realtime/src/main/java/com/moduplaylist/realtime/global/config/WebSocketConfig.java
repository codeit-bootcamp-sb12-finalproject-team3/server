package com.moduplaylist.realtime.global.config;

import com.moduplaylist.realtime.global.security.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP 기반 WebSocket 통신 설정.
 *
 * /pub : Client -> Server 메시지 발행 경로
 * /sub : Server -> Client 구독 경로
 * /ws  : WebSocket 연결 Endpoint
 *
 * JWT 인증/인가 Interceptor : StompAuthChannelInterceptor (CONNECT 시점, #52)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Server -> Client
        config.enableSimpleBroker("/sub", "/queue");

        // Client -> Server
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:3000",
                        "http://localhost:5173"
                )
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    /*
     * TODO(auth):
     * - 인증/인가 예외 처리 정책 (현재는 Spring 기본 STOMP ERROR 프레임에만 의존)
     * - SEND/SUBSCRIBE 권한 검증 — #53
     * - 이미 연결된 세션에 로그아웃/토큰만료 반영 여부
     */
}
