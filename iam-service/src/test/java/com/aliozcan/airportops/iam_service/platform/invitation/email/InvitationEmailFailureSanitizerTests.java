package com.aliozcan.airportops.iam_service.platform.invitation.email;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationEmailFailureSanitizerTests {

    private final InvitationEmailFailureSanitizer sanitizer =
            new InvitationEmailFailureSanitizer();

    @Test
    void removesStacktraceShapeSecretsAndRawInvitationTokens() {
        String rawToken = "abcdefghijklmnopqrstuvwxyzABCDEFGH_12345678";
        RuntimeException failure = new RuntimeException(
                "failed\nsecret=abc123 token=" + rawToken + " password=hidden");

        String sanitized = sanitizer.sanitize(failure);

        assertThat(sanitized)
                .doesNotContain("\n")
                .doesNotContain("abc123")
                .doesNotContain(rawToken)
                .doesNotContain("hidden")
                .contains("<redacted>");
        assertThat(sanitized.length()).isLessThanOrEqualTo(500);
    }
}
