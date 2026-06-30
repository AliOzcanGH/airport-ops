package com.aliozcan.airportops.iam_service.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class TestJwtDecoderConfig {

    public static final String VALID_TOKEN = "valid-token";
    public static final String WHITESPACE_EMAIL_TOKEN = "whitespace-email-token";
    public static final String UNPROVISIONED_TOKEN = "unprovisioned-token";
    public static final String PERMISSIONLESS_TOKEN = "permissionless-token";
    public static final String MISSING_EMAIL_TOKEN = "missing-email-token";
    public static final String BLANK_EMAIL_TOKEN = "blank-email-token";
    public static final String INVALID_TOKEN = "invalid-token";
    public static final String ISSUER = "http://127.0.0.1:8085/realms/airport-ops";
    public static final String EMAIL = "platform.admin@demo.com";
    public static final String PERMISSIONLESS_EMAIL = "k4.permissionless@integration.test";

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> {
            return switch (token) {
                case VALID_TOKEN -> jwt(token, EMAIL, true);
                case WHITESPACE_EMAIL_TOKEN -> jwt(token, "  " + EMAIL + "  ", true);
                case UNPROVISIONED_TOKEN -> jwt(token, "not.provisioned@demo.com", true);
                case PERMISSIONLESS_TOKEN -> jwt(token, PERMISSIONLESS_EMAIL, true);
                case MISSING_EMAIL_TOKEN -> jwt(token, null, false);
                case BLANK_EMAIL_TOKEN -> jwt(token, "   ", true);
                default -> throw new BadJwtException("Invalid test token");
            };
        };
    }

    private Jwt jwt(String token, String email, boolean includeEmail) {
        Instant issuedAt = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue(token)
                .headers(headers -> headers.put("alg", "RS256"))
                .issuer(URI.create(ISSUER).toString())
                .subject("keycloak-platform-admin-id")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim("preferred_username", EMAIL)
                .claim("realm_access", Map.of(
                        "roles",
                        List.of("default-roles-airport-ops", "PLATFORM_ADMIN")
                ));

        if (includeEmail) {
            builder.claim("email", email);
        }
        return builder.build();
    }
}
