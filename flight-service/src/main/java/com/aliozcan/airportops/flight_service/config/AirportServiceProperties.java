package com.aliozcan.airportops.flight_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.airport-service")
public record AirportServiceProperties(
        String baseUrl
) {
}
