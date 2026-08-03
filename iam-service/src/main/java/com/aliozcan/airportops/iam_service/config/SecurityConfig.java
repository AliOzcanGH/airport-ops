package com.aliozcan.airportops.iam_service.config;

import com.aliozcan.airportops.iam_service.security.IamAccessDeniedHandler;
import com.aliozcan.airportops.iam_service.security.IamJwtAuthenticationConverter;
import com.aliozcan.airportops.iam_service.security.HeaderOrCookieBearerTokenResolver;
import com.aliozcan.airportops.iam_service.security.SessionCsrfRequestMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final IamJwtAuthenticationConverter iamJwtAuthenticationConverter;
    private final IamAccessDeniedHandler iamAccessDeniedHandler;
    private final HeaderOrCookieBearerTokenResolver bearerTokenResolver;
    private final SessionCsrfRequestMatcher csrfRequestMatcher;
    private final SessionCookieProperties cookieProperties;

    public SecurityConfig(
            IamJwtAuthenticationConverter iamJwtAuthenticationConverter,
            IamAccessDeniedHandler iamAccessDeniedHandler,
            HeaderOrCookieBearerTokenResolver bearerTokenResolver,
            SessionCsrfRequestMatcher csrfRequestMatcher,
            SessionCookieProperties cookieProperties) {
        this.iamJwtAuthenticationConverter = iamJwtAuthenticationConverter;
        this.iamAccessDeniedHandler = iamAccessDeniedHandler;
        this.bearerTokenResolver = bearerTokenResolver;
        this.csrfRequestMatcher = csrfRequestMatcher;
        this.cookieProperties = cookieProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieCustomizer(builder ->
                builder.path("/")
                        .secure(cookieProperties.secure())
                        .sameSite(cookieProperties.sameSite()));
        CsrfTokenRequestAttributeHandler csrfRequestHandler =
                new CsrfTokenRequestAttributeHandler();

        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfRequestHandler)
                        .requireCsrfProtectionMatcher(csrfRequestMatcher))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/.well-known/jwks.json").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/iam-token").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/invitations/validate").permitAll()
                        .requestMatchers(HttpMethod.POST, "/invitations/accept").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/session/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/session/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/session/mfa/verify")
                                .permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/session/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/session/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/keycloak/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/app/setup/overview")
                                .authenticated()
                        .requestMatchers(HttpMethod.PUT, "/app/setup/profile")
                                .authenticated()
                        .requestMatchers(HttpMethod.POST, "/app/setup/complete")
                                .authenticated()
                        .requestMatchers(HttpMethod.GET, "/app/dashboard/overview")
                                .authenticated()
                        .requestMatchers(HttpMethod.GET, "/platform/authorization/probe")
                                .authenticated()
                        .requestMatchers(HttpMethod.GET, "/platform/tenants")
                                .authenticated()
                        .requestMatchers(HttpMethod.GET, "/platform/tenants/*")
                                .authenticated()
                        .requestMatchers(HttpMethod.POST, "/platform/invitations")
                                .authenticated()
                        .requestMatchers(HttpMethod.POST, "/organizations/*/invitations")
                                .authenticated()
                        .requestMatchers(HttpMethod.GET, "/organizations/*/members")
                                .authenticated()
                        .requestMatchers(HttpMethod.POST, "/app/stations")
                                .authenticated()
                        .requestMatchers(HttpMethod.GET, "/app/stations/*/gates")
                                .authenticated()
                        .requestMatchers(HttpMethod.POST, "/app/stations/*/gates")
                                .authenticated()
                        .requestMatchers(HttpMethod.PUT, "/app/stations/*/gates/*/status")
                                .authenticated()
                        .anyRequest().denyAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(iamAccessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver)
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(iamJwtAuthenticationConverter)))
                .build();
    }
}
