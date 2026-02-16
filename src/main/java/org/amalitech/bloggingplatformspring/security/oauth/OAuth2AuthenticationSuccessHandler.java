package org.amalitech.bloggingplatformspring.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.security.JwtTokenProvider;
import org.amalitech.bloggingplatformspring.security.RefreshCookieService;
import org.amalitech.bloggingplatformspring.services.UserService;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshCookieService refreshCookieService;
    private final UserUtils userUtils;
    private final UserService userService;

    @Value("${oauth.frontend.redirect.callback.url}")
    private String callbackURL;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        logger.info("OAuth Success Handler Entry");
        if (response.isCommitted()) {
            return;
        }

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        User savedUser = userService.processOAuth2User(principal);
        List<GrantedAuthority> authorities = userUtils.getUserAuthorities(savedUser);

        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        savedUser.getEmail(),
                        "",
                        authorities
                );

        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        String accessToken = jwtTokenProvider.createAccessToken(newAuth);
        String refreshToken = jwtTokenProvider.createRefreshToken(newAuth);

        refreshCookieService.setRefreshTokenCookie(refreshToken, response);
        String frontendUrl = String.format("%s?token=%s", callbackURL, accessToken);
        response.sendRedirect(frontendUrl);

    }
}