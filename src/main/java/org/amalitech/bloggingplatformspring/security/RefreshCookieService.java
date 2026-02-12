package org.amalitech.bloggingplatformspring.security;

import jakarta.servlet.http.HttpServletResponse;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class RefreshCookieService {

    @Value("${app.cookie-secure}")
    private boolean cookieSecure;

    public void setRefreshTokenCookie(String refreshToken, HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie
                .from(Constants.REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(cookieSecure)
                .maxAge(3600)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    }

}