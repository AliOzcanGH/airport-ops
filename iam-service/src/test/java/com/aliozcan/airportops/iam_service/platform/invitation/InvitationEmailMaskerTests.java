package com.aliozcan.airportops.iam_service.platform.invitation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationEmailMaskerTests {

    private final InvitationEmailMasker masker = new InvitationEmailMasker();

    @Test
    void masksEmailDeterministically() {
        assertThat(masker.mask("admin@pegasus.demo"))
                .isEqualTo("ad***@pegasus.demo");
        assertThat(masker.mask("ab@pegasus.demo"))
                .isEqualTo("a***@pegasus.demo");
        assertThat(masker.mask("a@pegasus.demo"))
                .isEqualTo("***@pegasus.demo");
    }

    @Test
    void hidesMalformedOrMissingEmail() {
        assertThat(masker.mask(null)).isEqualTo("***");
        assertThat(masker.mask("not-an-email")).isEqualTo("***");
        assertThat(masker.mask("@pegasus.demo")).isEqualTo("***");
        assertThat(masker.mask("admin@")).isEqualTo("***");
    }
}
