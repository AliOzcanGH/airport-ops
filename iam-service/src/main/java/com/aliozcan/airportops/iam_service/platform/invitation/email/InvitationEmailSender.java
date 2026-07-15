package com.aliozcan.airportops.iam_service.platform.invitation.email;

public interface InvitationEmailSender {

    void send(InvitationEmailMessage message);
}
