package com.aliozcan.airportops.iam_service.auth.mfa;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTests {

    private final TotpService service = new TotpService(
            new DefaultSecretGenerator(),
            new DefaultCodeGenerator(),
            Clock.fixed(Instant.parse("2026-07-16T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void generatesSecretAndOtpAuthUri() {
        String secret = service.generateSecret();

        String uri = service.otpauthUri("user@example.com", secret);

        assertThat(secret).isNotBlank();
        assertThat(uri)
                .startsWith("otpauth://totp/")
                .contains("secret=" + secret)
                .contains("issuer=Airport%20Ops")
                .contains("digits=6")
                .contains("period=30");
    }

    @Test
    void verifiesValidCurrentCodeAndRejectsInvalidCode() {
        String secret = service.generateSecret();
        String currentCode = service.generateCodeForCurrentTime(secret);

        assertThat(service.verify(secret, currentCode)).isTrue();
        assertThat(service.verify(secret, "000000")).isFalse();
        assertThat(service.verify(secret, "not-a-code")).isFalse();
    }
}
