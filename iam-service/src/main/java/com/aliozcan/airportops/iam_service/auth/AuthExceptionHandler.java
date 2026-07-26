package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.auth.session.AuthProviderUnavailableException;
import com.aliozcan.airportops.iam_service.auth.session.MfaChallengeExpiredException;
import com.aliozcan.airportops.iam_service.auth.session.MfaChallengeLockedException;
import com.aliozcan.airportops.iam_service.auth.session.MfaCodeInvalidException;
import com.aliozcan.airportops.iam_service.auth.session.MfaConfigurationException;
import com.aliozcan.airportops.iam_service.auth.session.SessionExpiredException;
import com.aliozcan.airportops.iam_service.app.setup.SetupAlreadyCompletedException;
import com.aliozcan.airportops.iam_service.app.setup.SetupProfileIncompleteException;
import com.aliozcan.airportops.iam_service.app.setup.SetupProfileRequiredException;
import com.aliozcan.airportops.iam_service.platform.invitation.InvitationAlreadyUsedException;
import com.aliozcan.airportops.iam_service.platform.invitation.InvitationExpiredException;
import com.aliozcan.airportops.iam_service.platform.invitation.InvitationNotFoundException;
import com.aliozcan.airportops.iam_service.platform.invitation.IamUserAlreadyExistsException;
import com.aliozcan.airportops.iam_service.platform.invitation.OrganizationAlreadyExistsException;
import com.aliozcan.airportops.iam_service.platform.invitation.PendingInvitationExistsException;
import com.aliozcan.airportops.iam_service.platform.invitation.ProvisioningInvariantException;
import com.aliozcan.airportops.iam_service.platform.tenant.PlatformTenantNotFoundException;
import com.aliozcan.airportops.iam_service.tenant.AmbiguousTenantContextException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class AuthExceptionHandler {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";
    private static final String USER_NOT_PROVISIONED_MESSAGE =
            "Authenticated user is not provisioned in IAM";
    private static final String VALIDATION_ERROR_MESSAGE = "Request validation failed";
    private static final String PENDING_INVITATION_EXISTS_MESSAGE =
            "A pending invitation already exists for this email";
    private static final String INVITATION_NOT_FOUND_MESSAGE = "Invitation not found";
    private static final String INVITATION_ALREADY_USED_MESSAGE =
            "Invitation has already been used";
    private static final String INVITATION_EXPIRED_MESSAGE = "Invitation has expired";
    private static final String IAM_USER_ALREADY_EXISTS_MESSAGE =
            "An IAM user already exists for this email";
    private static final String ORGANIZATION_ALREADY_EXISTS_MESSAGE =
            "An organization already exists with this name";
    private static final String PROVISIONING_CONFIGURATION_ERROR_MESSAGE =
            "Invitation provisioning is temporarily unavailable";
    private static final String AUTH_PROVIDER_UNAVAILABLE_MESSAGE =
            "Authentication provider is temporarily unavailable";
    private static final String SESSION_EXPIRED_MESSAGE =
            "Session has expired";
    private static final String MFA_CHALLENGE_EXPIRED_MESSAGE =
            "MFA challenge has expired";
    private static final String MFA_CHALLENGE_LOCKED_MESSAGE =
            "MFA challenge is locked";
    private static final String MFA_CODE_INVALID_MESSAGE =
            "MFA code is invalid";
    private static final String MFA_CONFIGURATION_ERROR_MESSAGE =
            "MFA is temporarily unavailable";
    private static final String TENANT_NOT_FOUND_MESSAGE =
            "Tenant organization not found";
    private static final String SETUP_PROFILE_REQUIRED_MESSAGE =
            "Setup profile must be saved before completion";
    private static final String SETUP_PROFILE_INCOMPLETE_MESSAGE =
            "Setup profile is missing required fields";
    private static final String SETUP_ALREADY_COMPLETED_MESSAGE =
            "Tenant setup has already been completed";
    private static final String AMBIGUOUS_TENANT_CONTEXT_MESSAGE =
            "Active IAM user has multiple tenant organizations";

    @ExceptionHandler(InvalidLoginException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLogin(
            InvalidLoginException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.name(),
                "INVALID_CREDENTIALS",
                INVALID_CREDENTIALS_MESSAGE,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(UserNotProvisionedException.class)
    public ResponseEntity<ErrorResponse> handleUserNotProvisioned(
            UserNotProvisionedException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.name(),
                "USER_NOT_PROVISIONED",
                USER_NOT_PROVISIONED_MESSAGE,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(AuthProviderUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAuthProviderUnavailable(
            AuthProviderUnavailableException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AUTH_PROVIDER_UNAVAILABLE",
                AUTH_PROVIDER_UNAVAILABLE_MESSAGE,
                request);
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ErrorResponse> handleSessionExpired(
            SessionExpiredException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.UNAUTHORIZED,
                "SESSION_EXPIRED",
                SESSION_EXPIRED_MESSAGE,
                request);
    }

    @ExceptionHandler(MfaChallengeExpiredException.class)
    public ResponseEntity<ErrorResponse> handleMfaChallengeExpired(
            MfaChallengeExpiredException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.UNAUTHORIZED,
                "MFA_CHALLENGE_EXPIRED",
                MFA_CHALLENGE_EXPIRED_MESSAGE,
                request);
    }

    @ExceptionHandler(MfaChallengeLockedException.class)
    public ResponseEntity<ErrorResponse> handleMfaChallengeLocked(
            MfaChallengeLockedException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.UNAUTHORIZED,
                "MFA_CHALLENGE_LOCKED",
                MFA_CHALLENGE_LOCKED_MESSAGE,
                request);
    }

    @ExceptionHandler(MfaCodeInvalidException.class)
    public ResponseEntity<ErrorResponse> handleMfaCodeInvalid(
            MfaCodeInvalidException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.UNAUTHORIZED,
                "MFA_CODE_INVALID",
                MFA_CODE_INVALID_MESSAGE,
                request);
    }

    @ExceptionHandler(MfaConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleMfaConfiguration(
            MfaConfigurationException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "MFA_CONFIGURATION_ERROR",
                MFA_CONFIGURATION_ERROR_MESSAGE,
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                VALIDATION_ERROR_MESSAGE,
                request);
    }

    @ExceptionHandler(PendingInvitationExistsException.class)
    public ResponseEntity<ErrorResponse> handlePendingInvitationExists(
            PendingInvitationExistsException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "PENDING_INVITATION_EXISTS",
                PENDING_INVITATION_EXISTS_MESSAGE,
                request);
    }

    @ExceptionHandler(InvitationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInvitationNotFound(
            InvitationNotFoundException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                "INVITATION_NOT_FOUND",
                INVITATION_NOT_FOUND_MESSAGE,
                request);
    }

    @ExceptionHandler(InvitationAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleInvitationAlreadyUsed(
            InvitationAlreadyUsedException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "INVITATION_ALREADY_USED",
                INVITATION_ALREADY_USED_MESSAGE,
                request);
    }

    @ExceptionHandler(InvitationExpiredException.class)
    public ResponseEntity<ErrorResponse> handleInvitationExpired(
            InvitationExpiredException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.GONE,
                "INVITATION_EXPIRED",
                INVITATION_EXPIRED_MESSAGE,
                request);
    }

    @ExceptionHandler(IamUserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleIamUserAlreadyExists(
            IamUserAlreadyExistsException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "IAM_USER_ALREADY_EXISTS",
                IAM_USER_ALREADY_EXISTS_MESSAGE,
                request);
    }

    @ExceptionHandler(OrganizationAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleOrganizationAlreadyExists(
            OrganizationAlreadyExistsException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "ORGANIZATION_ALREADY_EXISTS",
                ORGANIZATION_ALREADY_EXISTS_MESSAGE,
                request);
    }

    @ExceptionHandler(ProvisioningInvariantException.class)
    public ResponseEntity<ErrorResponse> handleProvisioningInvariant(
            ProvisioningInvariantException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PROVISIONING_CONFIGURATION_ERROR",
                PROVISIONING_CONFIGURATION_ERROR_MESSAGE,
                request);
    }

    @ExceptionHandler(PlatformTenantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePlatformTenantNotFound(
            PlatformTenantNotFoundException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                "TENANT_NOT_FOUND",
                TENANT_NOT_FOUND_MESSAGE,
                request);
    }

    @ExceptionHandler(SetupProfileRequiredException.class)
    public ResponseEntity<ErrorResponse> handleSetupProfileRequired(
            SetupProfileRequiredException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "SETUP_PROFILE_REQUIRED",
                SETUP_PROFILE_REQUIRED_MESSAGE,
                request);
    }

    @ExceptionHandler(SetupProfileIncompleteException.class)
    public ResponseEntity<ErrorResponse> handleSetupProfileIncomplete(
            SetupProfileIncompleteException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "SETUP_PROFILE_INCOMPLETE",
                SETUP_PROFILE_INCOMPLETE_MESSAGE,
                request);
    }

    @ExceptionHandler(SetupAlreadyCompletedException.class)
    public ResponseEntity<ErrorResponse> handleSetupAlreadyCompleted(
            SetupAlreadyCompletedException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "SETUP_ALREADY_COMPLETED",
                SETUP_ALREADY_COMPLETED_MESSAGE,
                request);
    }

    @ExceptionHandler(AmbiguousTenantContextException.class)
    public ResponseEntity<ErrorResponse> handleAmbiguousTenantContext(
            AmbiguousTenantContextException exception,
            HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "AMBIGUOUS_TENANT_CONTEXT",
                AMBIGUOUS_TENANT_CONTEXT_MESSAGE,
                request);
    }

    private ResponseEntity<ErrorResponse> errorResponse(
            HttpStatus status,
            String errorCode,
            String message,
            HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.name(),
                errorCode,
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }
}
