package com.aliozcan.airportops.iam_service.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@TestConfiguration
public class MockReportServiceConfig {

    @Bean
    public RestClient.Builder reportServiceRestClientTestBuilder() {
        return RestClient.builder().baseUrl("http://mock-report-service");
    }

    @Bean
    public MockRestServiceServer mockReportServiceServer() {
        return MockRestServiceServer.bindTo(reportServiceRestClientTestBuilder()).build();
    }

    @Bean
    @Primary
    public RestClient testReportServiceRestClient() {
        mockReportServiceServer();
        return reportServiceRestClientTestBuilder().build();
    }
}
