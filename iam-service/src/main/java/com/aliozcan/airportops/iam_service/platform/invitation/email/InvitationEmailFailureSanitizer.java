package com.aliozcan.airportops.iam_service.platform.invitation.email;

import org.springframework.stereotype.Component;

@Component
public class InvitationEmailFailureSanitizer {

    private static final int MAX_LENGTH = 500;

    public String sanitize(Throwable throwable) {
        if (throwable == null) {
            return "Unknown email delivery failure";
        }
        String type = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        String reason = message == null || message.isBlank()
                ? type
                : type + ": " + sanitizeMessage(message);
        return truncate(reason);
    }

    private String sanitizeMessage(String message) {
        return message
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("(?i)(access[_-]?key|secret|token|password)\\s*[:=]\\s*\\S+", "$1=<redacted>")
                .replaceAll("\\b[A-Za-z0-9_-]{43}\\b", "<redacted-token>")
                .trim();
    }

    private String truncate(String value) {
        if (value.length() <= MAX_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_LENGTH);
    }
}
