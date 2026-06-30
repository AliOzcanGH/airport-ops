package com.aliozcan.airportops.iam_service.platform;

import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.platform.dto.AuthorizationProbeResponse;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
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
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlatformAuthorizationProbeIntegrationTests {

    private static final String PROBE_PATH = "/platform/authorization/probe";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void grantsAccessWhenIamPermissionExists() {
        ResponseEntity<AuthorizationProbeResponse> response = callProbe(
                TestJwtDecoderConfig.VALID_TOKEN,
                AuthorizationProbeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Permission granted");
        assertThat(response.getBody().requiredPermission())
                .isEqualTo("platform:invitation:create");
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(PROBE_PATH, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsInvalidBearerToken() {
        ResponseEntity<String> response = callProbe(
                TestJwtDecoderConfig.INVALID_TOKEN,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsUnprovisionedUser() {
        ResponseEntity<ErrorResponse> response = callProbe(
                TestJwtDecoderConfig.UNPROVISIONED_TOKEN,
                ErrorResponse.class);

        assertForbidden(response, "USER_NOT_PROVISIONED");
        assertThat(response.getBody().message())
                .isEqualTo("Authenticated user is not provisioned in IAM");
    }

    @Test
    @Sql(statements = {
            "DELETE FROM iam.platform_user_roles WHERE user_id IN "
                    + "(SELECT id FROM iam.users WHERE lower(email) = "
                    + "lower('k4.permissionless@integration.test'))",
            "DELETE FROM iam.users WHERE lower(email) = "
                    + "lower('k4.permissionless@integration.test')",
            "INSERT INTO iam.users (email, password_hash, full_name, status) VALUES "
                    + "('k4.permissionless@integration.test', 'not-used', "
                    + "'K4 Permissionless Test User', 'ACTIVE')"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = {
            "DELETE FROM iam.platform_user_roles WHERE user_id IN "
                    + "(SELECT id FROM iam.users WHERE lower(email) = "
                    + "lower('k4.permissionless@integration.test'))",
            "DELETE FROM iam.users WHERE lower(email) = "
                    + "lower('k4.permissionless@integration.test')"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void rejectsProvisionedUserWithoutPermission() {
        ResponseEntity<ErrorResponse> response = callProbe(
                TestJwtDecoderConfig.PERMISSIONLESS_TOKEN,
                ErrorResponse.class);

        assertForbidden(response, "MISSING_PERMISSION");
        assertThat(response.getBody().message())
                .isEqualTo("Authenticated user does not have the required permission");
    }

    private <T> ResponseEntity<T> callProbe(String token, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                PROBE_PATH,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                responseType);
    }

    private void assertForbidden(
            ResponseEntity<ErrorResponse> response,
            String errorCode) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getBody().error()).isEqualTo("FORBIDDEN");
        assertThat(response.getBody().errorCode()).isEqualTo(errorCode);
        assertThat(response.getBody().path()).isEqualTo(PROBE_PATH);
    }
}
