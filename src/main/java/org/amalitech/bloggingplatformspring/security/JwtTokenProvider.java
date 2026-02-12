package org.amalitech.bloggingplatformspring.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class JwtTokenProvider {

    @Value("${jwt.access-token-secret}")
    private String accessTokenSecret;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-secret}")
    private String refreshTokenSecret;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String createAccessToken(Authentication authentication) {
        return createToken(authentication, accessTokenSecret, accessTokenExpirationMs);
    }

    public String createRefreshToken(Authentication authentication) {
        return createToken(authentication, refreshTokenSecret, refreshTokenExpirationMs);
    }

    public String getEmailFromAccessToken(String token) {
        return getEmailFromToken(token, accessTokenSecret);
    }

    public String getEmailFromRefreshToken(String token) {
        return getEmailFromToken(token, refreshTokenSecret);
    }

    public boolean validAccessToken(String authToken) {
        return validateToken(authToken, accessTokenSecret);
    }

    public boolean validRefreshToken(String authToken) {
        return validateToken(authToken, refreshTokenSecret);
    }

    public String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String createToken(Authentication authentication, String secret, long expirationMs) {
        String email = authentication.getName();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .issuedAt(now)
                .expiration(expiryDate)
                .subject(email)
                .id(String.valueOf(UUID.randomUUID()))
                .signWith(getSigningKey(secret))
                .compact();
    }

    private String getEmailFromToken(String token, String secret) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    private boolean validateToken(String authToken, String secret) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey(secret))
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (SecurityException | UnsupportedJwtException | IllegalArgumentException | MalformedJwtException |
                 ExpiredJwtException ex) {
            return false;
        }
    }

}