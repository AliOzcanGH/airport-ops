package com.aliozcan.airportops.iam_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({ReportServiceProperties.class, InternalServiceSecretProperties.class})
public class ReportServiceClientConfig {

    @Bean
    public RestClient reportServiceRestClient(ReportServiceProperties properties) {
        return RestClient.builder().baseUrl(properties.baseUrl()).build();
    }
}
