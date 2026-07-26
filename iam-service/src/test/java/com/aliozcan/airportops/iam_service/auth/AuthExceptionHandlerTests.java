package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.tenant.AmbiguousTenantContextException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthExceptionHandlerTests {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    void mapsAmbiguousTenantContextToConflict() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/auth/me");

        ResponseEntity<ErrorResponse> response = handler.handleAmbiguousTenantContext(
                new AmbiguousTenantContextException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("AMBIGUOUS_TENANT_CONTEXT");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getBody().path()).isEqualTo("/auth/me");
    }
}
