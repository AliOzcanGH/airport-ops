package com.aliozcan.airportops.iam_service.auth.token;

import com.aliozcan.airportops.iam_service.auth.dto.IamTokenResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class IamTokenController {

    private final IamTokenService iamTokenService;

    public IamTokenController(IamTokenService iamTokenService) {
        this.iamTokenService = iamTokenService;
    }

    @PostMapping("/iam-token")
    public IamTokenResponse issueToken(@AuthenticationPrincipal Jwt jwt) {
        return iamTokenService.issueToken(jwt);
    }
}
