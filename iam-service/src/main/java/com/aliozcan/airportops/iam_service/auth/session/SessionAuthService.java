package com.aliozcan.airportops.iam_service.auth.session;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class SessionAuthService {

    private final KeycloakSessionClient keycloakSessionClient;
    private final SessionCookieService cookieService;

    public SessionAuthService(
            KeycloakSessionClient keycloakSessionClient,
            SessionCookieService cookieService) {
        this.keycloakSessionClient = keycloakSessionClient;
        this.cookieService = cookieService;
    }

    public void login(String email, String password, HttpServletResponse response) {
        KeycloakTokenResponse tokens = keycloakSessionClient.login(email.trim(), password);
        cookieService.writeTokens(response, tokens);
    }

    public void refresh(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            cookieService.clearTokens(response);
            throw new SessionExpiredException();
        }
        try {
            cookieService.writeTokens(response, keycloakSessionClient.refresh(refreshToken));
        } catch (SessionExpiredException exception) {
            cookieService.clearTokens(response);
            throw exception;
        }
    }

    public void logout(String refreshToken, HttpServletResponse response) {
        try {
            if (refreshToken != null && !refreshToken.isBlank()) {
                keycloakSessionClient.logout(refreshToken);
            }
        } catch (AuthProviderUnavailableException | SessionExpiredException ignored) {
            // Local logout must succeed even when the provider session is unavailable.
        } finally {
            cookieService.clearTokens(response);
        }
    }
}
