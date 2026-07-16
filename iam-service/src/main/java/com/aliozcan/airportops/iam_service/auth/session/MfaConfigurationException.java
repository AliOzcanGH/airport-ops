package com.aliozcan.airportops.iam_service.auth.session;

public class MfaConfigurationException extends RuntimeException {

    public MfaConfigurationException(Throwable cause) {
        super("MFA is temporarily unavailable", cause);
    }
}
