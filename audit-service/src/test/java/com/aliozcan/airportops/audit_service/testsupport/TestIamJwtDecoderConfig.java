package com.aliozcan.airportops.audit_service.testsupport;

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

    public static final String ORG_A_ADMIN_TOKEN = "org-a-admin-token";
    public static final String ORG_B_ADMIN_TOKEN = "org-b-admin-token";
    public static final String ORG_A_VIEWER_TOKEN = "org-a-viewer-token";
    public static final String PLATFORM_ADMIN_TOKEN = "platform-admin-token";

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        return token -> switch (token) {
            case ORG_A_ADMIN_TOKEN -> tenantJwt(token, ORG_A, List.of("AIRLINE_ADMIN"), List.of("audit:read"));
            case ORG_B_ADMIN_TOKEN -> tenantJwt(token, ORG_B, List.of("AIRLINE_ADMIN"), List.of("audit:read"));
            case ORG_A_VIEWER_TOKEN -> tenantJwt(token, ORG_A, List.of("VIEWER"), List.of());
            case PLATFORM_ADMIN_TOKEN -> platformJwt(token);
            default -> throw new BadJwtException("Invalid test token");
        };
    }

    private Jwt tenantJwt(String token, UUID organizationId, List<String> roles, List<String> permissions) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue(token)
                .headers(headers -> headers.put("alg", "RS256"))
                .issuer("airport-ops-iam")
                .subject(UUID.randomUUID().toString())
                .audience(List.of("audit-service"))
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

    private Jwt platformJwt(String token) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue(token)
                .headers(headers -> headers.put("alg", "RS256"))
                .issuer("airport-ops-iam")
                .subject(UUID.randomUUID().toString())
                .audience(List.of("audit-service"))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(900))
                .claim("workspace", "PLATFORM")
                .claim("roles", List.of("PLATFORM_ADMIN"))
                .claim("permissions", List.of("tenant:read"))
                .claim("tokenScope", "PLATFORM_APP")
                .build();
    }
}
