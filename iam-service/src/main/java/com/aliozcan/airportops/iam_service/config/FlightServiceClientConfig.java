package com.aliozcan.airportops.iam_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FlightServiceProperties.class)
public class FlightServiceClientConfig {

    @Bean
    public RestClient flightServiceRestClient(FlightServiceProperties properties) {
        return RestClient.builder().baseUrl(properties.baseUrl()).build();
    }
}
