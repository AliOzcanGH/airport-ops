package com.aliozcan.airportops.iam_service.platform.invitation.email;

import com.aliozcan.airportops.iam_service.config.InvitationEmailProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationAcceptUrlBuilderTests {

    @Test
    void encodesRawTokenAsQueryParameter() {
        InvitationAcceptUrlBuilder builder = new InvitationAcceptUrlBuilder(
                new InvitationEmailProperties(
                        "http://127.0.0.1:5173/invitations/accept",
                        true));

        String url = builder.build("abc+/_ token");

        assertThat(url)
                .isEqualTo("http://127.0.0.1:5173/invitations/accept?token=abc%2B%2F_%20token");
    }
}
