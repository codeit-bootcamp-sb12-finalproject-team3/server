package com.moduplaylist.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API 서버의 인증/인가 및 Spring Security 기본 정책을 설정한다.
 *
 * JWT 인증 구현 시 AuthenticationFilter, 로그인/로그아웃 Handler 등을
 * 이 설정에 추가한다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // CorsConfig의 CORS 정책을 Spring Security에서도 사용
                .cors(cors -> {})

                // JWT 기반 인증을 사용할 예정이므로 우선 CSRF 비활성화
                // 쿠키 기반 인증 정책을 적용하는 경우 추후 다시 검토
                .csrf(csrf -> csrf.disable())

                // JWT 사용을 고려하여 서버 세션을 생성하지 않음
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // URL별 접근 권한
                .authorizeHttpRequests(auth -> auth
                        // 회원가입 / 인증 관련 API
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Spring 기본 오류 처리
                        .requestMatchers("/error").permitAll()

                        /*
                         * 인증 기능 구현 전까지 개발 편의를 위해 허용한다.
                         * JWT 구현 후 authenticated()로 변경할 것.
                         */
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}