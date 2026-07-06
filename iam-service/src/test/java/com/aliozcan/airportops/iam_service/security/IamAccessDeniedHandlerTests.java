package com.aliozcan.airportops.iam_service.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IamAccessDeniedHandlerTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final IamAccessDeniedHandler handler =
            new IamAccessDeniedHandler(objectMapper);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsUserNotProvisionedForUnprovisionedDetails() throws Exception {
        setAuthenticationDetails(IamAuthenticationDetails.unprovisioned());

        MockHttpServletResponse response = handle();

        assertError(response, "USER_NOT_PROVISIONED");
    }

    @Test
    void returnsMissingPermissionForNullDetails() throws Exception {
        setAuthenticationDetails(null);

        MockHttpServletResponse response = handle();

        assertError(response, "MISSING_PERMISSION");
    }

    @Test
    void returnsMissingPermissionForUnexpectedDetails() throws Exception {
        setAuthenticationDetails("unexpected-details");

        MockHttpServletResponse response = handle();

        assertError(response, "MISSING_PERMISSION");
    }

    @Test
    void returnsCsrfValidationErrorForCsrfFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/auth/session/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(
                request,
                response,
                new MissingCsrfTokenException(null));

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("errorCode").asText())
                .isEqualTo("CSRF_VALIDATION_FAILED");
    }

    private MockHttpServletResponse handle() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/platform/authorization/probe");
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.handle(request, response, new AccessDeniedException("denied"));
        return response;
    }

    private void setAuthenticationDetails(Object details) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("principal", "credentials", List.of());
        authentication.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void assertError(MockHttpServletResponse response, String errorCode)
            throws Exception {
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("errorCode").asText()).isEqualTo(errorCode);
        assertThat(body.get("path").asText())
                .isEqualTo("/platform/authorization/probe");
    }
}
