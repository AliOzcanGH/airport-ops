package com.aliozcan.airportops.flight_service.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

/**
 * Points the airport-service client at a port nothing is listening on, so
 * calls fail fast with a real connection-refused error — used to prove
 * flight-service handles an unreachable airport-service explicitly instead
 * of silently assuming a gate is valid.
 */
@TestConfiguration
public class UnavailableAirportServiceConfig {

    @Bean
    @Primary
    public RestClient testUnavailableAirportServiceRestClient() {
        return RestClient.builder().baseUrl("http://127.0.0.1:19999").build();
    }
}
