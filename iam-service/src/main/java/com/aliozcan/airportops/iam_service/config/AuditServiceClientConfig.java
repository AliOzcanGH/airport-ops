package com.aliozcan.airportops.iam_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({AuditServiceProperties.class, InternalServiceSecretProperties.class})
public class AuditServiceClientConfig {

    @Bean
    public RestClient auditServiceRestClient(AuditServiceProperties properties) {
        return RestClient.builder().baseUrl(properties.baseUrl()).build();
    }
}
