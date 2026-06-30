package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.auth.dto.LoginRequest;
import com.aliozcan.airportops.iam_service.auth.dto.LoginResponse;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTests {

    private static final String PLATFORM_ADMIN_EMAIL = "platform.admin@demo.com";
    private static final String PLATFORM_ADMIN_PASSWORD = "Admin123!";
    private static final String PLATFORM_ADMIN_PASSWORD_HASH =
            "$2y$10$wQmzRWJ7omqLDAEjIRzvpejjJMyarzcdn79Y/U1b0QOyud4C1R9CG";
    private static final Set<String> PLATFORM_ADMIN_PERMISSIONS = Set.of(
            "platform:invitation:create",
            "tenant:read",
            "tenant:manage"
    );

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginSucceedsWithPlatformAdminCredentials() {
        ResponseEntity<LoginResponse> response = login(
                PLATFORM_ADMIN_EMAIL,
                PLATFORM_ADMIN_PASSWORD,
                LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertSuccessfulPlatformLoginResponse(response.getBody());
    }

    @Test
    void loginEmailIsCaseInsensitive() {
        ResponseEntity<LoginResponse> response = login(
                "PLATFORM.ADMIN@DEMO.COM",
                PLATFORM_ADMIN_PASSWORD,
                LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertSuccessfulPlatformLoginResponse(response.getBody());
    }

    @Test
    void loginFailsWithWrongPassword() {
        ResponseEntity<ErrorResponse> response = login(
                PLATFORM_ADMIN_EMAIL,
                "wrong-password",
                ErrorResponse.class);

        assertInvalidCredentialsResponse(response);
    }

    @Test
    void loginFailsWithUnknownEmail() {
        ResponseEntity<ErrorResponse> response = login(
                "missing.user@demo.com",
                PLATFORM_ADMIN_PASSWORD,
                ErrorResponse.class);

        assertInvalidCredentialsResponse(response);
    }

    @Test
    void successfulLoginDoesNotReturnTokenFields() {
        ResponseEntity<String> response = login(
                PLATFORM_ADMIN_EMAIL,
                PLATFORM_ADMIN_PASSWORD,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .doesNotContain("\"token\"")
                .doesNotContain("\"jwt\"")
                .doesNotContain("\"accessToken\"");
    }

    @Test
    void actuatorHealthRemainsUp() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void passwordEncoderMatchesSeededBcryptHash() {
        assertThat(passwordEncoder.matches(
                PLATFORM_ADMIN_PASSWORD,
                PLATFORM_ADMIN_PASSWORD_HASH)).isTrue();
    }

    private <T> ResponseEntity<T> login(String email, String password, Class<T> responseType) {
        return restTemplate.postForEntity(
                "/auth/login",
                new LoginRequest(email, password),
                responseType);
    }

    private void assertSuccessfulPlatformLoginResponse(LoginResponse response) {
        assertThat(response).isNotNull();
        assertThat(response.userId()).isNotNull();
        assertThat(response.email()).isEqualTo(PLATFORM_ADMIN_EMAIL);
        assertThat(response.fullName()).isEqualTo("Platform Admin");
        assertThat(response.tokenScope()).isEqualTo("PLATFORM");
        assertThat(response.roles()).containsExactly("PLATFORM_ADMIN");
        assertThat(response.permissions()).containsExactly(
                "platform:invitation:create",
                "tenant:manage",
                "tenant:read"
        );
        assertThat(response.permissions())
                .containsExactlyInAnyOrderElementsOf(PLATFORM_ADMIN_PERMISSIONS);
    }

    private void assertInvalidCredentialsResponse(ResponseEntity<ErrorResponse> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getBody().error()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().errorCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().message()).isEqualTo("Invalid email or password");
        assertThat(response.getBody().path()).isEqualTo("/auth/login");
    }
}
