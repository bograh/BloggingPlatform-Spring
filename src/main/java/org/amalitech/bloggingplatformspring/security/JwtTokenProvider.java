package org.amalitech.bloggingplatformspring.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtTokenProvider {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessExpiration;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshExpiration;

    public JwtTokenProvider(
            @Value("${jwt.access-token-secret}") String accessSecret,
            @Value("${jwt.refresh-token-secret}") String refreshSecret) {

        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes());
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes());
    }

    public String createAccessToken(Authentication authentication) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpiration);

        return Jwts.builder()
                .subject(authentication.getName())
                .issuedAt(now)
                .expiration(expiry)
                .id(UUID.randomUUID().toString())
                .claim("roles",
                        authentication.getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList())
                .signWith(accessKey)
                .compact();
    }

    public String createRefreshToken(Authentication authentication) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpiration);
        String email = authentication.getName();

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .id(UUID.randomUUID().toString())
                .claim("roles",
                        authentication.getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList())
                .signWith(refreshKey)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return parse(token, accessKey);
    }

    public Claims parseRefreshToken(String token) {
        return parse(token, refreshKey);
    }

    private Claims parse(String token, SecretKey key) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public String getTokenFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        return null;
    }
}