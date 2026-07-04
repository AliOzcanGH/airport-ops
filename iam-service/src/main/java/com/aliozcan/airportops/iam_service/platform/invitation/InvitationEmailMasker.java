package com.aliozcan.airportops.iam_service.platform.invitation;

import org.springframework.stereotype.Component;

@Component
public class InvitationEmailMasker {

    public String mask(String email) {
        if (email == null) {
            return "***";
        }

        int separator = email.lastIndexOf('@');
        if (separator <= 0 || separator == email.length() - 1) {
            return "***";
        }

        String localPart = email.substring(0, separator);
        String domain = email.substring(separator);
        String visiblePrefix;
        if (localPart.length() >= 3) {
            visiblePrefix = localPart.substring(0, 2);
        } else if (localPart.length() == 2) {
            visiblePrefix = localPart.substring(0, 1);
        } else {
            visiblePrefix = "";
        }
        return visiblePrefix + "***" + domain;
    }
}
