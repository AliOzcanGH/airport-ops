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
    public static final String K8_ACTIVE_TOKEN = "k8-active-token";
    public static final String K8_PROVISIONING_TOKEN = "k8-provisioning-token";
    public static final String K8_SYNC_FAILED_TOKEN = "k8-sync-failed-token";
    public static final String K8_INACTIVE_TOKEN = "k8-inactive-token";
    public static final String TENANT_TOKEN = "tenant-token";
    public static final String DUAL_WORKSPACE_TOKEN = "dual-workspace-token";
    public static final String NO_WORKSPACE_TOKEN = "no-workspace-token";
    public static final String INACTIVE_ORGANIZATION_TOKEN =
            "inactive-organization-token";
    public static final String W5A_ONBOARDING_TOKEN = "w5a-onboarding-token";
    public static final String W5A_ACTIVE_TOKEN = "w5a-active-token";
    public static final String W5A_INACTIVE_ORG_TOKEN = "w5a-inactive-org-token";
    public static final String W5A_INACTIVE_MEMBER_TOKEN =
            "w5a-inactive-member-token";
    public static final String W5D_ADMIN_TOKEN = "w5d-admin-token";
    public static final String W5D_MEMBER_TOKEN = "w5d-member-token";
    public static final String W5D_ACTIVE_ADMIN_TOKEN = "w5d-active-admin-token";
    public static final String W5E_ADMIN_TOKEN = "w5e-admin-token";
    public static final String W5E_MEMBER_TOKEN = "w5e-member-token";
    public static final String W5E_ACTIVE_ADMIN_TOKEN = "w5e-active-admin-token";
    public static final String W6_ONBOARDING_TOKEN = "w6-onboarding-token";
    public static final String W6_ACTIVE_TOKEN = "w6-active-token";
    public static final String W6_INACTIVE_ORG_TOKEN = "w6-inactive-org-token";
    public static final String W6_INACTIVE_MEMBER_TOKEN = "w6-inactive-member-token";
    public static final String W7_ADMIN_TOKEN = "w7-admin-token";
    public static final String W7_OPS_TOKEN = "w7-ops-token";
    public static final String W7_OTHER_ADMIN_TOKEN = "w7-other-admin-token";
    public static final String W8A_TENANT_TOKEN = "w8a-tenant-token";
    public static final String W8A_NO_WORKSPACE_TOKEN = "w8a-no-workspace-token";
    public static final String W8B_TENANT_TOKEN = "w8b-tenant-token";
    public static final String W9_TENANT_TOKEN = "w9-tenant-token";
    public static final String W10_TENANT_TOKEN = "w10-tenant-token";
    public static final String W14_ADMIN_TOKEN = "w14-admin-token";
    public static final String W14_OPS_TOKEN = "w14-ops-token";
    public static final String W14_OTHER_ADMIN_TOKEN = "w14-other-admin-token";
    public static final String W14_PROXY_TENANT_TOKEN = "w14-proxy-tenant-token";
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
                case K8_ACTIVE_TOKEN -> jwt(token, "active@k8.auth.test", true);
                case K8_PROVISIONING_TOKEN ->
                        jwt(token, "provisioning@k8.auth.test", true);
                case K8_SYNC_FAILED_TOKEN ->
                        jwt(token, "failed@k8.auth.test", true);
                case K8_INACTIVE_TOKEN -> jwt(token, "inactive@k8.auth.test", true);
                case TENANT_TOKEN -> jwt(token, "tenant@w2a.test", true);
                case DUAL_WORKSPACE_TOKEN -> jwt(token, "dual@w2a.test", true);
                case NO_WORKSPACE_TOKEN -> jwt(token, "none@w2a.test", true);
                case INACTIVE_ORGANIZATION_TOKEN ->
                        jwt(token, "inactive-org@w2a.test", true);
                case W5A_ONBOARDING_TOKEN -> jwt(token, "onboarding@w5a.test", true);
                case W5A_ACTIVE_TOKEN -> jwt(token, "active@w5a.test", true);
                case W5A_INACTIVE_ORG_TOKEN ->
                        jwt(token, "inactive-org@w5a.test", true);
                case W5A_INACTIVE_MEMBER_TOKEN ->
                        jwt(token, "inactive-member@w5a.test", true);
                case W5D_ADMIN_TOKEN -> jwt(token, "admin@w5d.test", true);
                case W5D_MEMBER_TOKEN -> jwt(token, "member@w5d.test", true);
                case W5D_ACTIVE_ADMIN_TOKEN ->
                        jwt(token, "active-admin@w5d.test", true);
                case W5E_ADMIN_TOKEN -> jwt(token, "admin@w5e.test", true);
                case W5E_MEMBER_TOKEN -> jwt(token, "member@w5e.test", true);
                case W5E_ACTIVE_ADMIN_TOKEN ->
                        jwt(token, "active-admin@w5e.test", true);
                case W6_ONBOARDING_TOKEN -> jwt(token, "onboarding@w6.test", true);
                case W6_ACTIVE_TOKEN -> jwt(token, "active@w6.test", true);
                case W6_INACTIVE_ORG_TOKEN ->
                        jwt(token, "inactive-org@w6.test", true);
                case W6_INACTIVE_MEMBER_TOKEN ->
                        jwt(token, "inactive-member@w6.test", true);
                case W7_ADMIN_TOKEN -> jwt(token, "admin@w7.test", true);
                case W7_OPS_TOKEN -> jwt(token, "ops@w7.test", true);
                case W7_OTHER_ADMIN_TOKEN -> jwt(token, "other-admin@w7.test", true);
                case W8A_TENANT_TOKEN -> jwt(token, "tenant@w8a.test", true);
                case W8A_NO_WORKSPACE_TOKEN -> jwt(token, "none@w8a.test", true);
                case W8B_TENANT_TOKEN -> jwt(token, "tenant@w8b.test", true);
                case W9_TENANT_TOKEN -> jwt(token, "tenant@w9.test", true);
                case W10_TENANT_TOKEN -> jwt(token, "tenant@w10.test", true);
                case W14_ADMIN_TOKEN -> jwt(token, "admin@w14.test", true);
                case W14_OPS_TOKEN -> jwt(token, "ops@w14.test", true);
                case W14_OTHER_ADMIN_TOKEN -> jwt(token, "other-admin@w14.test", true);
                case W14_PROXY_TENANT_TOKEN -> jwt(token, "tenant@w14proxy.test", true);
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
