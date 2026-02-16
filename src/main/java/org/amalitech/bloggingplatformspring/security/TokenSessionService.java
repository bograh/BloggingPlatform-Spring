package org.amalitech.bloggingplatformspring.security;

import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.SessionInfo;
import org.amalitech.bloggingplatformspring.dtos.responses.SessionStats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TokenSessionService {

    private final JwtTokenProvider jwtTokenProvider;
    private final ConcurrentHashMap<String, String> revokedTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    public TokenSessionService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
        log.info("TokenSessionService initialized with cleanup task");
    }

    public void revokeToken(String token) {
        if (token != null && !token.isBlank()) {
            try {
                String email = jwtTokenProvider.getEmailFromAccessToken(token);
                revokedTokens.put(token, email);
                log.info("Token revoked for user: {}", email);
            } catch (Exception e) {
                log.error("Error while revoking token: {}", e.getMessage());
            }
        }
    }

    public boolean isTokenRevoked(String token) {
        return revokedTokens.containsKey(token);
    }

    public void createSession(String email, String accessToken, String refreshToken) {
        SessionInfo sessionInfo = SessionInfo.builder()
                .email(email)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .loginTimestamp(System.currentTimeMillis())
                .lastActivityTimestamp(System.currentTimeMillis())
                .build();

        activeSessions.put(email, sessionInfo);
        log.info("Session created for user: {}", email);
    }

    public void updateSessionActivity(String email) {
        SessionInfo sessionInfo = activeSessions.get(email);
        if (sessionInfo != null) {
            sessionInfo.setLastActivityTimestamp(System.currentTimeMillis());
        }
    }

    public void removeSession(String email) {
        SessionInfo sessionInfo = activeSessions.remove(email);
        if (sessionInfo != null) {
            if (sessionInfo.getAccessToken() != null) {
                revokedTokens.put(sessionInfo.getAccessToken(), email);
            }
            if (sessionInfo.getRefreshToken() != null) {
                revokedTokens.put(sessionInfo.getRefreshToken(), email);
            }
            log.info("Session removed for user: {}", email);
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredSessionsAndTokens() {
        long currentTime = System.currentTimeMillis();

        activeSessions.entrySet().removeIf(entry -> {
            SessionInfo sessionInfo = entry.getValue();
            long sessionAge = currentTime - sessionInfo.getLoginTimestamp();
            boolean isExpired = sessionAge > refreshTokenExpirationMs;

            if (isExpired) {
                log.info("Removing expired session for user: {}", entry.getKey());
            }
            return isExpired;
        });

        revokedTokens.entrySet().removeIf(entry -> {
            try {
                String token = entry.getKey();
                long expirationTime = jwtTokenProvider.getExpirationTimeFromAccessToken(token);
                boolean isExpired = currentTime > expirationTime;

                if (isExpired) {
                    log.debug("Removing expired revoked token");
                }
                return isExpired;
            } catch (Exception e) {
                return true;
            }
        });

        log.info("Cleanup completed. Active sessions: {}, Revoked tokens: {}",
                activeSessions.size(), revokedTokens.size());
    }

    public SessionStats getStatistics() {
        return new SessionStats(
                activeSessions.size(),
                revokedTokens.size()
        );
    }
}