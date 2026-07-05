package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class K8AuthGuardrailIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanUsers();
        insertUser("active@k8.auth.test", "ACTIVE");
        insertUser("provisioning@k8.auth.test", "PROVISIONING");
        insertUser("failed@k8.auth.test", "KEYCLOAK_SYNC_FAILED");
        insertUser("inactive@k8.auth.test", "INACTIVE");
    }

    @AfterEach
    void tearDown() {
        cleanUsers();
    }

    @Test
    void legacyLoginRejectsKeycloakUserWithNullPasswordHash() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/auth/login",
                Map.of(
                        "email", "active@k8.auth.test",
                        "password", "StrongPassword123!"),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void activePermissionlessUserIsProvisionedButHasNoAuthority() {
        ResponseEntity<ErrorResponse> response = probe(
                TestJwtDecoderConfig.K8_ACTIVE_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PERMISSION");
    }

    @Test
    void nonActiveUsersFailClosedAsUnprovisioned() {
        assertUnprovisioned(TestJwtDecoderConfig.K8_PROVISIONING_TOKEN);
        assertUnprovisioned(TestJwtDecoderConfig.K8_SYNC_FAILED_TOKEN);
        assertUnprovisioned(TestJwtDecoderConfig.K8_INACTIVE_TOKEN);
    }

    private ResponseEntity<ErrorResponse> probe(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                "/platform/authorization/probe",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ErrorResponse.class);
    }

    private void assertUnprovisioned(String token) {
        ResponseEntity<ErrorResponse> response = probe(token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("USER_NOT_PROVISIONED");
    }

    private void insertUser(String email, String status) {
        jdbcTemplate.update(
                """
                        INSERT INTO iam.users (
                            email, password_hash, full_name, status, auth_provider
                        ) VALUES (?, NULL, 'K8 Auth Test User', ?, 'KEYCLOAK')
                        """,
                email,
                status);
    }

    private void cleanUsers() {
        jdbcTemplate.update(
                "DELETE FROM iam.users WHERE email LIKE '%@k8.auth.test'");
    }
}
