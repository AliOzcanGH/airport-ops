package com.aliozcan.airportops.report_service.config;

import com.aliozcan.airportops.report_service.security.IamJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final IamJwtAuthenticationConverter iamJwtAuthenticationConverter;
    private final InternalServiceSecretFilter internalServiceSecretFilter;

    public SecurityConfig(
            IamJwtAuthenticationConverter iamJwtAuthenticationConverter,
            InternalServiceSecretFilter internalServiceSecretFilter) {
        this.iamJwtAuthenticationConverter = iamJwtAuthenticationConverter;
        this.internalServiceSecretFilter = internalServiceSecretFilter;
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
                        .requestMatchers(HttpMethod.GET, "/organizations/*/reports/daily-flights")
                                .authenticated()
                        .requestMatchers(HttpMethod.GET, "/organizations/*/reports/gate-utilization")
                                .authenticated()
                        // Guarded by InternalServiceSecretFilter instead of a JWT — no user session.
                        .requestMatchers(HttpMethod.GET, "/internal/organizations/*/operational-summary")
                                .permitAll()
                        .anyRequest().denyAll())
                .addFilterBefore(internalServiceSecretFilter, BasicAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(iamJwtAuthenticationConverter)))
                .build();
    }
}
