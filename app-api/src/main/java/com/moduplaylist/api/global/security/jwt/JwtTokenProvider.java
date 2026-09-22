package com.moduplaylist.api.global.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.BadCredentialsException;

@Component
public class JwtTokenProvider {

  private final byte[] secretKey;
  private final String issuer;
  private final long accessTokenValiditySeconds;
  private final long refreshTokenValiditySeconds;

  public JwtTokenProvider(
      @Value("${security.jwt.secret}") String secret,
      @Value("${security.jwt.issuer}") String issuer,
      @Value("${security.jwt.access-token-validity-seconds}") long accessTokenValiditySeconds,
      @Value("${security.jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds
  ) {
    this.secretKey = Base64.getDecoder().decode(secret);

    if (secretKey.length < 32) {
      throw new IllegalArgumentException("JWT 비밀키는 디코딩 후 32바이트 이상이어야 합니다.");
    }

    if (accessTokenValiditySeconds <=0 || refreshTokenValiditySeconds <= 0) {
      throw new IllegalArgumentException("JWT 유효 기간은 0초보다 커야 합니다.");
    }

    this.issuer = issuer;
    this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
  }

  public String generateAccessToken(UUID userId) {
    return generateToken(userId, "access", accessTokenValiditySeconds);
  }

  public String generateRefreshToken(UUID userId) {
    return generateToken(userId, "refresh", refreshTokenValiditySeconds);
  }

  private String generateToken(
      UUID userId,
      String tokenType,
      long validitySeconds
  ) {
    Instant now = Instant.now();

    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .subject(userId.toString())
        .issuer(issuer)
        .issueTime(Date.from(now))
        .expirationTime(Date.from(now.plusSeconds(validitySeconds)))
        .jwtID(UUID.randomUUID().toString())
        .claim("tokenType", tokenType)
        .build();

    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
        .type(JOSEObjectType.JWT)
        .build();

    SignedJWT signedJWT = new SignedJWT(header, claims);

    try {
      signedJWT.sign(new MACSigner(secretKey));
      return signedJWT.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("JWT 발급에 실패했습니다.", e);
    }
  }

  public JWTClaimsSet validateAccessToken(String token) {
    return validateToken(token, "access");
  }

  public JWTClaimsSet validateRefreshToken(String token) {
    return validateToken(token, "refresh");
  }

  private JWTClaimsSet validateToken(String token, String expectedTokenType) {
    if (token == null || token.isBlank()) {
      throw new BadCredentialsException("토큰이 없습니다.");
    }

    try {
      SignedJWT signedJWT = SignedJWT.parse(token);

      // 서버에서 사용하는 서명 알고리즘만 허용한다.
      if (!JWSAlgorithm.HS256.equals(signedJWT.getHeader().getAlgorithm())) {
        throw new BadCredentialsException("유효하지 않은 토큰입니다.");
      }

      if (!signedJWT.verify(new MACVerifier(secretKey))) {
        throw new BadCredentialsException("유효하지 않은 토큰입니다.");
      }

      JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

      if (!issuer.equals(claims.getIssuer())) {
        throw new BadCredentialsException("유효하지 않은 토큰입니다.");
      }

      if (!expectedTokenType.equals(claims.getStringClaim("tokenType"))) {
        throw new BadCredentialsException("토큰 종류가 올바르지 않습니다.");
      }

      Instant now = Instant.now();
      Date expirationTime = claims.getExpirationTime();

      if (expirationTime == null || !now.isBefore(expirationTime.toInstant())) {
        throw new BadCredentialsException("만료되었거나 만료 정보가 없는 토큰입니다.");
      }

      Date notBeforeTime = claims.getNotBeforeTime();

      if (notBeforeTime != null && now.isBefore(notBeforeTime.toInstant())) {
        throw new BadCredentialsException("필수 정보가 없는 토큰입니다.");
      }

      // 사용자 ID가 UUID 형식인지 확인한다.
      UUID.fromString(claims.getSubject());

      return claims;
    } catch (ParseException | JOSEException | IllegalArgumentException e) {
      throw new BadCredentialsException("유효하지 않은 토큰입니다.", e);
    }
  }


}
