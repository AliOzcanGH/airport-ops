package com.aliozcan.airportops.iam_service.auth.session;

public interface KeycloakSessionClient {

    KeycloakTokenResponse login(String email, String password);

    KeycloakTokenResponse refresh(String refreshToken);

    void logout(String refreshToken);
}
