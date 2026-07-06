package com.aliozcan.airportops.iam_service.auth.session;

public class SessionExpiredException extends RuntimeException {

    public SessionExpiredException() {
        super("Session has expired");
    }
}
