package com.aliozcan.airportops.iam_service.auth.session;

public class AuthProviderUnavailableException extends RuntimeException {

    public AuthProviderUnavailableException(Throwable cause) {
        super("Authentication provider is unavailable", cause);
    }
}
