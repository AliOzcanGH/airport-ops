package com.aliozcan.airportops.iam_service.auth;

public class InvalidLoginException extends RuntimeException {

    public InvalidLoginException() {
        super("Invalid email or password");
    }
}
