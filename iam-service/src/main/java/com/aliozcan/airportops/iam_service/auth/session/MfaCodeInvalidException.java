package com.aliozcan.airportops.iam_service.auth.session;

public class MfaCodeInvalidException extends RuntimeException {

    public MfaCodeInvalidException() {
        super("MFA code is invalid");
    }
}
