package org.amalitech.bloggingplatformspring.config;

import lombok.RequiredArgsConstructor;
import org.amalitech.bloggingplatformspring.security.JwtAccessDeniedHandler;
import org.amalitech.bloggingplatformspring.security.JwtAuthenticationEntryPoint;
import org.amalitech.bloggingplatformspring.security.JwtAuthenticationFilter;
import org.amalitech.bloggingplatformspring.security.oauth.CustomOAuth2UserService;
import org.amalitech.bloggingplatformspring.security.oauth.OAuth2AuthenticationFailureHandler;
import org.amalitech.bloggingplatformspring.security.oauth.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String AUTHOR_ROLE = "AUTHOR";
    private static final String POSTS_PATH = "/api/posts/**";
    private static final String TAGS_PATH = "/api/tags/**";
    private static final String FEED_PATH = "/api/feed/**";
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/graphql", "/graphiql",
            "/favicon.ico",
            "/actuator/health",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/error"
    };
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, POSTS_PATH, TAGS_PATH).permitAll()
                        .requestMatchers(FEED_PATH).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/posts").hasRole(AUTHOR_ROLE)
                        .requestMatchers(HttpMethod.PUT, POSTS_PATH).hasRole(AUTHOR_ROLE)
                        .requestMatchers(HttpMethod.DELETE, POSTS_PATH).hasAnyRole(AUTHOR_ROLE, ADMIN_ROLE)

                        .requestMatchers(HttpMethod.POST, "/api/tags").hasAnyRole(AUTHOR_ROLE, ADMIN_ROLE)
                        .requestMatchers(HttpMethod.PUT, TAGS_PATH).hasAnyRole(AUTHOR_ROLE, ADMIN_ROLE)
                        .requestMatchers(HttpMethod.DELETE, TAGS_PATH).hasAnyRole(AUTHOR_ROLE, ADMIN_ROLE)

                        .requestMatchers("/api/users/profile").authenticated()

                        .requestMatchers(
                                "/api/admin/**",
                                "/api/users/**",
                                "/api/metrics/performance/**",
                                "/api/security/audit/**",
                                "/actuator/**"
                        ).hasRole(ADMIN_ROLE)

                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:3001"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}