package com.aliozcan.airportops.iam_service.app.setup;

import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupCompletionResponse;
import com.aliozcan.airportops.iam_service.auth.UserNotProvisionedException;
import com.aliozcan.airportops.iam_service.domain.model.OrganizationEntity;
import com.aliozcan.airportops.iam_service.domain.model.OrganizationSetupProfileEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;
import com.aliozcan.airportops.iam_service.repository.OrganizationRepository;
import com.aliozcan.airportops.iam_service.repository.OrganizationSetupProfileRepository;
import com.aliozcan.airportops.iam_service.repository.TenantAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.security.IamAuthenticationDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AppSetupCompletionService {

    private final UserRepository userRepository;
    private final TenantAuthorizationRepository tenantAuthorizationRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSetupProfileRepository profileRepository;

    public AppSetupCompletionService(
            UserRepository userRepository,
            TenantAuthorizationRepository tenantAuthorizationRepository,
            OrganizationRepository organizationRepository,
            OrganizationSetupProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.tenantAuthorizationRepository = tenantAuthorizationRepository;
        this.organizationRepository = organizationRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public AppSetupCompletionResponse complete(Authentication authentication) {
        IamAuthenticationDetails details = iamDetails(authentication);
        UUID userId = userRepository.findActiveById(details.iamUserId())
                .orElseThrow(UserNotProvisionedException::new)
                .getId();

        List<UUID> onboardingOrganizations = tenantAuthorizationRepository
                .findOnboardingAirlineAdminOrganizationIdsForUpdate(userId);
        if (onboardingOrganizations.size() > 1) {
            throw new AccessDeniedException(
                    "Setup completion tenant context is ambiguous");
        }
        if (onboardingOrganizations.isEmpty()) {
            List<UUID> activeOrganizations = tenantAuthorizationRepository
                    .findActiveAirlineAdminOrganizationIdsForCompletionForUpdate(userId);
            if (!activeOrganizations.isEmpty()) {
                throw new SetupAlreadyCompletedException();
            }
            throw new AccessDeniedException(
                    "Setup completion requires an onboarding airline administrator membership");
        }

        OrganizationEntity organization = organizationRepository
                .findById(onboardingOrganizations.get(0))
                .orElseThrow(() -> new AccessDeniedException(
                        "Tenant organization is not available"));

        if (organization.getDeletedAt() != null
                || organization.getStatus() == OrganizationStatus.INACTIVE) {
            throw new AccessDeniedException("Inactive tenant setup cannot be completed");
        }
        OrganizationSetupProfileEntity profile = profileRepository
                .findById(organization.getId())
                .orElseThrow(SetupProfileRequiredException::new);
        if (!isComplete(profile)) {
            throw new SetupProfileIncompleteException();
        }

        Instant completedAt = Instant.now();
        organization.activate(completedAt);
        return new AppSetupCompletionResponse(
                organization.getId(),
                organization.getStatus(),
                completedAt);
    }

    private boolean isComplete(OrganizationSetupProfileEntity profile) {
        return hasText(profile.getDisplayName())
                && hasText(profile.getCountryCode())
                && hasText(profile.getTimezone())
                && hasText(profile.getOperationsContactEmail());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private IamAuthenticationDetails iamDetails(Authentication authentication) {
        if (authentication == null
                || !(authentication.getDetails() instanceof IamAuthenticationDetails details)
                || !details.provisioned()
                || details.iamUserId() == null) {
            throw new UserNotProvisionedException();
        }
        return details;
    }
}
