package com.aliozcan.airportops.iam_service.auth.token;

import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.auth.dto.IamTokenResponse;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w8a.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w8a.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w8a.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W8A %'",
        "INSERT INTO iam.users (email, full_name, status, auth_provider) VALUES "
                + "('tenant@w8a.test', 'W8A Tenant User', 'ACTIVE', 'KEYCLOAK'), "
                + "('none@w8a.test', 'W8A No Workspace User', 'ACTIVE', 'KEYCLOAK')",
        "INSERT INTO iam.organizations (name, status) VALUES ('W8A Tenant Org', 'ACTIVE')",
        "INSERT INTO iam.organization_members (organization_id, user_id, status) "
                + "SELECT o.id, u.id, 'ACTIVE' FROM iam.users u "
                + "JOIN iam.organizations o ON u.email = 'tenant@w8a.test' "
                + "AND o.name = 'W8A Tenant Org'",
        "INSERT INTO iam.member_roles (member_id, role_id) "
                + "SELECT m.id, r.id FROM iam.organization_members m "
                + "JOIN iam.users u ON u.id = m.user_id "
                + "JOIN iam.roles r ON r.code = 'AIRLINE_ADMIN' "
                + "WHERE u.email = 'tenant@w8a.test'"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w8a.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w8a.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w8a.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W8A %'"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class IamTokenControllerIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void issuesPlatformWorkspaceTokenForPlatformAdmin() throws Exception {
        ResponseEntity<IamTokenResponse> response = postIamToken(
                TestJwtDecoderConfig.VALID_TOKEN, IamTokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().expiresIn()).isEqualTo(900L);

        SignedJWT signedJWT = SignedJWT.parse(response.getBody().iamAccessToken());
        assertSignatureValid(signedJWT);

        Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
        assertThat(claims.get("iss")).isEqualTo("airport-ops-iam");
        assertThat(claims.get("workspace")).isEqualTo("PLATFORM");
        assertThat(claims).containsKey("organizationId");
        assertThat(claims.get("organizationId")).isNull();
        assertThat(claims).containsKey("organizationStatus");
        assertThat(claims.get("organizationStatus")).isNull();
        assertThat(claims.get("tokenScope")).isEqualTo("PLATFORM_APP");
        assertThat(claims.get("email")).isEqualTo(TestJwtDecoderConfig.EMAIL);
        assertThat(claims.get("keycloakSub")).isEqualTo("keycloak-platform-admin-id");
        assertThat(stringList(claims, "roles")).containsExactly("PLATFORM_ADMIN");
        assertThat(stringList(claims, "permissions")).containsExactly(
                "platform:invitation:create", "tenant:manage", "tenant:read");
        assertThat(stringList(claims, "aud")).containsExactly("airport-service");
        assertThat(Duration.between(
                signedJWT.getJWTClaimsSet().getIssueTime().toInstant(),
                signedJWT.getJWTClaimsSet().getExpirationTime().toInstant()))
                .isEqualTo(Duration.ofSeconds(900));
    }

    @Test
    void issuesTenantWorkspaceTokenForTenantUser() throws Exception {
        ResponseEntity<IamTokenResponse> response = postIamToken(
                TestJwtDecoderConfig.W8A_TENANT_TOKEN, IamTokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SignedJWT signedJWT = SignedJWT.parse(response.getBody().iamAccessToken());
        assertSignatureValid(signedJWT);

        Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
        UUID expectedOrganizationId = jdbcTemplate.queryForObject(
                "SELECT id FROM iam.organizations WHERE name = 'W8A Tenant Org'",
                UUID.class);
        assertThat(claims.get("workspace")).isEqualTo("TENANT");
        assertThat(claims.get("organizationId"))
                .isEqualTo(expectedOrganizationId.toString());
        assertThat(claims.get("organizationStatus")).isEqualTo("ACTIVE");
        assertThat(claims.get("tokenScope")).isEqualTo("TENANT_APP");
        assertThat(stringList(claims, "roles")).containsExactly("AIRLINE_ADMIN");
        assertThat(stringList(claims, "permissions"))
                .contains("member:invite", "flight:create", "audit:read");
    }

    @Test
    void rejectsUserWithNoWorkspaceContext() {
        ResponseEntity<ErrorResponse> response = postIamToken(
                TestJwtDecoderConfig.W8A_NO_WORKSPACE_TOKEN, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("NO_WORKSPACE_CONTEXT");
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/iam-token", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsInvalidBearerToken() {
        ResponseEntity<String> response = postIamToken(
                TestJwtDecoderConfig.INVALID_TOKEN, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void jwksEndpointPublishesVerifyingKey() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/.well-known/jwks.json",
                HttpMethod.GET,
                null,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("keys");
        List<?> keys = (List<?>) body.get("keys");
        assertThat(keys).hasSize(1);
        Map<?, ?> key = (Map<?, ?>) keys.get(0);
        assertThat(key.get("kid")).isEqualTo("iam-key-1");
        assertThat(key.get("kty")).isEqualTo("RSA");
        assertThat(key.containsKey("d")).isFalse();
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Map<String, Object> claims, String key) {
        return (List<String>) claims.get(key);
    }

    private void assertSignatureValid(SignedJWT signedJWT) throws ParseException {
        ResponseEntity<String> jwksResponse = restTemplate.getForEntity(
                "/.well-known/jwks.json", String.class);
        try {
            JWKSet jwkSet = JWKSet.parse(jwksResponse.getBody());
            JWK jwk = jwkSet.getKeys().get(0);
            RSAKey rsaKey = jwk.toRSAKey();
            assertThat(signedJWT.verify(new RSASSAVerifier(rsaKey))).isTrue();
        } catch (Exception exception) {
            throw new AssertionError("Failed to verify IAM JWT signature", exception);
        }
    }

    private <T> ResponseEntity<T> postIamToken(String token, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                "/auth/iam-token",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                responseType);
    }
}
