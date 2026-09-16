package com.moduplaylist.realtime.global.config;

import com.moduplaylist.core.user.repository.JwtRegistry;
import com.moduplaylist.realtime.global.security.JwtAccessTokenVerifier;
import com.moduplaylist.realtime.global.security.RealtimeJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class RealtimeSecurityConfig {

    @Bean
    public SecurityFilterChain realtimeSecurityFilterChain(
            HttpSecurity http,
            JwtAccessTokenVerifier tokenVerifier,
            JwtRegistry jwtRegistry
    ) throws Exception {
        RealtimeJwtAuthenticationFilter jwtFilter =
                new RealtimeJwtAuthenticationFilter(tokenVerifier, jwtRegistry);

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/sse").authenticated()
                        .requestMatchers("/ws/**", "/actuator/health", "/error").permitAll()
                        .anyRequest().denyAll())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(
                        (request, response, cause) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
