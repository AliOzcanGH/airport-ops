package com.aliozcan.airportops.flight_service.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@TestConfiguration
public class TestIamJwtDecoderConfig {

    public static final UUID ORG_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID ORG_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    public static final String ADMIN_TOKEN = "admin-token";
    public static final String VIEWER_TOKEN = "viewer-token";
    public static final String OTHER_ORG_ADMIN_TOKEN = "other-org-admin-token";
    public static final String INVALID_TOKEN = "invalid-token";

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        return token -> switch (token) {
            case ADMIN_TOKEN -> jwt(token, ORG_A, List.of("AIRLINE_ADMIN"),
                    List.of("flight:create", "flight:read"));
            case VIEWER_TOKEN -> jwt(token, ORG_A, List.of("VIEWER"), List.of("flight:read"));
            case OTHER_ORG_ADMIN_TOKEN -> jwt(token, ORG_B, List.of("AIRLINE_ADMIN"),
                    List.of("flight:create", "flight:read"));
            default -> throw new BadJwtException("Invalid test token");
        };
    }

    private Jwt jwt(String token, UUID organizationId, List<String> roles, List<String> permissions) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue(token)
                .headers(headers -> headers.put("alg", "RS256"))
                .issuer("airport-ops-iam")
                .subject(UUID.randomUUID().toString())
                .audience(List.of("airport-service", "flight-service"))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(900))
                .claim("workspace", "TENANT")
                .claim("organizationId", organizationId.toString())
                .claim("organizationStatus", "ACTIVE")
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("tokenScope", "TENANT_APP")
                .build();
    }
}
