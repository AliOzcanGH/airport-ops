package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.AuthMeResponse;
import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.domain.model.enums.UserStatus;
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

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthMeControllerIntegrationTests {

    private static final String USER_NOT_PROVISIONED_MESSAGE =
            "Authenticated user is not provisioned in IAM";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returnsKeycloakIdentityAndIamAuthorizationForProvisionedUser() {
        ResponseEntity<AuthMeResponse> response = getMe(
                TestJwtDecoderConfig.VALID_TOKEN,
                AuthMeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().keycloakSubject())
                .isEqualTo("keycloak-platform-admin-id");
        assertThat(response.getBody().issuer()).isEqualTo(TestJwtDecoderConfig.ISSUER);
        assertThat(response.getBody().email()).isEqualTo(TestJwtDecoderConfig.EMAIL);
        assertThat(response.getBody().fullName()).isEqualTo("Platform Admin");
        assertThat(response.getBody().preferredUsername())
                .isEqualTo(TestJwtDecoderConfig.EMAIL);
        assertThat(response.getBody().iamUserId()).isNotNull();
        assertThat(response.getBody().iamUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.getBody().keycloakRoles()).contains("PLATFORM_ADMIN");
        assertThat(response.getBody().iamRoles()).containsExactly("PLATFORM_ADMIN");
        assertThat(response.getBody().permissions()).containsExactly(
                "platform:invitation:create",
                "tenant:manage",
                "tenant:read"
        );
    }

    @Test
    void trimsJwtEmailForLookupAndResponse() {
        ResponseEntity<AuthMeResponse> response = getMe(
                TestJwtDecoderConfig.WHITESPACE_EMAIL_TOKEN,
                AuthMeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo(TestJwtDecoderConfig.EMAIL);
        assertThat(response.getBody().fullName()).isEqualTo("Platform Admin");
        assertThat(response.getBody().iamUserId()).isNotNull();
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/auth/me", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsInvalidBearerToken() {
        ResponseEntity<String> response = getMe(
                TestJwtDecoderConfig.INVALID_TOKEN,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsUserMissingFromIam() {
        ResponseEntity<ErrorResponse> response = getMe(
                TestJwtDecoderConfig.UNPROVISIONED_TOKEN,
                ErrorResponse.class);

        assertUserNotProvisioned(response);
    }

    @Test
    void rejectsTokenWithoutEmailClaim() {
        ResponseEntity<ErrorResponse> response = getMe(
                TestJwtDecoderConfig.MISSING_EMAIL_TOKEN,
                ErrorResponse.class);

        assertUserNotProvisioned(response);
    }

    @Test
    void rejectsTokenWithBlankEmailClaim() {
        ResponseEntity<ErrorResponse> response = getMe(
                TestJwtDecoderConfig.BLANK_EMAIL_TOKEN,
                ErrorResponse.class);

        assertUserNotProvisioned(response);
    }

    @Test
    void existingKeycloakMeEndpointRemainsAvailable() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/auth/keycloak/me",
                HttpMethod.GET,
                bearerTokenRequest(TestJwtDecoderConfig.VALID_TOKEN),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("PLATFORM_ADMIN");
    }

    private <T> ResponseEntity<T> getMe(String token, Class<T> responseType) {
        return restTemplate.exchange(
                "/auth/me",
                HttpMethod.GET,
                bearerTokenRequest(token),
                responseType);
    }

    private HttpEntity<Void> bearerTokenRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private void assertUserNotProvisioned(ResponseEntity<ErrorResponse> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getBody().error()).isEqualTo("FORBIDDEN");
        assertThat(response.getBody().errorCode()).isEqualTo("USER_NOT_PROVISIONED");
        assertThat(response.getBody().message()).isEqualTo(USER_NOT_PROVISIONED_MESSAGE);
        assertThat(response.getBody().path()).isEqualTo("/auth/me");
    }
}
