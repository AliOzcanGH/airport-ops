package com.aliozcan.airportops.iam_service.app.setup;

import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupProfileRequest;
import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupProfileResponse;
import com.aliozcan.airportops.iam_service.auth.UserNotProvisionedException;
import com.aliozcan.airportops.iam_service.domain.model.OrganizationSetupProfileEntity;
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
public class AppSetupProfileService {

    private final UserRepository userRepository;
    private final TenantAuthorizationRepository tenantAuthorizationRepository;
    private final OrganizationSetupProfileRepository profileRepository;

    public AppSetupProfileService(
            UserRepository userRepository,
            TenantAuthorizationRepository tenantAuthorizationRepository,
            OrganizationSetupProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.tenantAuthorizationRepository = tenantAuthorizationRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public AppSetupProfileResponse save(
            Authentication authentication,
            AppSetupProfileRequest request) {
        IamAuthenticationDetails details = iamDetails(authentication);
        UUID userId = userRepository.findActiveById(details.iamUserId())
                .orElseThrow(UserNotProvisionedException::new)
                .getId();

        List<UUID> authorizedOrganizations = tenantAuthorizationRepository
                .findOnboardingAirlineAdminOrganizationIdsForUpdate(userId);
        if (authorizedOrganizations.size() != 1) {
            throw new AccessDeniedException(
                    "Tenant profile requires an onboarding airline administrator");
        }

        UUID organizationId = authorizedOrganizations.get(0);
        Instant now = Instant.now();
        profileRepository.upsert(
                organizationId,
                request.displayName(),
                request.iataCode(),
                request.icaoCode(),
                request.countryCode(),
                request.timezone(),
                request.baseAirportIata(),
                request.operationsContactEmail(),
                now);

        OrganizationSetupProfileEntity saved = profileRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Organization setup profile UPSERT did not persist a row"));
        return AppSetupProfileResponse.from(saved);
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
