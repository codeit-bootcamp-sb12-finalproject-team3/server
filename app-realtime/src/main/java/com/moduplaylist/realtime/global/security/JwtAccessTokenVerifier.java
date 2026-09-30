package com.moduplaylist.realtime.global.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenVerifier {

    private final byte[] secretKey;
    private final String issuer;

    public JwtAccessTokenVerifier(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer}") String issuer
    ) {
        this.secretKey = Base64.getDecoder().decode(secret);
        if (secretKey.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes after decoding.");
        }
        this.issuer = issuer;
    }

    public VerifiedAccessToken verify(String token) {
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Access token is required.");
        }

        try {
            SignedJWT signedJwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(signedJwt.getHeader().getAlgorithm())
                    || !signedJwt.verify(new MACVerifier(secretKey))) {
                throw new BadCredentialsException("Invalid access token.");
            }

            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            validateClaims(claims);

            return new VerifiedAccessToken(
                    UUID.fromString(claims.getSubject()),
                    claims.getJWTID()
            );
        } catch (ParseException | JOSEException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid access token.", exception);
        }
    }

    private void validateClaims(JWTClaimsSet claims) throws ParseException {
        Instant now = Instant.now();
        Date expirationTime = claims.getExpirationTime();
        Date notBeforeTime = claims.getNotBeforeTime();

        if (!issuer.equals(claims.getIssuer())
                || !"access".equals(claims.getStringClaim("tokenType"))
                || claims.getSubject() == null
                || claims.getSubject().isBlank()
                || claims.getJWTID() == null
                || claims.getJWTID().isBlank()
                || expirationTime == null
                || !now.isBefore(expirationTime.toInstant())
                || (notBeforeTime != null && now.isBefore(notBeforeTime.toInstant()))) {
            throw new BadCredentialsException("Invalid access token claims.");
        }
    }
}
