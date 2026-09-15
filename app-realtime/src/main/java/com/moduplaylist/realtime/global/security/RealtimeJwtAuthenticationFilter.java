package com.moduplaylist.realtime.global.security;

import com.moduplaylist.core.user.repository.JwtRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class RealtimeJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String SSE_PATH = "/api/sse";

    private final JwtAccessTokenVerifier tokenVerifier;
    private final JwtRegistry jwtRegistry;

    public RealtimeJwtAuthenticationFilter(
            JwtAccessTokenVerifier tokenVerifier,
            JwtRegistry jwtRegistry
    ) {
        this.tokenVerifier = tokenVerifier;
        this.jwtRegistry = jwtRegistry;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !SSE_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                VerifiedAccessToken verifiedToken = tokenVerifier.verify(token);
                if (!jwtRegistry.isAccessTokenActive(
                        verifiedToken.userId(), verifiedToken.tokenId())) {
                    throw new BadCredentialsException("Inactive access token.");
                }

                RealtimePrincipal principal = new RealtimePrincipal(verifiedToken.userId());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, List.of());

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (BadCredentialsException exception) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization)
                && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }

        // Native browser EventSource cannot set the Authorization header.
        return request.getParameter("access_token");
    }
}
