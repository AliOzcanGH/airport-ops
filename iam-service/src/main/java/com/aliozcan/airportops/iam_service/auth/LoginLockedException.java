package com.aliozcan.airportops.iam_service.auth;

public class LoginLockedException extends RuntimeException {

    public LoginLockedException() {
        super("Too many failed login attempts");
    }
}
