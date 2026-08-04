package com.aliozcan.airportops.airport_service.config;

import com.aliozcan.airportops.airport_service.security.IamJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final IamJwtAuthenticationConverter iamJwtAuthenticationConverter;

    public SecurityConfig(IamJwtAuthenticationConverter iamJwtAuthenticationConverter) {
        this.iamJwtAuthenticationConverter = iamJwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/organizations/*/stations").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/organizations/*/stations").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/organizations/*/stations/*/gates").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/organizations/*/stations/*/gates/*").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/organizations/*/gates/*").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/organizations/*/stations/*/gates").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                "/organizations/*/stations/*/gates/*/status").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(iamJwtAuthenticationConverter)))
                .build();
    }
}
