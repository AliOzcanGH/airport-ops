package com.aliozcan.airportops.iam_service.auth.session;

record MfaVerificationResult(
        KeycloakTokenResponse tokens,
        Failure failure
) {
    enum Failure {
        EXPIRED,
        LOCKED,
        INVALID_CODE
    }

    static MfaVerificationResult success(KeycloakTokenResponse tokens) {
        return new MfaVerificationResult(tokens, null);
    }

    static MfaVerificationResult failed(Failure failure) {
        return new MfaVerificationResult(null, failure);
    }
}
