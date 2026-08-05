package org.amalitech.bloggingplatformspring.security;

import jakarta.servlet.http.HttpServletResponse;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RefreshCookieService {

    @Value("${app.cookie-secure}")
    private String cookieSecure;

    public void setRefreshTokenCookie(String refreshToken, HttpServletResponse response) {
        ResponseCookie cookie = buildCookie(refreshToken, Duration.ofDays(7));
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = buildCookie("", Duration.ZERO);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie
                .from(Constants.REFRESH_TOKEN_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(Boolean.parseBoolean(cookieSecure))
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
    }

}