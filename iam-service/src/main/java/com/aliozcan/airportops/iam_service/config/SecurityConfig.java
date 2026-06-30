package com.aliozcan.airportops.iam_service.config;

import com.aliozcan.airportops.iam_service.security.IamAccessDeniedHandler;
import com.aliozcan.airportops.iam_service.security.IamJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final IamJwtAuthenticationConverter iamJwtAuthenticationConverter;
    private final IamAccessDeniedHandler iamAccessDeniedHandler;

    public SecurityConfig(
            IamJwtAuthenticationConverter iamJwtAuthenticationConverter,
            IamAccessDeniedHandler iamAccessDeniedHandler) {
        this.iamJwtAuthenticationConverter = iamJwtAuthenticationConverter;
        this.iamAccessDeniedHandler = iamAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/keycloak/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/platform/authorization/probe")
                                .authenticated()
                        .anyRequest().denyAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(iamAccessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(iamJwtAuthenticationConverter)))
                .build();
    }
}
