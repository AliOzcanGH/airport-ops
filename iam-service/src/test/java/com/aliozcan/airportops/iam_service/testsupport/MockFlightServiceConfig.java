package com.aliozcan.airportops.iam_service.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@TestConfiguration
public class MockFlightServiceConfig {

    @Bean
    public RestClient.Builder flightServiceRestClientTestBuilder() {
        return RestClient.builder().baseUrl("http://mock-flight-service");
    }

    @Bean
    public MockRestServiceServer mockFlightServiceServer() {
        return MockRestServiceServer.bindTo(flightServiceRestClientTestBuilder()).build();
    }

    @Bean
    @Primary
    public RestClient testFlightServiceRestClient() {
        mockFlightServiceServer();
        return flightServiceRestClientTestBuilder().build();
    }
}
