package com.aliozcan.airportops.flight_service.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@TestConfiguration
public class MockAirportServiceConfig {

    @Bean
    public RestClient.Builder airportServiceRestClientTestBuilder() {
        return RestClient.builder().baseUrl("http://mock-airport-service");
    }

    @Bean
    public MockRestServiceServer mockAirportServiceServer() {
        return MockRestServiceServer.bindTo(airportServiceRestClientTestBuilder()).build();
    }

    @Bean
    @Primary
    public RestClient testAirportServiceRestClient() {
        mockAirportServiceServer();
        return airportServiceRestClientTestBuilder().build();
    }
}
