package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.AuthMeResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthMeController {

    private final AuthMeService authMeService;

    public AuthMeController(AuthMeService authMeService) {
        this.authMeService = authMeService;
    }

    @GetMapping("/me")
    public AuthMeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authMeService.getCurrentUser(jwt);
    }
}
