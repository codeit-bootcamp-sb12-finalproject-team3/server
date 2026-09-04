package com.moduplaylist.realtime.global.config;

import org.springframework.context.annotation.Configuration;
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
 * JWT 인증/인가 Interceptor는 인증 기능 구현 시 추가한다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Server -> Client
        config.enableSimpleBroker("/sub");

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

    /*
     * TODO(auth):
     * JWT 인증 구현 후 아래 항목 추가
     * - JwtAuthenticationFilter
     * - 인증/인가 예외 처리
     * - 실제 URL 권한 정책
     * - Refresh/Logout 정책
     */

    /*
     * TODO(auth):
     * JWT 인증 구현 후 ClientInboundChannel에
     * 인증 및 권한 Interceptor를 추가한다.
     *
     * CONNECT 시 JWT 검증 후 Principal 설정,
     * SEND/SUBSCRIBE 권한 검증 필요.
     */
}