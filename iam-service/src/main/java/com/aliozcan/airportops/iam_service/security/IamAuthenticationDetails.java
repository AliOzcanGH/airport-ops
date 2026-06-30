package com.aliozcan.airportops.iam_service.security;

import java.util.UUID;

public record IamAuthenticationDetails(
        UUID iamUserId,
        boolean provisioned
) {

    public static IamAuthenticationDetails provisioned(UUID iamUserId) {
        return new IamAuthenticationDetails(iamUserId, true);
    }

    public static IamAuthenticationDetails unprovisioned() {
        return new IamAuthenticationDetails(null, false);
    }
}
