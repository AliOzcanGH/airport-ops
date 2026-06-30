package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.KeycloakMeResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/keycloak")
public class KeycloakAuthController {

    private final KeycloakRealmRoleExtractor roleExtractor;

    public KeycloakAuthController(KeycloakRealmRoleExtractor roleExtractor) {
        this.roleExtractor = roleExtractor;
    }

    @GetMapping("/me")
    public KeycloakMeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new KeycloakMeResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("preferred_username"),
                jwt.getIssuer() == null ? null : jwt.getIssuer().toString(),
                roleExtractor.extract(jwt)
        );
    }
}
