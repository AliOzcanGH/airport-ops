package com.aliozcan.airportops.iam_service.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@TestConfiguration
public class MockAuditServiceConfig {

    @Bean
    public RestClient.Builder auditServiceRestClientTestBuilder() {
        return RestClient.builder().baseUrl("http://mock-audit-service");
    }

    @Bean
    public MockRestServiceServer mockAuditServiceServer() {
        return MockRestServiceServer.bindTo(auditServiceRestClientTestBuilder()).build();
    }

    @Bean
    @Primary
    public RestClient testAuditServiceRestClient() {
        mockAuditServiceServer();
        return auditServiceRestClientTestBuilder().build();
    }
}
