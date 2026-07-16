package com.aliozcan.airportops.iam_service.auth.session;

public class MfaChallengeExpiredException extends RuntimeException {

    public MfaChallengeExpiredException() {
        super("MFA challenge has expired");
    }
}
