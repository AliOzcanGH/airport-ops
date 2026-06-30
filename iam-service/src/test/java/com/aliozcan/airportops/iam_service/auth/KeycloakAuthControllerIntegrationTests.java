package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.KeycloakMeResponse;
import com.aliozcan.airportops.iam_service.auth.dto.LoginRequest;
import com.aliozcan.airportops.iam_service.auth.dto.LoginResponse;
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
class KeycloakAuthControllerIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returnsKeycloakClaimsForValidBearerToken() {
        ResponseEntity<KeycloakMeResponse> response = restTemplate.exchange(
                "/auth/keycloak/me",
                HttpMethod.GET,
                bearerTokenRequest(TestJwtDecoderConfig.VALID_TOKEN),
                KeycloakMeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().subject()).isEqualTo("keycloak-platform-admin-id");
        assertThat(response.getBody().email()).isEqualTo(TestJwtDecoderConfig.EMAIL);
        assertThat(response.getBody().preferredUsername())
                .isEqualTo(TestJwtDecoderConfig.EMAIL);
        assertThat(response.getBody().issuer()).isEqualTo(TestJwtDecoderConfig.ISSUER);
        assertThat(response.getBody().roles()).contains("PLATFORM_ADMIN");
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/auth/keycloak/me", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsInvalidBearerToken() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/auth/keycloak/me",
                HttpMethod.GET,
                bearerTokenRequest(TestJwtDecoderConfig.INVALID_TOKEN),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void actuatorHealthRemainsPublic() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void customLoginRemainsPublic() {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/auth/login",
                new LoginRequest("platform.admin@demo.com", "Admin123!"),
                LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("platform.admin@demo.com");
    }

    private HttpEntity<Void> bearerTokenRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
