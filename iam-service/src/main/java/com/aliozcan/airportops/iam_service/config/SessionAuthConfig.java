package com.aliozcan.airportops.iam_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        KeycloakSessionProperties.class,
        SessionCookieProperties.class
})
public class SessionAuthConfig {
}
