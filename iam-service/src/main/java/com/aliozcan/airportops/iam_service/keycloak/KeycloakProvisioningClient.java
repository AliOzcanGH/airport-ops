package com.aliozcan.airportops.iam_service.keycloak;

public interface KeycloakProvisioningClient {

    String createUser(String email, String password);
}
