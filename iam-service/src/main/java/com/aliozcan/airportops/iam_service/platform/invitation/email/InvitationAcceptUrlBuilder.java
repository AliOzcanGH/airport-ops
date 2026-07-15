package com.aliozcan.airportops.iam_service.platform.invitation.email;

import com.aliozcan.airportops.iam_service.config.InvitationEmailProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Component
public class InvitationAcceptUrlBuilder {

    private final InvitationEmailProperties properties;

    public InvitationAcceptUrlBuilder(InvitationEmailProperties properties) {
        this.properties = properties;
    }

    public String build(String rawToken) {
        String encodedToken = UriUtils.encode(
                rawToken,
                StandardCharsets.UTF_8);
        return UriComponentsBuilder
                .fromUriString(properties.acceptBaseUrl())
                .queryParam("token", encodedToken)
                .build(true)
                .toUriString();
    }

    public boolean devLinkEnabled() {
        return properties.devLinkEnabled();
    }
}
