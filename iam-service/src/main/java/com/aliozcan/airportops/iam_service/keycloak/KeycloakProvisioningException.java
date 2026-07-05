package com.aliozcan.airportops.iam_service.keycloak;

public class KeycloakProvisioningException extends RuntimeException {

    private final boolean duplicateIdentity;

    public KeycloakProvisioningException(
            String message,
            boolean duplicateIdentity) {
        super(message);
        this.duplicateIdentity = duplicateIdentity;
    }

    public KeycloakProvisioningException(
            String message,
            Throwable cause) {
        super(message, cause);
        this.duplicateIdentity = false;
    }

    public boolean isDuplicateIdentity() {
        return duplicateIdentity;
    }
}
