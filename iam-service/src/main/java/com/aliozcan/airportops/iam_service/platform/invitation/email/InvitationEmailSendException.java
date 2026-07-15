package com.aliozcan.airportops.iam_service.platform.invitation.email;

public class InvitationEmailSendException extends RuntimeException {

    public InvitationEmailSendException(String message) {
        super(message);
    }

    public InvitationEmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
