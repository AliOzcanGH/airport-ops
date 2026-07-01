package com.aliozcan.airportops.iam_service.platform.invitation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationTokenServiceTests {

    private final InvitationTokenService tokenService = new InvitationTokenService();

    @Test
    void generatesHighEntropyUrlSafeTokens() {
        InvitationTokenService.GeneratedInvitationToken first = tokenService.generate();
        InvitationTokenService.GeneratedInvitationToken second = tokenService.generate();

        assertThat(first.rawToken()).matches("[A-Za-z0-9_-]{43}");
        assertThat(second.rawToken()).matches("[A-Za-z0-9_-]{43}");
        assertThat(first.rawToken()).isNotEqualTo(second.rawToken());
        assertThat(first.tokenHash()).matches("[0-9a-f]{64}");
        assertThat(first.tokenHash()).isNotEqualTo(first.rawToken());
    }

    @Test
    void hashesTokensDeterministicallyWithSha256() {
        String rawToken = "local-dev-invitation-token";

        assertThat(tokenService.hash(rawToken))
                .isEqualTo(tokenService.hash(rawToken))
                .isEqualTo("f471f8cd2817a6e785cced42c7c3de2e5910cdeaecfe7ad15c8525e48780faf1");
    }
}
