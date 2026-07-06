package com.aliozcan.airportops.iam_service.auth.session;

import com.aliozcan.airportops.iam_service.auth.InvalidLoginException;
import com.aliozcan.airportops.iam_service.config.KeycloakSessionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakRestSessionClientTests {

    private static final String TOKEN_ENDPOINT =
            "http://keycloak.test/realms/airport-ops/protocol/openid-connect/token";

    private MockRestServiceServer server;
    private KeycloakRestSessionClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KeycloakRestSessionClient(
                new KeycloakSessionProperties(
                        "http://keycloak.test",
                        "airport-ops",
                        "iam-service-session",
                        "client-secret"),
                builder);
    }

    @Test
    void exchangesCredentialsWithConfidentialClient() {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("grant_type=password")))
                .andExpect(content().string(containsString("client_id=iam-service-session")))
                .andExpect(content().string(containsString("client_secret=client-secret")))
                .andExpect(content().string(containsString("username=user%40demo.test")))
                .andRespond(withSuccess(
                        """
                                {
                                  "access_token": "access-token",
                                  "refresh_token": "refresh-token",
                                  "expires_in": 300,
                                  "refresh_expires_in": 1800
                                }
                                """,
                        MediaType.APPLICATION_JSON));

        KeycloakTokenResponse response = client.login("user@demo.test", "secret-password");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        server.verify();
    }

    @Test
    void mapsRejectedCredentialsToGenericLoginFailure() {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> client.login("user@demo.test", "wrong-password"))
                .isInstanceOf(InvalidLoginException.class);
        server.verify();
    }

    @Test
    void mapsRejectedRefreshTokenToExpiredSession() {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> client.refresh("expired-refresh"))
                .isInstanceOf(SessionExpiredException.class);
        server.verify();
    }
}
