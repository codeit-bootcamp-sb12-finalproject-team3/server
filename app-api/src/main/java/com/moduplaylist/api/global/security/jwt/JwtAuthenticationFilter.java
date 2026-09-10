package com.moduplaylist.api.global.security.jwt;

import com.moduplaylist.api.global.security.CustomUserDetailsService;
import com.moduplaylist.api.global.security.handler.CustomAuthenticationEntryPoint;
import com.moduplaylist.core.user.repository.JwtRegistry;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final CustomUserDetailsService userDetailsService;
  private final CustomAuthenticationEntryPoint authenticationEntryPoint;
  private final JwtRegistry jwtRegistry;

  private final AccountStatusUserDetailsChecker userDetailsChecker =
      new AccountStatusUserDetailsChecker();

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getServletPath();

    // 만료된 Access Token이 로그인이나 토큰 갱신을 막지 않도록 제외
    return "/api/auth/login".equals(path)
        || "/api/auth/refresh".equals(path)
        || "/api/auth/logout".equals(path);
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {

    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authorization == null
        || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authorization.substring(7).trim();

    try {
      JWTClaimsSet claims = jwtTokenProvider.validateAccessToken(token);
      UUID userId = UUID.fromString(claims.getSubject());

      // Redis에 저장된 현재 로그인에 속한 토큰인지 확인
      if (!jwtRegistry.isAccessTokenActive(userId, claims.getJWTID())) {
        throw new BadCredentialsException("유효하지 않은 로그인 정보입니다.");
      }

      UserDetails userDetails = userDetailsService.loadUserById(userId);
      userDetailsChecker.check(userDetails);

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              userDetails,
              null,
              userDetails.getAuthorities()
          );

      authentication.setDetails(
          new WebAuthenticationDetailsSource().buildDetails(request)
      );

      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);

    } catch (AuthenticationException e) {
      SecurityContextHolder.clearContext();
      authenticationEntryPoint.commence(request, response, e);
      return;
    }

    filterChain.doFilter(request, response);
  }
}