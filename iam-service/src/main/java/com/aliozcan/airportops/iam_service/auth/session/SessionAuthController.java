package com.aliozcan.airportops.iam_service.auth.session;

import com.aliozcan.airportops.iam_service.auth.session.dto.CsrfMetadataResponse;
import com.aliozcan.airportops.iam_service.auth.session.dto.MfaLoginChallengeResponse;
import com.aliozcan.airportops.iam_service.auth.session.dto.MfaVerifyRequest;
import com.aliozcan.airportops.iam_service.auth.session.dto.SessionLoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/session")
public class SessionAuthController {

    private final SessionAuthService sessionAuthService;
    private final SessionCookieService cookieService;

    public SessionAuthController(
            SessionAuthService sessionAuthService,
            SessionCookieService cookieService) {
        this.sessionAuthService = sessionAuthService;
        this.cookieService = cookieService;
    }

    @GetMapping("/csrf")
    public CsrfMetadataResponse csrf(CsrfToken csrfToken) {
        return new CsrfMetadataResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken());
    }

    @PostMapping("/login")
    public MfaLoginChallengeResponse login(
            @Valid @RequestBody SessionLoginRequest request) {
        return sessionAuthService.login(request.email(), request.password());
    }

    @PostMapping("/mfa/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            HttpServletResponse response) {
        sessionAuthService.verifyMfa(request.challengeId(), request.code(), response);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        sessionAuthService.refresh(cookieService.readRefreshToken(request), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        sessionAuthService.logout(cookieService.readRefreshToken(request), response);
    }
}
