package org.amalitech.bloggingplatformspring.security.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.utils.UserUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserUtils userUtils;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {
        log.info("OAuthUserService Entry");

        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId =
                userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId,
                oauth2User.getAttributes()
        );

        if (userInfo.getEmail() == null) {
            throw new OAuth2AuthenticationException(
                    "Email not found from OAuth2 provider");
        }

        User user = userUtils.saveOrUpdateOAuthUser(userInfo, registrationId);
        List<GrantedAuthority> authorities = userUtils.getUserAuthorities(user);

        return new DefaultOAuth2User(
                authorities,
                oauth2User.getAttributes(),
                "email"
        );
    }
}