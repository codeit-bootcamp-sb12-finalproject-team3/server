package com.moduplaylist.api.global.config;

import com.moduplaylist.api.global.security.CustomUserDetailsService;
import com.moduplaylist.api.global.security.handler.CustomAccessDeniedHandler;
import com.moduplaylist.api.global.security.handler.CustomAuthenticationEntryPoint;
import com.moduplaylist.api.global.security.handler.CustomAuthenticationFailureHandler;
import com.moduplaylist.api.global.security.handler.CustomAuthenticationSuccessHandler;
import com.moduplaylist.api.global.security.jwt.JwtAuthenticationFilter;
import com.moduplaylist.api.global.security.jwt.JwtTokenProvider;
import com.moduplaylist.core.user.repository.JwtRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * API 서버의 인증/인가 및 Spring Security 기본 정책을 설정한다.
 *
 * JWT 인증 필터, 로그인 Handler 등을 이 설정에 연결한다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtTokenProvider jwtTokenProvider;
  private final JwtRegistry jwtRegistry;
  private final CustomUserDetailsService userDetailsService;
  private final CustomAuthenticationSuccessHandler successHandler;
  private final CustomAuthenticationFailureHandler failureHandler;
  private final CustomAuthenticationEntryPoint authenticationEntryPoint;
  private final CustomAccessDeniedHandler accessDeniedHandler;

  @Value("${security.jwt.cookie-secure}")
  private boolean cookieSecure;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    CookieCsrfTokenRepository csrfTokenRepository =
        CookieCsrfTokenRepository.withHttpOnlyFalse();

    csrfTokenRepository.setCookiePath("/");
    csrfTokenRepository.setCookieCustomizer(cookie -> cookie
        .secure(cookieSecure)
        .sameSite("Lax")
    );

    // 쿠키에서 읽어 헤더로 전달한 CSRF 토큰을 그대로 비교한다.
    CsrfTokenRequestAttributeHandler csrfTokenRequestHandler =
        new CsrfTokenRequestAttributeHandler();

    JwtAuthenticationFilter jwtAuthenticationFilter =
        new JwtAuthenticationFilter(
            jwtTokenProvider,
            userDetailsService,
            jwtRegistry
        );

    http
        // CorsConfig의 CORS 정책을 Spring Security에서도 사용
        .cors(cors -> {})

        // Refresh Token을 쿠키로 전달하므로 쿠키 기반 CSRF 보호 적용
        // Cookie: XSRF-TOKEN / Header: X-XSRF-TOKEN
        .csrf(csrf -> csrf
            .csrfTokenRepository(csrfTokenRepository)
            .csrfTokenRequestHandler(csrfTokenRequestHandler)
        )

        // JWT 사용을 고려하여 서버 세션을 생성하지 않음
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )

        .requestCache(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)

        // 로그아웃은 Redis 무효화를 수행하는 API에서 처리한다.
        .logout(AbstractHttpConfigurer::disable)

        // URL별 접근 권한
        .authorizeHttpRequests(auth -> auth
            // 회원가입 / 인증 관련 API
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
            .requestMatchers(
                HttpMethod.POST,
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/auth/logout"
            ).permitAll()
            .requestMatchers(
                HttpMethod.GET,
                "/api/auth/csrf-token"
            ).permitAll()

            // Swagger
            .requestMatchers(
                "/swagger-ui/**",
                "/v3/api-docs/**"
            ).permitAll()

            // Spring 기본 오류 처리
            .requestMatchers("/error").permitAll()

            // 그 외 요청은 JWT 인증 필요
            .anyRequest().authenticated()
        )

        // 이메일·비밀번호 로그인 및 성공·실패 처리
        .formLogin(form -> form
            .loginProcessingUrl("/api/auth/login")
            .usernameParameter("email")
            .passwordParameter("password")
            .successHandler(successHandler)
            .failureHandler(failureHandler)
        )

        // 인증·인가 오류를 공통 JSON 형식으로 응답
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler)
        )

        // 로그인 필터 앞에 JWT 인증 필터 등록
        .addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        );

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}