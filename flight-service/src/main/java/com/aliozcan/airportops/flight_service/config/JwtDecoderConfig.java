package com.aliozcan.airportops.flight_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;

@Configuration
@EnableConfigurationProperties(IamJwtProperties.class)
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(IamJwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwksUri()).build();

        OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> issuerValidator =
                new JwtClaimValidator<>("iss", properties.issuer()::equals);
        OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> audienceValidator =
                new JwtClaimValidator<Object>("aud", audience ->
                        audience instanceof List<?> values && values.contains(properties.audience()));

        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtTimestampValidator(), issuerValidator, audienceValidator));
        return decoder;
    }
}
