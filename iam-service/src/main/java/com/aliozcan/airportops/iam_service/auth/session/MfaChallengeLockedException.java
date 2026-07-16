package com.aliozcan.airportops.iam_service.auth.session;

public class MfaChallengeLockedException extends RuntimeException {

    public MfaChallengeLockedException() {
        super("MFA challenge is locked");
    }
}
