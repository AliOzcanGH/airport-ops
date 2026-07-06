package com.aliozcan.airportops.iam_service.auth.session;

import com.aliozcan.airportops.iam_service.auth.InvalidLoginException;
import com.aliozcan.airportops.iam_service.config.KeycloakSessionProperties;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class KeycloakRestSessionClient implements KeycloakSessionClient {

    private final KeycloakSessionProperties properties;
    private final RestClient restClient;

    public KeycloakRestSessionClient(
            KeycloakSessionProperties properties,
            RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public KeycloakTokenResponse login(String email, String password) {
        MultiValueMap<String, String> form = clientCredentials();
        form.add("grant_type", "password");
        form.add("username", email);
        form.add("password", password);

        try {
            return requestTokens(properties.tokenEndpoint(), form);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)) {
                throw new InvalidLoginException();
            }
            throw new AuthProviderUnavailableException(exception);
        } catch (ResourceAccessException exception) {
            throw new AuthProviderUnavailableException(exception);
        }
    }

    @Override
    public KeycloakTokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = clientCredentials();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        try {
            return requestTokens(properties.tokenEndpoint(), form);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)) {
                throw new SessionExpiredException();
            }
            throw new AuthProviderUnavailableException(exception);
        } catch (ResourceAccessException exception) {
            throw new AuthProviderUnavailableException(exception);
        }
    }

    @Override
    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = clientCredentials();
        form.add("refresh_token", refreshToken);

        try {
            restClient.post()
                    .uri(properties.logoutEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException | ResourceAccessException exception) {
            throw new AuthProviderUnavailableException(exception);
        }
    }

    private KeycloakTokenResponse requestTokens(
            String endpoint,
            MultiValueMap<String, String> form) {
        KeycloakTokenResponse response = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KeycloakTokenResponse.class);
        if (response == null
                || response.accessToken() == null
                || response.refreshToken() == null) {
            throw new AuthProviderUnavailableException(
                    new IllegalStateException("Authentication provider returned no tokens"));
        }
        return response;
    }

    private MultiValueMap<String, String> clientCredentials() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        return form;
    }
}
